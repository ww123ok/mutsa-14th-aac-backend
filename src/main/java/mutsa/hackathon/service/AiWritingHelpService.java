package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.WritingHelpQuestionResponse;
import mutsa.hackathon.dto.WritingHelpStatusResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiWritingHelpService {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final int DAILY_LIMIT = 3;

    private static final int RECENT_DIARY_LIMIT = 5;

    private static final int RECENT_DIARY_CONTENT_LIMIT = 400;

    private final AppUserRepository
            appUserRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final DiaryRepository
            diaryRepository;

    private final WritingHelpQuestionGenerator
            writingHelpQuestionGenerator;

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

        List<String> recentQuestionHistory =
                aiQuestionRepository
                        .findTop12ByUserIdAndQuestionTypeOrderByAskedDateDescQuestionOrderDesc(
                                userId,
                                AiQuestionType.WRITING_HELP
                        )
                        .stream()
                        .filter(question -> !today.equals(question.getAskedDate()))
                        .map(AiQuestion::getQuestionText)
                        .limit(10)
                        .toList();

        List<String> recentDiaryContexts =
                diaryRepository
                        .findByUserIdAndRecordedDateBeforeAndDeletedFalseOrderByRecordedDateDescCreatedAtDesc(
                                userId,
                                today,
                                PageRequest.of(0, RECENT_DIARY_LIMIT)
                        )
                        .stream()
                        .map(this::toRecentDiaryContext)
                        .toList();

        int questionOrder =
                Math.toIntExact(
                        usedCount + 1
                );

        WritingHelpPrompt prompt =
                new WritingHelpPrompt(
                        user.getNickname(),
                        user.getJob(),
                        user.getAiMemoryProfile(),
                        questionOrder,
                        previousQuestions,
                        recentQuestionHistory,
                        recentDiaryContexts
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

    private String toRecentDiaryContext(Diary diary) {
        String normalizedContent = diary.getContent()
                .replaceAll("\\s+", " ")
                .trim();
        if (normalizedContent.length() > RECENT_DIARY_CONTENT_LIMIT) {
            normalizedContent = normalizedContent.substring(0, RECENT_DIARY_CONTENT_LIMIT) + "…";
        }
        return diary.getRecordedDate() + ": " + normalizedContent;
    }
}
