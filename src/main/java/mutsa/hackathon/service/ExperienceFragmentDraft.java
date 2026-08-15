package mutsa.hackathon.service;

import java.util.List;

public record ExperienceFragmentDraft(
        String anonymizedContent,
        String generalTopic,
        List<String> keywords,
        String matchingText
) {
}
