package mutsa.hackathon.service;

import java.time.LocalDate;
import java.util.List;

public record WeeklyRewardGenerationContext(
        Long weeklyRewardId,
        Long userId,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        List<DayRecord> days
) {
    public WeeklyRewardGenerationContext {
        if (
                weeklyRewardId == null
                        || userId == null
                        || weekStartDate == null
                        || weekEndDate == null
        ) {
            throw new IllegalArgumentException("주간 보상 생성 정보가 누락되었습니다.");
        }
        days = days == null ? List.of() : List.copyOf(days);
        if (days.size() < 3) {
            throw new IllegalArgumentException("주간 보상에는 최소 3개의 기록이 필요합니다.");
        }
    }

    public record DayRecord(
            LocalDate recordedDate,
            String diaryContent,
            String colorHex,
            List<String> keywords
    ) {
        public DayRecord {
            if (
                    recordedDate == null
                            || diaryContent == null
                            || diaryContent.isBlank()
                            || colorHex == null
                            || colorHex.isBlank()
            ) {
                throw new IllegalArgumentException("주간 보상 일기 정보가 올바르지 않습니다.");
            }
            diaryContent = diaryContent.trim();
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
        }
    }
}