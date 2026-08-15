package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryTrashResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deletedAt,

        RewardSummary reward
) {

    public static DiaryTrashResponse from(
            Diary diary,
            DiaryReward reward
    ) {
        if (diary == null || !diary.isDeleted()) {
            throw new IllegalArgumentException(
                    "휴지통 일기 정보는 필수입니다."
            );
        }

        return new DiaryTrashResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt(),
                diary.getDeletedAt(),
                RewardSummary.from(reward)
        );
    }

    public record RewardSummary(
            String status,
            String colorHex,
            List<String> keywords,
            String colorComment
    ) {

        private static RewardSummary from(
                DiaryReward reward
        ) {
            if (reward == null) {
                return null;
            }

            return new RewardSummary(
                    reward.getGenerationStatus().name(),
                    reward.getColorHex(),
                    reward.getKeywords(),
                    reward.getColorComment()
            );
        }
    }
}
