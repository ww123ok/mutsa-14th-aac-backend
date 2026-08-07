package mutsa.hackathon.dto;

import java.util.List;

public record MemoryCandidateReviewResponse(
        Long diaryId,
        MemoryReviewStatus reviewStatus,

        /**
         * 이번 요청에서 실제로 상태가 변경된 후보 수.
         * 같은 결정을 반복 요청한 경우에는 0임.
         */
        int reviewedCount,

        List<MemoryCandidateResponse> items
) {
}