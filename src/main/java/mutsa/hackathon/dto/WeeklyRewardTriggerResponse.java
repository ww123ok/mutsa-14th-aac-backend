package mutsa.hackathon.dto;

import java.time.LocalDate;

public record WeeklyRewardTriggerResponse(
        boolean eligible,
        Long weeklyRewardId,
        LocalDate weekStartDate,
        String message
) {
    public static WeeklyRewardTriggerResponse eligible(
            Long rewardId,
            LocalDate weekStartDate
    ) {
        return new WeeklyRewardTriggerResponse(
                true,
                rewardId,
                weekStartDate,
                "주간 보상 생성을 요청했습니다."
        );
    }

    public static WeeklyRewardTriggerResponse notEligible(LocalDate weekStartDate) {
        return new WeeklyRewardTriggerResponse(
                false,
                null,
                weekStartDate,
                "일기가 3개 미만이거나 일간 색 보상을 준비 중입니다."
        );
    }
}