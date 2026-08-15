package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class WeeklyRewardDispatcher {

    private final WeeklyRewardGenerationService generationService;

    @Async("weeklyRewardExecutor")
    public void dispatch(Long weeklyRewardId) {
        generationService.generate(weeklyRewardId);
    }
}