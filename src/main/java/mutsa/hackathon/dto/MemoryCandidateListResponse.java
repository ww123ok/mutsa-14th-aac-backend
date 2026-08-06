package mutsa.hackathon.dto;

import java.util.List;

public record MemoryCandidateListResponse(
        Long diaryId,
        MemoryReviewStatus reviewStatus,
        boolean reviewRequired,
        List<MemoryCandidateResponse> items
) {
}