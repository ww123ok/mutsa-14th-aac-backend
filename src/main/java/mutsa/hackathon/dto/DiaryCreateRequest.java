package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;

public record DiaryCreateRequest(

        @NotBlank(message = "일기 내용은 필수입니다.")
        String content

) {
}