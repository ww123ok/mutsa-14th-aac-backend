package mutsa.hackathon.service;

@FunctionalInterface
public interface WeeklyImageGenerator {
    GeneratedWeeklyImage generate(
            WeeklyRewardGenerationContext context,
            WeeklyVisualPlan visualPlan
    );
}
