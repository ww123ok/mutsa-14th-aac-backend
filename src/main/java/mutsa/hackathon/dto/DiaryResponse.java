package mutsa.hackathon.dto;

import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,
        LocalDateTime createdAt,
        RewardSummary reward
) {

    public static DiaryResponse from(Diary diary, DiaryReward reward) {
        return new DiaryResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt(),
                RewardSummary.from(reward)
        );
    }

    public record RewardSummary(
            String status,
            String colorHex,
            String colorName
    ) {
        private static RewardSummary from(DiaryReward reward) {
            if (reward == null) {
                return null;
            }

            return new RewardSummary(
                    reward.getGenerationStatus().name(),
                    reward.getColorHex(),
                    reward.getColorName()
            );
        }
    }
}
