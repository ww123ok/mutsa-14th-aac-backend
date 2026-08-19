package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiaryCommentCreateRequest(

        @NotBlank(
                message = "댓글 내용은 필수입니다."
        )
        @Size(
                max = 2000,
                message = "댓글은 2000자 이하로 작성해야 합니다."
        )
        String content
) {
}
