package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceFragmentFeedbackRequest(
        @NotBlank(message = "반응 내용을 입력해 주세요.")
        @Size(max = 1000, message = "반응은 1,000자 이하로 입력해 주세요.")
        String content
) {
}
