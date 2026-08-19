package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import mutsa.hackathon.dto.DiaryTrashDetailResponse;
import mutsa.hackathon.dto.DiaryTrashResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AiQuestionRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryCommentRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryShareRepository;
import mutsa.hackathon.repository.ExperienceFragmentArrivalRepository;
import mutsa.hackathon.repository.SharedDiaryLogRepository;
import mutsa.hackathon.repository.UserMemoryItemRepository;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryTrashService {

    private final DiaryRepository diaryRepository;
    private final DiaryCommentRepository diaryCommentRepository;
    private final DiaryRewardRepository diaryRewardRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final UserMemoryItemRepository userMemoryItemRepository;
    private final DiaryShareRepository diaryShareRepository;
    private final ExperienceFragmentArrivalRepository experienceFragmentArrivalRepository;
    private final SharedDiaryLogRepository sharedDiaryLogRepository;
    private final WeeklyRewardEntryRepository weeklyRewardEntryRepository;
    private final WeeklyRewardRepository weeklyRewardRepository;
    private final AiMemoryProfileService aiMemoryProfileService;

    @Transactional(readOnly = true)
    public List<DiaryTrashResponse> getTrash(
            Long userId
    ) {
        List<Diary> diaries = diaryRepository
                .findAllByUserIdAndDeletedTrueOrderByDeletedAtDesc(
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
                                        reward -> reward
                                                .getDiary()
                                                .getId(),
                                        Function.identity()
                                )
                        );

        return diaries.stream()
                .map(diary -> DiaryTrashResponse.from(
                        diary,
                        rewardsByDiaryId.get(diary.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DiaryTrashDetailResponse getTrashDiary(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findTrashedDiary(
                userId,
                diaryId
        );

        DiaryReward reward = diaryRewardRepository
                .findByDiaryId(diaryId)
                .orElse(null);

        AiQuestion reflectionQuestion = aiQuestionRepository
                .findByDiaryIdAndQuestionType(
                        diaryId,
                        AiQuestionType.REFLECTION
                )
                .orElse(null);

        return DiaryTrashDetailResponse.from(
                diary,
                reward,
                reflectionQuestion
        );
    }

    @Transactional
    public void restore(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findTrashedDiary(
                userId,
                diaryId
        );

        diary.restore();
    }

    @Transactional
    public void permanentlyDelete(
            Long userId,
            Long diaryId
    ) {
        Diary diary = findTrashedDiary(
                userId,
                diaryId
        );

        deleteArrivalsUsingQueryDiary(diaryId);
        deleteExperienceFragment(diaryId);
        invalidateUnfinishedWeeklyRewards(diaryId);

        weeklyRewardEntryRepository
                .deleteAllByDiaryId(diaryId);

        userMemoryItemRepository
                .deleteAllByUserIdAndSourceDiaryId(
                        userId,
                        diaryId
                );

        diaryCommentRepository
                .deleteAllByDiaryId(
                        diaryId
                );

        aiQuestionRepository
                .deleteAllByDiaryId(diaryId);

        diaryRewardRepository
                .deleteAllByDiaryId(diaryId);

        diaryRepository.delete(diary);
        diaryRepository.flush();

        aiMemoryProfileService.rebuildProfile(
                userId
        );
    }

    /**
     * 이 일기를 매칭 기준(query diary)으로 생성된 수신함 도착 정보는
     * 원본 일기보다 먼저 제거한다. 이미 수신이 끝난 전달 기록은
     * queryDiary FK를 사용하지 않으므로 그대로 보존됨.
     */
    private void deleteArrivalsUsingQueryDiary(
            Long diaryId
    ) {
        experienceFragmentArrivalRepository
                .deleteAllByQueryDiaryId(diaryId);
        experienceFragmentArrivalRepository.flush();
    }

    private void deleteExperienceFragment(
            Long diaryId
    ) {
        diaryShareRepository
                .findByDiaryId(diaryId)
                .ifPresent(share -> {
                    experienceFragmentArrivalRepository
                            .deleteAllByDiaryShareId(
                                    share.getId()
                            );
                    experienceFragmentArrivalRepository.flush();

                    sharedDiaryLogRepository
                            .deleteAllByDiaryShareId(
                                    share.getId()
                            );
                    sharedDiaryLogRepository.flush();

                    diaryShareRepository.delete(share);
                    diaryShareRepository.flush();
                });
    }

    /**
     * 완료된 주간 보상은 이미 생성이 끝난 하나의 결과물이므로
     * 보상 자체는 유지하고 해당 일기의 DailyColor 연결만 제거한다.
     *
     * 반대로 PENDING / GENERATING / FAILED 보상은 아직 소스 집합이
     * 확정된 결과물이 아니므로 해당 일기를 영구 삭제할 때 보상 준비
     * 데이터 전체를 폐기한다. 이후 스케줄러가 남은 일기로 조건을 다시
     * 만족하면 새로운 주간 보상을 준비할 수 있다.
     */
    private void invalidateUnfinishedWeeklyRewards(
            Long diaryId
    ) {
        List<WeeklyRewardEntry> linkedEntries =
                weeklyRewardEntryRepository
                        .findAllByDiaryId(diaryId);

        Set<Long> invalidRewardIds =
                linkedEntries.stream()
                        .map(WeeklyRewardEntry::getWeeklyReward)
                        .filter(reward ->
                                reward.getGenerationStatus()
                                        != WeeklyRewardStatus.COMPLETED
                        )
                        .map(reward -> reward.getId())
                        .collect(Collectors.toCollection(
                                LinkedHashSet::new
                        ));

        for (Long weeklyRewardId : invalidRewardIds) {
            weeklyRewardEntryRepository
                    .deleteAllByWeeklyRewardId(
                            weeklyRewardId
                    );
            weeklyRewardEntryRepository.flush();

            weeklyRewardRepository
                    .deleteById(weeklyRewardId);
            weeklyRewardRepository.flush();
        }
    }

    private Diary findTrashedDiary(
            Long userId,
            Long diaryId
    ) {
        return diaryRepository
                .findByIdAndUserIdAndDeletedTrue(
                        diaryId,
                        userId
                )
                .orElseThrow(() ->
                        new ProjectException(
                                ErrorCode.TRASH_DIARY_NOT_FOUND
                        )
                );
    }
}
