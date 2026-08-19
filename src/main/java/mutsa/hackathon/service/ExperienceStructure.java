package mutsa.hackathon.service;

import java.util.List;

/** Internal representation used only to compare the structure of experiences. */
public record ExperienceStructure(
        String matchingText,
        List<String> keywords
) {
    public ExperienceStructure {
        if (matchingText == null || matchingText.isBlank()) {
            throw new IllegalArgumentException("Experience matching text is required.");
        }
        matchingText = matchingText.trim();
        keywords = keywords == null ? List.of() : keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
    }
}
