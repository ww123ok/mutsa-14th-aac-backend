package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryTrashDetailResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime deletedAt,

        RewardSummary reward,
        ReflectionSummary reflection
) {

    public static DiaryTrashDetailResponse from(
            Diary diary,
            DiaryReward reward,
            AiQuestion reflectionQuestion
    ) {
        if (diary == null || !diary.isDeleted()) {
            throw new IllegalArgumentException(
                    "휴지통 일기 정보는 필수입니다."
            );
        }

        return new DiaryTrashDetailResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt(),
                diary.getDeletedAt(),
                RewardSummary.from(reward),
                ReflectionSummary.from(reflectionQuestion)
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

    public record ReflectionSummary(
            Long questionId,
            String questionText,
            String answerText,

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime answeredAt,

            String generationSource
    ) {

        private static ReflectionSummary from(
                AiQuestion question
        ) {
            if (question == null) {
                return null;
            }

            return new ReflectionSummary(
                    question.getId(),
                    question.getQuestionText(),
                    question.getAnswerText(),
                    question.getAnsweredAt(),
                    question.getGenerationSource().name()
            );
        }
    }
}
