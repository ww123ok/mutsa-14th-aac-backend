package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import mutsa.hackathon.repository.WeeklyRewardRepository;
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
    private static final String IMAGE_FAILURE_REASON = "WEEKLY_REWARD_IMAGE_GENERATION_FAILED";
    private static final String RESULT_TEXT_FAILURE_REASON = "WEEKLY_REWARD_RESULT_TEXT_GENERATION_FAILED";
    private static final String STORAGE_FAILURE_REASON = "WEEKLY_REWARD_IMAGE_STORAGE_FAILED";
    private static final String COMPLETION_FAILURE_REASON = "WEEKLY_REWARD_COMPLETION_FAILED";

    private final WeeklyRewardClaimService claimService;
    private final WeeklyRewardRepository weeklyRewardRepository;
    private final OpenAiWeeklyVisualPlanGenerator visualPlanGenerator;
    private final FallbackWeeklyVisualPlanFactory fallbackVisualPlanFactory;
    private final OpenAiWeeklyImageGenerator imageGenerator;
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
        GenerationStage stage = GenerationStage.VISUAL_PLAN;

        try {
            WeeklyVisualCategory previousWeekCategory =
                    findPreviousWeekCategory(context);
            WeeklyVisualPlan visualPlan = createVisualPlan(
                    context,
                    previousWeekCategory
            );

            log.info(
                    "Weekly visual plan selected: weeklyRewardId={}, category={}, motifLength={}",
                    weeklyRewardId,
                    visualPlan.visualCategory(),
                    visualPlan.visualMotif().length()
            );

            stage = GenerationStage.IMAGE;
            GeneratedWeeklyImage image = createImage(context, visualPlan);

            stage = GenerationStage.RESULT_TEXT;
            WeeklyRewardResultText resultText = createResultText(
                    context,
                    visualPlan,
                    image
            );

            stage = GenerationStage.STORAGE;
            storedImage = imageStorage.store(context, image);

            stage = GenerationStage.COMPLETION;
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

            String failureReason = failureReason(stage);
            log.error(
                    "Weekly reward generation failed: weeklyRewardId={}, stage={}, "
                            + "failureReason={}, exceptionType={}, message={}",
                    weeklyRewardId,
                    stage,
                    failureReason,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            completionService.fail(weeklyRewardId, failureReason);
        }
    }

    private WeeklyVisualPlan createVisualPlan(
            WeeklyRewardGenerationContext context,
            WeeklyVisualCategory previousWeekCategory
    ) {
        try {
            return visualPlanGenerator.generate(
                    context,
                    previousWeekCategory
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Weekly visual plan fallback used: weeklyRewardId={}, reason={}",
                    context.weeklyRewardId(),
                    exception.getClass().getSimpleName()
            );
            return fallbackVisualPlanFactory.create(
                    context,
                    previousWeekCategory
            );
        }
    }

    private WeeklyVisualCategory findPreviousWeekCategory(
            WeeklyRewardGenerationContext context
    ) {
        return weeklyRewardRepository
                .findByUserIdAndWeekStartDate(
                        context.userId(),
                        context.weekStartDate().minusWeeks(1)
                )
                .filter(reward ->
                        reward.getGenerationStatus()
                                == WeeklyRewardStatus.COMPLETED
                )
                .map(WeeklyReward::getCategoryKeyword)
                .map(this::categoryFromKeyword)
                .orElse(null);
    }

    private WeeklyVisualCategory categoryFromKeyword(
            String categoryKeyword
    ) {
        if (categoryKeyword == null || categoryKeyword.isBlank()) {
            return null;
        }

        return switch (categoryKeyword.trim()) {
            case "그래픽 포스터" -> WeeklyVisualCategory.GRAPHIC_POSTER;
            case "3D캐릭터" -> WeeklyVisualCategory.NON_HUMAN_CHARACTER;
            case "유화" -> WeeklyVisualCategory.OIL_ACRYLIC;
            case "LP커버" -> WeeklyVisualCategory.ALBUM_COVER;
            case "픽셀아트" -> WeeklyVisualCategory.PIXEL_ART;
            case "실사 풍경" -> WeeklyVisualCategory.PHOTO_LANDSCAPE;
            default -> null;
        };
    }

    private GeneratedWeeklyImage createImage(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    ) {
        return imageGenerator.generate(context, visualPlan);
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
    private String failureReason(GenerationStage stage) {
        return switch (stage) {
            case IMAGE -> IMAGE_FAILURE_REASON;
            case RESULT_TEXT -> RESULT_TEXT_FAILURE_REASON;
            case STORAGE -> STORAGE_FAILURE_REASON;
            case COMPLETION -> COMPLETION_FAILURE_REASON;
            case VISUAL_PLAN -> FAILURE_REASON;
        };
    }

    private enum GenerationStage {
        VISUAL_PLAN,
        IMAGE,
        RESULT_TEXT,
        STORAGE,
        COMPLETION
    }

}
