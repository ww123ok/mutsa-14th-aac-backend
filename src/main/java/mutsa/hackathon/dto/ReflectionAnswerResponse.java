package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.AiQuestion;

import java.time.LocalDateTime;

public record ReflectionAnswerResponse(
        Long diaryId,
        Long questionId,
        String questionText,
        String answerText,

        @JsonFormat(
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        LocalDateTime answeredAt
) {

    public static ReflectionAnswerResponse from(
            AiQuestion question
    ) {
        if (
                question == null
                        || question.getDiary() == null
        ) {
            throw new IllegalArgumentException(
                    "성찰 질문 정보가 올바르지 않습니다."
            );
        }

        return new ReflectionAnswerResponse(
                question.getDiary().getId(),
                question.getId(),
                question.getQuestionText(),
                question.getAnswerText(),
                question.getAnsweredAt()
        );
    }
}
