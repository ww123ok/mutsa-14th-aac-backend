package mutsa.hackathon.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WeeklyRewardResultText(
        String title,
        String summary,
        String categoryKeyword,
        List<String> keywords
) {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_SUMMARY_LENGTH = 1_000;
    private static final int MAX_KEYWORD_LENGTH = 30;
    private static final List<String> ALLOWED_CATEGORY_KEYWORDS = List.of(
            "그래픽 포스터",
            "3D캐릭터",
            "유화",
            "LP커버"
    );
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");
    private static final Pattern SENTENCE_END =
            Pattern.compile("[.!?](?=\\s|$)");

    public WeeklyRewardResultText {
        title = normalizeKoreanRequired(
                title,
                MAX_TITLE_LENGTH,
                "주간 제목은 필수입니다."
        );

        summary = normalizeKoreanRequired(
                summary,
                MAX_SUMMARY_LENGTH,
                "주간 설명은 필수입니다."
        );

        categoryKeyword = normalizeCategoryKeyword(categoryKeyword);

        int sentenceCount = countSentences(summary);
        if (sentenceCount < 2 || sentenceCount > 3) {
            throw new IllegalArgumentException(
                    "주간 설명은 짧은 한국어 문장 2~3개여야 합니다."
            );
        }

        keywords = normalizeKeywords(keywords);
    }

    private static String normalizeCategoryKeyword(
            String categoryKeyword
    ) {
        if (categoryKeyword == null || categoryKeyword.isBlank()) {
            throw new IllegalArgumentException(
                    "주간 이미지 카테고리 키워드는 필수입니다."
            );
        }

        String normalized = categoryKeyword.trim()
                .replaceAll("\\s+", " ");

        if (!ALLOWED_CATEGORY_KEYWORDS.contains(normalized)) {
            throw new IllegalArgumentException(
                    "주간 이미지 카테고리 키워드는 그래픽 포스터, 3D캐릭터, 유화, LP커버 중 하나여야 합니다."
            );
        }

        return normalized;
    }

    private static List<String> normalizeKeywords(
            List<String> keywords
    ) {
        if (keywords == null) {
            throw new IllegalArgumentException(
                    "주간 키워드는 필수입니다."
            );
        }

        LinkedHashSet<String> normalized =
                new LinkedHashSet<>();

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }

            String value = keyword.trim()
                    .replaceAll("\\s+", " ");

            if (value.contains("#")) {
                throw new IllegalArgumentException(
                        "주간 키워드에는 #을 사용할 수 없습니다."
                );
            }

            if (
                    value.length() > MAX_KEYWORD_LENGTH
                            || !containsHangul(value)
            ) {
                throw new IllegalArgumentException(
                        "주간 키워드는 30자 이하의 한국어여야 합니다."
                );
            }

            normalized.add(value);

            if (normalized.size() > 5) {
                throw new IllegalArgumentException(
                        "주간 하단 키워드는 3~5개여야 합니다."
                );
            }
        }

        if (normalized.size() < 3) {
            throw new IllegalArgumentException(
                    "주간 하단 키워드는 3개 이상 필요합니다."
            );
        }

        return List.copyOf(normalized);
    }

    private static int countSentences(String value) {
        Matcher matcher = SENTENCE_END.matcher(value);
        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private static String normalizeKoreanRequired(
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
                    "주간 결과 문구가 허용 길이를 초과했습니다."
            );
        }

        if (!containsHangul(normalized)) {
            throw new IllegalArgumentException(
                    "주간 결과 문구는 한국어여야 합니다."
            );
        }

        return normalized;
    }

    private static boolean containsHangul(String value) {
        return HANGUL.matcher(value).find();
    }
}
