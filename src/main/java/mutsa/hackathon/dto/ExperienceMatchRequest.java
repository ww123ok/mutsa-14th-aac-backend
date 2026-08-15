package mutsa.hackathon.dto;

import jakarta.validation.constraints.NotNull;

public record ExperienceMatchRequest(@NotNull Long diaryId) {
}
