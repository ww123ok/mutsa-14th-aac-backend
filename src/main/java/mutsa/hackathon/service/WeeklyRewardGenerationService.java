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
    private final OpenAiWeeklyVisualPlanGenerator visualPlanGenerator;
    private final FallbackWeeklyVisualPlanFactory fallbackVisualPlanFactory;
    private final OpenAiWeeklyImageGenerator imageGenerator;
    private final FallbackWeeklyPosterGenerator fallbackPosterGenerator;
    private final OpenAiWeeklyRewardResultTextGenerator resultTextGenerator;
    private final FallbackWeeklyRewardResultTextFactory fallbackResultTextFactory;
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
            WeeklyVisualPlan visualPlan = createVisualPlan(context);
            GeneratedWeeklyImage image = createImage(context, visualPlan);
            WeeklyRewardResultText resultText = createResultText(
                    context,
                    visualPlan,
                    image
            );
            storedImage = imageStorage.store(context, image);

            completionService.complete(
                    weeklyRewardId,
                    resultText,
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

    private WeeklyVisualPlan createVisualPlan(
            WeeklyRewardGenerationContext context
    ) {
        try {
            return visualPlanGenerator.generate(context);
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly visual plan fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackVisualPlanFactory.create(context);
        }
    }

    private GeneratedWeeklyImage createImage(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        try {
            return imageGenerator.generate(context, visualPlan);
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly poster fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackPosterGenerator.generate(context, visualPlan);
        }
    }

    private WeeklyRewardResultText createResultText(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan,
            GeneratedWeeklyImage image
    ) {
        try {
            return resultTextGenerator.generate(
                    context,
                    visualPlan,
                    image
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly result text fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackResultTextFactory.create(
                    context,
                    visualPlan
            );
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
