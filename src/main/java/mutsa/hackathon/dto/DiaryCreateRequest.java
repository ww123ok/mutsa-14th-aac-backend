package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DiaryCreateRequest(
        @NotBlank(message = "일기 내용은 필수입니다.")
        String content,

        @NotNull(message = "일기 작성일은 필수입니다.")
        LocalDate recordedDate
) {
}