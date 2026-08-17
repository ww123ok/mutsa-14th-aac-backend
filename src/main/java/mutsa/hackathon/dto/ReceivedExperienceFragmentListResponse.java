package mutsa.hackathon.dto;

import mutsa.hackathon.domain.SharedDiaryLog;

import java.time.LocalDateTime;
import java.util.List;

/** A persisted experience fragment that the authenticated user has already received. */
public record ReceivedExperienceFragmentListResponse(
        Long deliveryId,
        Long shareId,
        String anonymizedContent,
        String generalTopic,
        List<String> keywords,
        LocalDateTime receivedAt,
        boolean feedbackSubmitted,
        LocalDateTime feedbackSubmittedAt
) {

    public static ReceivedExperienceFragmentListResponse from(SharedDiaryLog delivery) {
        return new ReceivedExperienceFragmentListResponse(
                delivery.getId(),
                delivery.getDiaryShare().getId(),
                delivery.getDiaryShare().getAnonymizedContent(),
                delivery.getDiaryShare().getGeneralTopic(),
                List.copyOf(delivery.getDiaryShare().getKeywords()),
                delivery.getCreatedAt(),
                delivery.hasFeedbackSubmitted(),
                delivery.getFeedbackSubmittedAt()
        );
    }
}
