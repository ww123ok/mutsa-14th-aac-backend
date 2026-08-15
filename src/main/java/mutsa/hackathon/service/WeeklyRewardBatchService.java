package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import mutsa.hackathon.dto.WeeklyRewardTriggerResponse;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import mutsa.hackathon.util.WeeklyRewardPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardBatchService {

    private final DiaryRepository diaryRepository;
    private final WeeklyRewardRepository weeklyRewardRepository;
    private final WeeklyRewardPreparationService preparationService;
    private final WeeklyRewardDispatcher dispatcher;

    @Value("${app.weekly-reward.minimum-diary-count:3}")
    private int minimumDiaryCount;

    public BatchResult generateWeek(LocalDate weekStartDate) {
        WeeklyRewardPeriod period = WeeklyRewardPeriod.fromStart(weekStartDate);
        Set<Long> dispatchedRewardIds = new LinkedHashSet<>();

        List<Long> retryableIds = weeklyRewardRepository.findIdsByWeekAndStatuses(
                period.startDate(),
                EnumSet.of(
                        WeeklyRewardStatus.PENDING,
                        WeeklyRewardStatus.FAILED,
                        WeeklyRewardStatus.GENERATING
                )
        );
        retryableIds.forEach(rewardId -> dispatchOnce(
                rewardId,
                dispatchedRewardIds
        ));

        List<Long> eligibleUserIds = diaryRepository
                .findEligibleUserIdsForWeek(
                        period.startDate(),
                        period.endDate(),
                        minimumDiaryCount
                );

        int preparedCount = 0;
        for (Long userId : eligibleUserIds) {
            try {
                Optional<Long> rewardId = preparationService.prepare(
                        userId,
                        period.startDate()
                );
                if (rewardId.isPresent()) {
                    preparedCount++;
                    dispatchOnce(rewardId.get(), dispatchedRewardIds);
                }
            } catch (RuntimeException exception) {
                Optional<Long> existingId = preparationService.findExistingId(
                        userId,
                        period.startDate()
                );
                if (existingId.isPresent()) {
                    dispatchOnce(existingId.get(), dispatchedRewardIds);
                    continue;
                }
                log.warn(
                        "Weekly reward preparation failed: userId={}, weekStart={}, reason={}",
                        userId,
                        period.startDate(),
                        exception.getClass().getSimpleName()
                );
            }
        }

        return new BatchResult(
                period.startDate(),
                eligibleUserIds.size(),
                preparedCount,
                dispatchedRewardIds.size()
        );
    }

    public WeeklyRewardTriggerResponse generateForUser(
            Long userId,
            LocalDate weekStartDate
    ) {
        WeeklyRewardPeriod.fromStart(weekStartDate);
        Optional<Long> rewardId;
        try {
            rewardId = preparationService.prepare(userId, weekStartDate);
        } catch (RuntimeException exception) {
            rewardId = preparationService.findExistingId(userId, weekStartDate);
            if (rewardId.isEmpty()) {
                throw exception;
            }
        }
        if (rewardId.isEmpty()) {
            return WeeklyRewardTriggerResponse.notEligible(weekStartDate);
        }
        dispatcher.dispatch(rewardId.get());
        return WeeklyRewardTriggerResponse.eligible(
                rewardId.get(),
                weekStartDate
        );
    }

    private void dispatchOnce(Long rewardId, Set<Long> dispatchedIds) {
        if (dispatchedIds.add(rewardId)) {
            dispatcher.dispatch(rewardId);
        }
    }

    public record BatchResult(
            LocalDate weekStartDate,
            int eligibleUserCount,
            int preparedCount,
            int dispatchedCount
    ) {
    }
}