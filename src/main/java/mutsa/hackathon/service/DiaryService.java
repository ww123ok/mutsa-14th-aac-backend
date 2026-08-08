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

    private final DiaryRepository diaryRepository;

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

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DiaryCreateResponse create(
            Long userId,
            DiaryCreateRequest request
    ) {
        LocalDate today =
                LocalDate.now(SERVICE_ZONE);

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

        Diary diary = saveDiary(
                user,
                request.content(),
                today
        );

        DiaryReward reward =
                diaryRewardRepository.save(
                        DiaryReward
                                .createPending(diary)
                );

        GeneratedReflectionQuestion generatedQuestion =
                generateReflectionQuestion(diary.getContent());

        AiQuestion reflectionQuestion =
                aiQuestionRepository.save(
                        AiQuestion.createReflection(
                                user,
                                diary,
                                generatedQuestion.questionText(),
                                today,
                                generatedQuestion.generationSource()
                        )
                );

        eventPublisher.publishEvent(
                new DiaryRewardGenerationRequested(
                        reward.getId()
                )
        );

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

        List<Diary> diaries = diaryRepository
                .findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                        userId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                );

        if (diaries.isEmpty()) {
            return List.of();
        }

        Map<Long, DiaryReward> rewardsByDiaryId = diaryRewardRepository
                .findAllByDiaryIdIn(
                        diaries.stream().map(Diary::getId).toList()
                )
                .stream()
                .collect(
                        Collectors.toMap(
                                reward -> reward.getDiary().getId(),
                                Function.identity()
                        )
                );

        return diaries.stream()
                .map(diary -> DiaryResponse.from(
                        diary,
                        rewardsByDiaryId.get(diary.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryResponse getDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findActiveDiary(userId, diaryId);
        DiaryReward reward = diaryRewardRepository
                .findByDiaryId(diaryId)
                .orElse(null);

        return DiaryResponse.from(diary, reward);
    }

    @Transactional
    public void deleteDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findActiveDiary(
                userId,
                diaryId
        );

        diary.softDelete();

        /*
         * 삭제된 일기의 내용을 바탕으로 생성된 기억이
         * 이후 질문에 계속 사용되지 않도록 폐기함.
         * 다른 일기에서 나온 승인 기억은 유지.
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
            return diaryRepository.saveAndFlush(
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

    private GeneratedReflectionQuestion generateReflectionQuestion(
            String diaryContent
    ) {
        try {
            return new GeneratedReflectionQuestion(
                    diaryReflectionQuestionGenerator.generate(
                            diaryContent
                    ),
                    QuestionGenerationSource.AI
            );
        } catch (RuntimeException exception) {
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
