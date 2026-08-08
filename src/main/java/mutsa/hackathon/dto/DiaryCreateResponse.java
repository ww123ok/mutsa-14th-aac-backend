package mutsa.hackathon.dto;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.Diary;
import mutsa.hackathon.domain.DiaryReward;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryCreateResponse(
        Long diaryId,
        LocalDate recordedDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        RewardResponse reward,
        ReflectionQuestionResponse reflectionQuestion
) {

    public static DiaryCreateResponse from(
            Diary diary,
            DiaryReward reward,
            AiQuestion reflectionQuestion
    ) {
        return new DiaryCreateResponse(
                diary.getId(),
                diary.getRecordedDate(),
                diary.getCreatedAt(),
                RewardResponse.from(reward),
                ReflectionQuestionResponse.from(reflectionQuestion)
        );
    }

    public record RewardResponse(
            String status,
            String colorHex,
            String colorName
    ) {
        private static RewardResponse from(DiaryReward reward) {
            return new RewardResponse(
                    reward.getGenerationStatus().name(),
                    reward.getColorHex(),
                    reward.getColorName()
            );
        }
    }

    public record ReflectionQuestionResponse(
            Long questionId,
            String questionText,
            boolean answerRequired,
            String generationSource
    ) {
        private static ReflectionQuestionResponse from(AiQuestion question) {
            return new ReflectionQuestionResponse(
                    question.getId(),
                    question.getQuestionText(),
                    false,
                    question.getGenerationSource().name()
            );
        }
    }
}
