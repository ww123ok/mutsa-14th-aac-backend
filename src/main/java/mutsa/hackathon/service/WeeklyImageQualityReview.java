package mutsa.hackathon.service;

import java.util.List;

public record WeeklyImageQualityReview(
        boolean reviewed,
        boolean approved,
        List<String> violations,
        String correctionPrompt
) {
    private static final int MAX_VIOLATION_LENGTH = 160;
    private static final int MAX_CORRECTION_LENGTH = 1_200;

    public WeeklyImageQualityReview {
        violations = violations == null
                ? List.of()
                : violations.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> truncate(value.trim(), MAX_VIOLATION_LENGTH))
                .limit(8)
                .toList();

        correctionPrompt = correctionPrompt == null
                ? ""
                : truncate(correctionPrompt.trim(), MAX_CORRECTION_LENGTH);

        if (!reviewed) {
            approved = true;
            violations = List.of();
            correctionPrompt = "";
        }
    }

    public static WeeklyImageQualityReview skipped() {
        return new WeeklyImageQualityReview(
                false,
                true,
                List.of(),
                ""
        );
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
