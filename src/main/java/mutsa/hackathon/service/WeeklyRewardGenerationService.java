package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardGenerationService {

    private static final String FAILURE_REASON = "WEEKLY_REWARD_GENERATION_FAILED";

    private final WeeklyRewardClaimService claimService;
    private final OpenAiWeeklyRewardInsightGenerator insightGenerator;
    private final FallbackWeeklyRewardInsightFactory fallbackInsightFactory;
    private final OpenAiWeeklyImageGenerator imageGenerator;
    private final FallbackWeeklyPosterGenerator fallbackPosterGenerator;
    private final WeeklyImageStorage imageStorage;
    private final WeeklyRewardCompletionService completionService;

    public void generate(Long weeklyRewardId) {
        WeeklyRewardClaimService.ClaimResult claim = claimService.claim(weeklyRewardId);
        if (!claim.claimed()) {
            return;
        }

        WeeklyRewardGenerationContext context = claim.context();
        StoredWeeklyImage storedImage = null;

        try {
            WeeklyRewardInsight insight = createInsight(context);
            GeneratedWeeklyImage image = createImage(context, insight);
            storedImage = imageStorage.store(context, image);

            completionService.complete(
                    weeklyRewardId,
                    insight,
                    image,
                    storedImage
            );
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                deleteQuietly(storedImage.key());
            }
            log.warn(
                    "Weekly reward generation failed: weeklyRewardId={}, reason={}",
                    weeklyRewardId,
                    exception.getClass().getSimpleName()
            );
            completionService.fail(weeklyRewardId, FAILURE_REASON);
        }
    }

    private WeeklyRewardInsight createInsight(
            WeeklyRewardGenerationContext context
    ) {
        try {
            return insightGenerator.generate(context);
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly insight fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackInsightFactory.create(context);
        }
    }

    private GeneratedWeeklyImage createImage(
            WeeklyRewardGenerationContext context,
            WeeklyRewardInsight insight
    ) {
        try {
            return imageGenerator.generate(context, insight);
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly poster fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackPosterGenerator.generate(context, insight);
        }
    }

    private void deleteQuietly(String key) {
        try {
            imageStorage.delete(key);
        } catch (RuntimeException deleteException) {
            log.warn(
                    "Orphan weekly image cleanup failed: reason={}",
                    deleteException.getClass().getSimpleName()
            );
        }
    }
}