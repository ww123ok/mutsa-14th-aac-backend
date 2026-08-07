package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotNull;

public record MemoryCandidateReviewRequest(

        @NotNull(
                message = "기억 후보 승인 여부는 필수입니다."
        )
        Boolean approved

) {
}