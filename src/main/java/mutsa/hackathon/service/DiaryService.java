package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.QuestionGenerationSource;
import mutsa.hackathon.dto.DiaryCreateRequest;
import mutsa.hackathon.dto.DiaryCreateResponse;
import mutsa.hackathon.dto.DiaryResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final AiQuestionRepository
            aiQuestionRepository;

    private final AppUserRepository
            appUserRepository;

    private final AiMemoryProfileService
            aiMemoryProfileService;

    private final DiaryReflectionQuestionGenerator
            diaryReflectionQuestionGenerator;

    private final ApplicationEventPublisher
            eventPublisher;

    @Transactional
    public DiaryCreateResponse create(
            Long userId,
            DiaryCreateRequest request
    ) {
        LocalDate today =
                LocalDate.now(
                        SERVICE_ZONE
                );

        validateDiaryNotWrittenToday(
                userId,
                today
        );

        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        Diary diary =
                saveDiary(
                        user,
                        request.content(),
                        today
                );

        DiaryReward reward =
                diaryRewardRepository.save(
                        DiaryReward.createPending(
                                diary
                        )
                );

        /*
         * 최신 기획에서는 성찰 질문이
         * 항상 오늘 작성한 일기 내용만을 사용.
         * 개인화 기억 반영 동의와 성찰 질문 생성은
         * 서로 별개의 개념.
         */
        GeneratedReflectionQuestion generatedQuestion =
                generateReflectionQuestion(
                        diary.getContent()
                );

        AiQuestion reflectionQuestion =
                aiQuestionRepository.save(
                        AiQuestion.createReflection(
                                user,
                                diary,
                                generatedQuestion
                                        .questionText(),
                                today,
                                generatedQuestion
                                        .generationSource()
                        )
                );

        /*
         * 색상 생성은 항상 수행
         */
        eventPublisher.publishEvent(
                new DiaryRewardGenerationRequested(
                        reward.getId()
                )
        );

        /*
         * 다음 작성 도움 질문에 오늘의 정보를
         * 활용하겠다고 사용자가 선택했고,
         * 동시에 전역 AI 기억 동의가 활성화된 경우에만
         * 개인화 기억 추출을 요청
         */
        if (
                request
                        .shouldUseDiaryContentForPersonalization()
                        && user.isAiMemoryConsent()
        ) {
            eventPublisher.publishEvent(
                    new DiaryMemoryExtractionRequested(
                            diary.getId()
                    )
            );
        }

        return DiaryCreateResponse.from(
                diary,
                reward,
                reflectionQuestion
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
         * 이후 질문에 사용되지 않도록 폐기
         */
        aiMemoryProfileService
                .revokeMemoriesFromDiary(
                        userId,
                        diaryId
                );
    }

    private void validateDiaryNotWrittenToday(
            Long userId,
            LocalDate today
    ) {
        if (
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                userId,
                                today
                        )
        ) {
            throw new ProjectException(
                    ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            );
        }
    }

    private Diary saveDiary(
            AppUser user,
            String content,
            LocalDate today
    ) {
        try {
            return diaryRepository
                    .saveAndFlush(
                            Diary.create(
                                    user,
                                    content,
                                    today
                            )
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new ProjectException(
                    ErrorCode
                            .DIARY_ALREADY_WRITTEN_TODAY
            );
        }
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
