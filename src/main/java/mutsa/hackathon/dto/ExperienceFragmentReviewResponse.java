package mutsa.hackathon.dto;

import mutsa.hackathon.domain.DiaryShare;

import java.time.LocalDateTime;
import java.util.List;

/** Owner-only detail used by the client to switch between the original and anonymized record. */
public record ExperienceFragmentReviewResponse(
        Long shareId,
        String status,
        String originalContent,
        String anonymizedContent,
        String generalTopic,
        List<String> keywords,
        LocalDateTime reviewAvailableAt
) {
    public static ExperienceFragmentReviewResponse from(DiaryShare share) {
        return new ExperienceFragmentReviewResponse(
                share.getId(),
                share.getShareStatus().name(),
                share.getDiary().getContent(),
                share.getAnonymizedContent(),
                share.getGeneralTopic(),
                List.copyOf(share.getKeywords()),
                share.getReviewAvailableAt()
        );
    }
}
