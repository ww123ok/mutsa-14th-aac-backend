package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.DiaryReward;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.RewardGenerationStatus;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import mutsa.hackathon.util.WeeklyRewardPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class WeeklyRewardPreparationService {

    private final AppUserRepository appUserRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryRewardRepository diaryRewardRepository;
    private final WeeklyRewardRepository weeklyRewardRepository;
    private final WeeklyRewardEntryRepository weeklyRewardEntryRepository;
    private final WeeklyFallbackDailyColorGenerator fallbackDailyColorGenerator;
    private final Clock weeklyRewardClock;

    @Value("${app.weekly-reward.minimum-diary-count:3}")
    private int minimumDiaryCount;

    @Value("${app.weekly-reward.daily-reward-wait-minutes:10}")
    private long dailyRewardWaitMinutes;

    @Transactional
    public Optional<Long> prepare(
            Long userId,
            LocalDate weekStartDate
    ) {
        WeeklyRewardPeriod period = WeeklyRewardPeriod.fromStart(weekStartDate);

        Optional<WeeklyReward> existing = weeklyRewardRepository
                .findByUserIdAndWeekStartDate(userId, weekStartDate);
        if (existing.isPresent()) {
            return existing.map(WeeklyReward::getId);
        }

        List<Diary> diaries = diaryRepository
                .findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                        userId,
                        period.startDate(),
                        period.endDate()
                );

        if (diaries.size() < minimumDiaryCount) {
            return Optional.empty();
        }

        Map<Long, DiaryReward> rewardsByDiaryId = diaryRewardRepository
                .findAllByDiaryIdIn(diaries.stream().map(Diary::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        reward -> reward.getDiary().getId(),
                        Function.identity()
                ));

        LocalDateTime waitBoundary = LocalDateTime.now(weeklyRewardClock)
                .minusMinutes(dailyRewardWaitMinutes);
        boolean hasFreshPendingReward = diaries.stream()
                .map(diary -> rewardsByDiaryId.get(diary.getId()))
                .filter(java.util.Objects::nonNull)
                .anyMatch(reward ->
                        reward.getGenerationStatus() == RewardGenerationStatus.PENDING
                                && reward.getUpdatedAt() != null
                                && reward.getUpdatedAt().isAfter(waitBoundary)
                );
        if (hasFreshPendingReward) {
            return Optional.empty();
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        WeeklyReward weeklyReward = weeklyRewardRepository.saveAndFlush(
                WeeklyReward.createPending(
                        user,
                        period.startDate(),
                        period.endDate()
                )
        );

        List<WeeklyRewardEntry> entries = diaries.stream()
                .map(diary -> createEntry(
                        weeklyReward,
                        diary,
                        rewardsByDiaryId.get(diary.getId())
                ))
                .toList();
        weeklyRewardEntryRepository.saveAll(entries);
        weeklyRewardEntryRepository.flush();
        return Optional.of(weeklyReward.getId());
    }

    @Transactional(readOnly = true)
    public Optional<Long> findExistingId(
            Long userId,
            LocalDate weekStartDate
    ) {
        return weeklyRewardRepository
                .findByUserIdAndWeekStartDate(userId, weekStartDate)
                .map(WeeklyReward::getId);
    }

    private WeeklyRewardEntry createEntry(
            WeeklyReward weeklyReward,
            Diary diary,
            DiaryReward dailyReward
    ) {
        if (
                dailyReward != null
                        && dailyReward.getGenerationStatus() == RewardGenerationStatus.COMPLETED
        ) {
            return WeeklyRewardEntry.from(weeklyReward, dailyReward);
        }
        return WeeklyRewardEntry.fallback(
                weeklyReward,
                diary,
                fallbackDailyColorGenerator.generate(diary.getContent())
        );
    }
}