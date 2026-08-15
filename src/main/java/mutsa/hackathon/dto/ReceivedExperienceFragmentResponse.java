package mutsa.hackathon.dto;

import java.util.List;

public record ReceivedExperienceFragmentResponse(Long deliveryId, Long shareId, String anonymizedContent,
                                                 String generalTopic, List<String> keywords, int remainingCredit) {
}
