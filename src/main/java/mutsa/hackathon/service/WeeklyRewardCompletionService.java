package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void complete(
            Long rewardId,
            WeeklyRewardResultText resultText,
            GeneratedWeeklyImage image,
            StoredWeeklyImage storedImage
    ) {
        WeeklyReward reward = weeklyRewardRepository.findByIdForUpdate(rewardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 주간 보상입니다."
                ));

        reward.complete(
                resultText.title(),
                resultText.summary(),
                resultText.keywords(),
                storedImage.key(),
                storedImage.contentType(),
                image.source(),
                LocalDateTime.now(weeklyRewardClock)
        );

        eventPublisher.publishEvent(
                InAppNotificationRequested.weeklyRewardCompleted(
                        reward.getUser().getId(),
                        reward.getId()
                )
        );
    }

    @Transactional
    public void fail(Long rewardId, String failureReason) {
        weeklyRewardRepository.findByIdForUpdate(rewardId)
                .ifPresent(reward -> reward.fail(failureReason));
    }
}
