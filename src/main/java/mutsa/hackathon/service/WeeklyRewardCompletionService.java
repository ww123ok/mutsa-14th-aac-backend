package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class WeeklyRewardCompletionService {

    private final WeeklyRewardRepository weeklyRewardRepository;
    private final Clock weeklyRewardClock;

    @Transactional
    public void complete(
            Long rewardId,
            WeeklyRewardInsight insight,
            GeneratedWeeklyImage image,
            StoredWeeklyImage storedImage
    ) {
        WeeklyReward reward = weeklyRewardRepository.findByIdForUpdate(rewardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 주간 보상입니다."
                ));

        reward.complete(
                insight.title(),
                insight.summary(),
                insight.keywords(),
                storedImage.key(),
                storedImage.contentType(),
                image.source(),
                LocalDateTime.now(weeklyRewardClock)
        );
    }

    @Transactional
    public void fail(Long rewardId, String failureReason) {
        weeklyRewardRepository.findByIdForUpdate(rewardId)
                .ifPresent(reward -> reward.fail(failureReason));
    }
}