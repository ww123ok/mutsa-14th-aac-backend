package mutsa.hackathon.service;

import java.util.LinkedHashSet;
import java.util.List;

public record WeeklyRewardInsight(
        String title,
        String summary,
        List<String> keywords,
        String visualMotif
) {
    public WeeklyRewardInsight {
        title = normalizeRequired(title, 100, "주간 제목은 필수입니다.");
        summary = normalizeRequired(summary, 1000, "주간 설명은 필수입니다.");
        visualMotif = normalizeRequired(visualMotif, 1200, "이미지 모티프는 필수입니다.");

        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String value = keyword.trim().replaceFirst("^#", "");
                values.add(value.length() <= 30 ? value : value.substring(0, 30));
                if (values.size() == 3) {
                    break;
                }
            }
        }
        if (values.isEmpty()) {
            values.add("이번주");
        }
        keywords = List.copyOf(values);
    }

    private static String normalizeRequired(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}