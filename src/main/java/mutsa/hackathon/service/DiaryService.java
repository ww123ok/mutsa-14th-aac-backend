package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryDetailResponse;
import mutsa.hackathon.dto.DiaryHiddenResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private static final String
            FALLBACK_REFLECTION_QUESTION =
            "오늘의 기록에서 가장 오래 마음에 남은 순간은 무엇인가요?";

    private final DiaryRepository
            diaryRepository;

    private final DiaryRewardRepository
            diaryRewardRepository;

    private final AiQuestionRepository
            aiQuestionRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    private final DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    private final DiaryCreatePersistenceService
            diaryCreatePersistenceService;

    private final UserDayService
            userDayService;

    /**
     * 이 메서드에는 의도적으로 @Transactional을 붙이지 않음.
     * 1. 짧은 read-only transaction으로 작성 가능 여부 확인
     * 2. transaction 밖에서 OpenAI 성찰 질문 호출
     * 3. 짧은 write transaction으로 결과 저장
     */
    public DiaryCreateResponse create(
            Long userId,
            DiaryCreateRequest request
    ) {
        LocalDate today =
                userDayService.currentDay(
                        userId
                );

        return createForRecordedDate(
                userId,
                request,
                today
        );
    }

    /**
     * 개발용 날짜 지정 일기 API가 기존 생성 흐름을
     * 그대로 재사용할 수 있도록 제공하는 메서드.
     *
     * 일반 사용자용 Controller는 이 메서드를 직접
     * 노출하지 않으며 기존 create()를 계속 사용합니다.
     */
    public DiaryCreateResponse createForRecordedDate(
            Long userId,
            DiaryCreateRequest request,
            LocalDate recordedDate
    ) {
        return createForRecordedDate(
                userId,
                request,
                recordedDate,
                false
        );
    }

    /**
     * DAYBIT 하루 경계를 넘긴 임시 저장 일기를
     * 기존 일기 생성 파이프라인으로 완료.
     * 일반 생성과 동일하게 성찰 질문을 만든 뒤,
     * DB 저장 transaction에서 자동 완료 안내까지 함께 남김.
     */
    public DiaryCreateResponse
    autoCompleteForRecordedDate(
            Long userId,
            DiaryCreateRequest request,
            LocalDate recordedDate
    ) {
        return createForRecordedDate(
                userId,
                request,
                recordedDate,
                true
        );
    }

    private DiaryCreateResponse createForRecordedDate(
            Long userId,
            DiaryCreateRequest request,
            LocalDate recordedDate,
            boolean autoCompleted
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "일기 작성 요청은 필수입니다."
            );
        }

        if (recordedDate == null) {
            throw new IllegalArgumentException(
                    "일기 작성 날짜는 필수입니다."
            );
        }

        diaryCreatePersistenceService
                .validateCanCreate(
                        userId,
                        recordedDate
                );

        GeneratedReflectionQuestion
                generatedQuestion =
                generateReflectionQuestion(
                        request.content()
                );

        if (autoCompleted) {
            return diaryCreatePersistenceService
                    .persistAutoCompleted(
                            userId,
                            request,
                            recordedDate,
                            generatedQuestion
                                    .questionText(),
                            generatedQuestion
                                    .generationSource()
                    );
        }

        return diaryCreatePersistenceService
                .persist(
                        userId,
                        request,
                        recordedDate,
                        generatedQuestion
                                .questionText(),
                        generatedQuestion
                                .generationSource()
                );
    }

    /**
     * 월간 아카이브의 공식 조회.
     * 한 번의 요청으로 해당 월의 모든 일기와
     * 날짜별 색 보상 정보를 제공.
     * 프론트엔드는 recordedDate + reward.colorHex를
     * 이용하여 달력을 구성하고,
     * 동일 응답의 content / createdAt / diaryId를
     * 이용하여 목록을 구성할 수 있음.
     */
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
                        .findVisibleByUserIdAndRecordedDateBetween(
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

    /**
     * 숨김 일기 목록 조회.
     * 숨김은 삭제와 별개의 상태이며 일반 월간 아카이브에서만
     * 제외됩니다. 숨김 목록에서는 최근 숨긴 일기부터 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<DiaryHiddenResponse> getHiddenDiaries(
            Long userId
    ) {
        List<Diary> diaries =
                diaryRepository
                        .findAllByUserIdAndDeletedFalseAndHiddenTrueOrderByHiddenAtDesc(
                                userId
                        );

        if (diaries.isEmpty()) {
            return List.of();
        }

        Map<Long, DiaryReward> rewardsByDiaryId =
                diaryRewardRepository
                        .findAllByDiaryIdIn(
                                diaries.stream()
                                        .map(Diary::getId)
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
                        DiaryHiddenResponse.from(
                                diary,
                                rewardsByDiaryId.get(
                                        diary.getId()
                                )
                        )
                )
                .toList();
    }

    @Transactional
    public void hideDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary =
                findActiveDiary(
                        userId,
                        diaryId
                );

        diary.hide();
    }

    @Transactional
    public void unhideDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary =
                findActiveDiary(
                        userId,
                        diaryId
                );

        diary.unhide();
    }

    /**
     * 아카이브 상세 조회.
     * 일기 본문과 색 보상뿐 아니라
     * 그날 생성된 성찰 질문과 선택적으로 작성한
     * 성찰 답변까지 함께 반환.
     */
    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiary(
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

        AiQuestion reflectionQuestion =
                aiQuestionRepository
                        .findByDiaryIdAndQuestionType(
                                diaryId,
                                AiQuestionType.REFLECTION
                        )
                        .orElse(null);

        return DiaryDetailResponse.from(
                diary,
                reward,
                reflectionQuestion
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
            QuestionGenerationSource generationSource
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
