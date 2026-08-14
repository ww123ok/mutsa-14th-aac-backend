package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryDetailResponse(
        Long diaryId,
        LocalDate recordedDate,
        String content,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime createdAt,

        RewardSummary reward,
        ReflectionSummary reflection
) {

    public static DiaryDetailResponse from(
            Diary diary,
            DiaryReward reward,
            AiQuestion reflectionQuestion
    ) {
        if (diary == null) {
            throw new IllegalArgumentException(
                    "일기 정보는 필수입니다."
            );
        }

        return new DiaryDetailResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getContent(),
                diary.getCreatedAt(),
                RewardSummary.from(reward),
                ReflectionSummary.from(
                        reflectionQuestion
                )
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
                    reward.getGenerationStatus()
                            .name(),
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

            @JsonFormat(
                    pattern = "yyyy-MM-dd'T'HH:mm:ss"
            )
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
                    question.getGenerationSource()
                            .name()
            );
        }
    }
}