package mutsa.hackathon.dto;

import java.util.List;

public record ExperienceMatchResponse(Long shareId, String generalTopic, List<String> keywords, double similarity) {
}
