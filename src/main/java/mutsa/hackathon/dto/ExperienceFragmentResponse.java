package mutsa.hackathon.dto;

import mutsa.hackathon.domain.DiaryShare;
import java.time.LocalDateTime;
import java.util.List;

public record ExperienceFragmentResponse(
        Long shareId, Long diaryId, String status, String anonymizedContent,
        String generalTopic, List<String> keywords, String rejectionReason,
        LocalDateTime createdAt, LocalDateTime reviewAvailableAt, LocalDateTime approvedAt
) {
    public static ExperienceFragmentResponse from(DiaryShare share) {
        return new ExperienceFragmentResponse(share.getId(), share.getDiary().getId(),
                share.getShareStatus().name(), share.getAnonymizedContent(), share.getGeneralTopic(),
                List.copyOf(share.getKeywords()), share.getRejectionReason(), share.getCreatedAt(),
                share.getReviewAvailableAt(), share.getApprovedAt());
    }
}
