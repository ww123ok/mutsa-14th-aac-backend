package mutsa.hackathon.service;

import java.util.Arrays;
import java.util.regex.Pattern;

public record WeeklyVisualPlan(
        WeeklyVisualCategory visualCategory,
        String visualMotif
) {

    private static final int MIN_MOTIF_WORDS = 80;
    private static final int MAX_MOTIF_WORDS = 220;
    private static final int MAX_MOTIF_LENGTH = 2_200;
    private static final Pattern LATIN_LETTER =
            Pattern.compile("[A-Za-z]");
    private static final Pattern HANGUL =
            Pattern.compile("[가-힣]");

    public WeeklyVisualPlan {
        if (visualCategory == null) {
            throw new IllegalArgumentException(
                    "주간 이미지 카테고리는 필수입니다."
            );
        }

        visualMotif = normalizeRequired(
                visualMotif,
                MAX_MOTIF_LENGTH,
                "이미지 모티프는 필수입니다."
        );

        int wordCount = countWords(visualMotif);
        if (
                wordCount < MIN_MOTIF_WORDS
                        || wordCount > MAX_MOTIF_WORDS
        ) {
            throw new IllegalArgumentException(
                    "이미지 모티프는 영어 80~220단어여야 합니다."
            );
        }

        if (
                !LATIN_LETTER.matcher(visualMotif).find()
                        || HANGUL.matcher(visualMotif).find()
        ) {
            throw new IllegalArgumentException(
                    "이미지 모티프는 영어로 작성되어야 합니다."
            );
        }
    }

    private static int countWords(String value) {
        return (int) Arrays.stream(value.split("\\s+"))
                .filter(word -> !word.isBlank())
                .count();
    }

    private static String normalizeRequired(
            String value,
            int maxLength,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        String normalized = value.trim()
                .replaceAll("\\s+", " ");

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    "이미지 모티프가 허용 길이를 초과했습니다."
            );
        }

        return normalized;
    }
}
