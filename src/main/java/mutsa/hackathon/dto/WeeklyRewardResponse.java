package mutsa.hackathon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WeeklyRewardResponse(
        Long weeklyRewardId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        String status,
        boolean available,
        int diaryCount,
        String imageSource,
        String imageUrl,
        Instant imageUrlExpiresAt,
        String title,
        String summary,
        String categoryKeyword,
        List<String> keywords,
        List<DailyColor> dailyColors,

        boolean viewed,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime generatedAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime viewedAt
) {
    public static WeeklyRewardResponse from(
            WeeklyReward reward,
            List<WeeklyRewardEntry> entries,
            String imageUrl,
            Instant imageUrlExpiresAt
    ) {
        boolean completed = reward.getGenerationStatus()
                == mutsa.hackathon.domain.WeeklyRewardStatus.COMPLETED;
        boolean available = completed && imageUrl != null && !imageUrl.isBlank();

        return new WeeklyRewardResponse(
                reward.getId(),
                reward.getWeekStartDate(),
                reward.getWeekEndDate(),
                reward.getGenerationStatus().name(),
                available,
                entries.size(),
                reward.getImageSource() == null
                        ? null
                        : reward.getImageSource().name(),
                completed ? imageUrl : null,
                completed ? imageUrlExpiresAt : null,
                reward.getTitle(),
                reward.getSummary(),
                reward.getCategoryKeyword(),
                reward.getKeywords(),
                entries.stream().map(DailyColor::from).toList(),
                reward.isViewed(),
                reward.getGeneratedAt(),
                reward.getViewedAt()
        );
    }

    public record DailyColor(
            LocalDate recordedDate,
            String colorHex,
            String colorSource,
            List<String> keywords
    ) {
        private static DailyColor from(WeeklyRewardEntry entry) {
            return new DailyColor(
                    entry.getRecordedDate(),
                    entry.getColorHex(),
                    entry.getColorSource().name(),
                    entry.getKeywords()
            );
        }
    }
}
