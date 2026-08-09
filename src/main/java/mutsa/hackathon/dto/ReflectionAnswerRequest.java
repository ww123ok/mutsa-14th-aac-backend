package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;

public record ReflectionAnswerRequest(

        @NotBlank(
                message = "성찰 질문 답변은 공백일 수 없습니다."
        )
        String answerText

) {
}