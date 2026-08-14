package mutsa.hackathon.service;

@FunctionalInterface
public interface WeeklyRewardInsightGenerator {
    WeeklyRewardInsight generate(WeeklyRewardGenerationContext context);
}
