package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private static final String
            FALLBACK_REFLECTION_QUESTION =
            "오늘의 기록에서 가장 오래 마음에 남은 순간은 무엇인가요?";

    private final DiaryRepository
            diaryRepository;

    private final DiaryRewardRepository
            diaryRewardRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    private final DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    private final DiaryCreatePersistenceService
            diaryCreatePersistenceService;

    /**
     * 이 메서드에는 의도적으로 @Transactional을 붙이지 않음.
     * 흐름:
     * 1. 짧은 read-only transaction으로 작성 가능 여부 확인
     * 2. transaction이 없는 상태에서 OpenAI 성찰 질문 호출
     * 3. 짧은 write transaction으로 결과 저장
     */
    public DiaryCreateResponse create(
            Long userId,
            DiaryCreateRequest request
    ) {
        LocalDate today =
                LocalDate.now(
                        SERVICE_ZONE
                );

        diaryCreatePersistenceService
                .validateCanCreate(
                        userId,
                        today
                );

        /*
         * 외부 OpenAI 호출.
         * 이 시점에는 DiaryService transaction이 존재하지 않음.
         */
        GeneratedReflectionQuestion
                generatedQuestion =
                generateReflectionQuestion(
                        request.content()
                );

        return diaryCreatePersistenceService
                .persist(
                        userId,
                        request,
                        today,
                        generatedQuestion
                                .questionText(),
                        generatedQuestion
                                .generationSource()
                );
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getMonthlyDiaries(
            Long userId,
            int year,
            int month
    ) {
        YearMonth yearMonth =
                YearMonth.of(
                        year,
                        month
                );

        List<Diary> diaries =
                diaryRepository
                        .findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                                userId,
                                yearMonth.atDay(1),
                                yearMonth.atEndOfMonth()
                        );

        if (diaries.isEmpty()) {
            return List.of();
        }

        Map<Long, DiaryReward>
                rewardsByDiaryId =
                diaryRewardRepository
                        .findAllByDiaryIdIn(
                                diaries.stream()
                                        .map(
                                                Diary::getId
                                        )
                                        .toList()
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        reward ->
                                                reward
                                                        .getDiary()
                                                        .getId(),
                                        Function.identity()
                                )
                        );

        return diaries.stream()
                .map(diary ->
                        DiaryResponse.from(
                                diary,
                                rewardsByDiaryId
                                        .get(
                                                diary.getId()
                                        )
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary =
                findActiveDiary(
                        userId,
                        diaryId
                );

        DiaryReward reward =
                diaryRewardRepository
                        .findByDiaryId(
                                diaryId
                        )
                        .orElse(null);

        return DiaryResponse.from(
                diary,
                reward
        );
    }

    @Transactional
    public void deleteDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary =
                findActiveDiary(
                        userId,
                        diaryId
                );

        diary.softDelete();

        /*
         * 삭제된 일기에서 생성된 기억은
         * 이후 질문에 사용되지 않도록 폐기.
         * 이 과정에는 외부 OpenAI 호출이 없음.
         */
        aiMemoryProfileService
                .revokeMemoriesFromDiary(
                        userId,
                        diaryId
                );
    }

    private GeneratedReflectionQuestion
    generateReflectionQuestion(
            String diaryContent
    ) {
        DiaryReflectionPrompt prompt =
                new DiaryReflectionPrompt(
                        diaryContent
                );

        try {
            return new GeneratedReflectionQuestion(
                    diaryReflectionQuestionGenerator
                            .generate(
                                    prompt
                            ),
                    QuestionGenerationSource.AI
            );

        } catch (
                RuntimeException exception
        ) {
            return new GeneratedReflectionQuestion(
                    FALLBACK_REFLECTION_QUESTION,
                    QuestionGenerationSource.FALLBACK
            );
        }
    }

    private record GeneratedReflectionQuestion(
            String questionText,
            QuestionGenerationSource
            generationSource
    ) {
    }

    private Diary findActiveDiary(
            Long userId,
            Long diaryId
    ) {
        return diaryRepository
                .findByIdAndUserIdAndDeletedFalse(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.DIARY_NOT_FOUND
                        )
                );
    }
}