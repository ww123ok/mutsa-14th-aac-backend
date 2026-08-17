package mutsa.hackathon.dto;

import mutsa.hackathon.domain.ExperienceFragmentArrival;

import java.time.LocalDateTime;
import java.util.List;

/** Metadata used to render a received-but-not-yet-opened experience fragment. */
public record ExperienceFragmentInboxResponse(
        Long arrivalId,
        String generalTopic,
        List<String> keywords,
        LocalDateTime arrivedAt
) {
    public static ExperienceFragmentInboxResponse from(ExperienceFragmentArrival arrival) {
        return new ExperienceFragmentInboxResponse(
                arrival.getId(),
                arrival.getDiaryShare().getGeneralTopic(),
                List.copyOf(arrival.getDiaryShare().getKeywords()),
                arrival.getCreatedAt()
        );
    }
}
