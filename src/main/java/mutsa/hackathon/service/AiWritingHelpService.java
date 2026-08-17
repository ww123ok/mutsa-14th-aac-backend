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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiWritingHelpService {

    private static final int DAILY_LIMIT = 3;

    private final AppUserRepository
            appUserRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

    private final UserDayService
            userDayService;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    @Transactional(readOnly = true)
    public WritingHelpStatusResponse getStatus(
            Long userId
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

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

    /**
     * OpenAI 응답을 기다리는 동안 DB transaction을
     * 유지하지 않기 위해 의도적으로 @Transactional을
     * 사용하지 않음.
     * Repository 조회/저장은 각각 필요한 짧은
     * transaction만 사용.
     */
    public WritingHelpQuestionResponse
    generateQuestion(
            Long userId
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

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

        List<String> previousQuestions =
                aiQuestionRepository
                        .findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                today
                        )
                        .stream()
                        .map(
                                AiQuestion::getQuestionText
                        )
                        .toList();

        List<String> earlierQuestions =
                aiQuestionRepository
                        .findTop12ByUserIdAndQuestionTypeAndAskedDateBeforeOrderByAskedDateDescQuestionOrderDesc(
                                userId,
                                AiQuestionType.WRITING_HELP,
                                today
                        )
                        .stream()
                        .map(
                                AiQuestion::getQuestionText
                        )
                        .toList();

        int questionOrder =
                Math.toIntExact(
                        usedCount + 1
                );

        Optional<WritingHelpMemoryContext> memoryContext =
                aiMemoryProfileService
                        .findNextWritingHelpMemory(
                                userId
                        );

        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        user.getNickname(),
                        user.getJob(),
                        memoryContext
                                .map(
                                        WritingHelpMemoryContext::memoryProfile
                                )
                                .orElse(
                                        null
                                ),
                        questionOrder,
                        previousQuestions,
                        earlierQuestions
                );

        /*
         * 외부 OpenAI 호출.
         * generateQuestion() 자체에 transaction이 없으므로
         * DB transaction 밖에서 실행.
         */
        String questionText =
                writingHelpQuestionGenerator
                        .generate(prompt);

        AiQuestion question =
                aiQuestionRepository.save(
                        AiQuestion.createWritingHelp(
                                user,
                                questionText,
                                questionOrder,
                                today,
                                QuestionGenerationSource.AI
                        )
                );

        memoryContext.ifPresent(context ->
                aiMemoryProfileService
                        .markWritingHelpMemoryUsed(
                                userId,
                                context.memoryId()
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
