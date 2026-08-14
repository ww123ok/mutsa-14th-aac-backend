package mutsa.hackathon.dto;

import java.util.List;

public record WeeklyRewardArchiveResponse(
        int year,
        int month,
        List<WeeklyRewardResponse> items
) {
    public WeeklyRewardArchiveResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}