package mutsa.hackathon.dto;

import mutsa.hackathon.domain.SharedDiaryLog;

import java.time.LocalDateTime;

/** The receiver is intentionally not exposed to keep feedback anonymous. */
public record ExperienceFragmentFeedbackResponse(
        Long deliveryId,
        String content,
        LocalDateTime submittedAt
) {
    public static ExperienceFragmentFeedbackResponse from(SharedDiaryLog delivery) {
        return new ExperienceFragmentFeedbackResponse(
                delivery.getId(),
                delivery.getFeedbackSummary(),
                delivery.getFeedbackSubmittedAt()
        );
    }
}
