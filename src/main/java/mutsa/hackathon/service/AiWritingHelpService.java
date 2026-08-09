package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AiWritingHelpService {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final int DAILY_LIMIT = 3;

    private final AppUserRepository
            appUserRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    /**
     * 오늘의 작성 도움 질문 사용 상태를 조회.
     * 질문을 새로 생성하지 않으므로 사용 횟수는
     * 소모되지 않음.
     */
    @Transactional(readOnly = true)
    public WritingHelpStatusResponse getStatus(
            Long userId
    ) {
        validateUserExists(userId);

        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

        long usedCount =
                countTodayQuestions(
                        userId,
                        today
                );

        return WritingHelpStatusResponse.of(
                DAILY_LIMIT,
                usedCount
        );
    }

    @Transactional
    public WritingHelpQuestionResponse generateQuestion(
            Long userId
    ) {
        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

        long usedCount =
                countTodayQuestions(
                        userId,
                        today
                );

        if (usedCount >= DAILY_LIMIT) {
            throw new ProjectException(
                    ErrorCode
                            .WRITING_HELP_LIMIT_EXCEEDED
            );
        }

        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        String questionText =
                writingHelpQuestionGenerator.generate(
                        new WritingHelpPrompt(
                                user.getNickname(),
                                user.getJob(),
                                user.getAiMemoryProfile()
                        )
                );

        AiQuestion question =
                aiQuestionRepository.save(
                        AiQuestion.createWritingHelp(
                                user,
                                questionText,
                                Math.toIntExact(
                                        usedCount + 1
                                ),
                                today,
                                QuestionGenerationSource.AI
                        )
                );

        return WritingHelpQuestionResponse.from(
                question,
                DAILY_LIMIT
        );
    }

    private long countTodayQuestions(
            Long userId,
            LocalDate today
    ) {
        return aiQuestionRepository
                .countByUserIdAndQuestionTypeAndAskedDate(
                        userId,
                        AiQuestionType.WRITING_HELP,
                        today
                );
    }

    private void validateUserExists(
            Long userId
    ) {
        if (
                userId == null
                        || !appUserRepository
                        .existsById(userId)
        ) {
            throw new ProjectException(
                    ErrorCode.USER_NOT_FOUND
            );
        }
    }
}
