package mutsa.hackathon.service;

public interface WeeklyVisualPlanGenerator {

    WeeklyVisualPlan generate(
            WeeklyRewardGenerationContext context
    );
}
