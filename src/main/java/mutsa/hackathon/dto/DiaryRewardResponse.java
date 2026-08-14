package mutsa.hackathon.dto;

import mutsa.hackathon.domain.DiaryReward;

import java.util.List;

public record DiaryRewardResponse(
        Long diaryId,
        String status,
        String colorHex,
        List<String> keywords,
        String colorComment
) {

    public static DiaryRewardResponse from(
            DiaryReward reward
    ) {
        if (reward == null) {
            throw new IllegalArgumentException(
                    "일기 색상 보상은 필수입니다."
            );
        }

        return new DiaryRewardResponse(
                reward.getDiary().getId(),
                reward.getGenerationStatus().name(),
                reward.getColorHex(),
                reward.getKeywords(),
                reward.getColorComment()
        );
    }
}