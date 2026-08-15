package mutsa.hackathon.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DAYBIT 일간 색 보상의 공통 값 정책.
 * Entity와 AI 생성 결과가 동일한 검증 규칙을 사용하도록
 * 색상/키워드/색 코멘트 정규화와 서비스 예약 색상 차단을 한 곳에서 담당.
 */
public final class DiaryRewardPolicy {

    public static final int MIN_KEYWORD_COUNT = 1;
    public static final int MAX_KEYWORD_COUNT = 3;
    public static final int MAX_KEYWORD_LENGTH = 20;
    public static final int MAX_COMMENT_SUMMARY_LENGTH = 220;
    public static final int MAX_COLOR_COMMENT_LENGTH = 300;

    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private static final Set<String> RESERVED_UI_COLORS =
            Set.of(
                    "#FFFFFF",
                    "#F3F4F7",
                    "#E7E9EE",
                    "#DFE2EA",
                    "#CDD1DA",
                    "#AFB6C4",
                    "#858C9C",
                    "#5F6473",
                    "#4F5563",
                    "#2D3038",
                    "#414450",
                    "#F6F8FA"
            );

    private static final List<String>
            FORBIDDEN_DIRECT_NEGATIVE_PREFIXES =
            List.of(
                    "외로운",
                    "슬픈",
                    "우울한",
                    "불행한"
            );

    /**
     * AI 코멘트가 사용자의 기록을 멀리서 분석하거나
     * 추측하는 말투로 변하는 대표 표현을 최소한으로 차단.
     * 의미 판단 전체를 서버 blacklist로 해결하지 않고,
     * 결정적으로 잡을 수 있는 형태만 방어
     */
    private static final List<String>
            FORBIDDEN_COMMENT_FRAGMENTS =
            List.of(
                    "추천",
                    "적어주셨어요",
                    "기록되어 있어요",
                    "것 같",
                    "듯해",
                    "보여요",
                    "보이네요"
            );

    private DiaryRewardPolicy() {
    }

    public static String normalizeColorHex(
            String colorHex
    ) {
        if (
                colorHex == null
                        || !HEX_COLOR
                        .matcher(colorHex)
                        .matches()
        ) {
            throw new IllegalArgumentException(
                    "색상 코드는 #RRGGBB 형식이어야 합니다."
            );
        }

        String normalized =
                colorHex.toUpperCase(Locale.ROOT);

        if (
                RESERVED_UI_COLORS.contains(
                        normalized
                )
        ) {
            throw new IllegalArgumentException(
                    "DAYBIT UI 예약 색상은 일기 보상으로 사용할 수 없습니다."
            );
        }

        return normalized;
    }

    public static List<String> normalizeKeywords(
            List<String> keywords
    ) {
        if (keywords == null) {
            throw new IllegalArgumentException(
                    "색 보상 키워드는 필수입니다."
            );
        }

        if (
                keywords.isEmpty()
                        || keywords.size()
                        > MAX_KEYWORD_COUNT
        ) {
            throw new IllegalArgumentException(
                    "색 보상 키워드는 1개 이상 3개 이하여야 합니다."
            );
        }

        LinkedHashSet<String> normalizedKeywords =
                new LinkedHashSet<>();

        for (String keyword : keywords) {
            String normalized =
                    normalizeKeyword(
                            keyword
                    );

            normalizedKeywords.add(
                    normalized
            );
        }

        if (normalizedKeywords.isEmpty()) {
            throw new IllegalArgumentException(
                    "색 보상 키워드는 최소 1개가 필요합니다."
            );
        }

        return List.copyOf(
                normalizedKeywords
        );
    }

    /**
     * 최종 사용자 노출 색상 코멘트는 AI가 생성한 시각적 근거 설명을 그대로 사용합니다.
     * 닉네임 기반 고정 문구는 더 이상 서버에서 덧붙이지 않음
     */
    public static String composeColorCommentSummary(
            String commentSummary
    ) {
        return normalizeColorCommentSummary(
                commentSummary
        );
    }

    public static String normalizeColorCommentSummary(
            String commentSummary
    ) {
        if (
                commentSummary == null
                        || commentSummary.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Color comment is required."
            );
        }

        String normalized =
                commentSummary
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (
                normalized.length()
                        > MAX_COMMENT_SUMMARY_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "Color comment must be at most 220 characters."
            );
        }

        if (!normalized.endsWith(".")) {
            throw new IllegalArgumentException(
                    "Color comment must end with a period '.'."
            );
        }

        if (
                FORBIDDEN_COMMENT_FRAGMENTS
                        .stream()
                        .anyMatch(
                                normalized::contains
                        )
        ) {
            throw new IllegalArgumentException(
                    "Color comment contains a forbidden phrase."
            );
        }

        return normalized;
    }

    public static String normalizeColorComment(
            String colorComment
    ) {
        if (
                colorComment == null
                        || colorComment.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "색 보상 코멘트는 필수입니다."
            );
        }

        String normalized =
                colorComment
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (
                normalized.length()
                        > MAX_COLOR_COMMENT_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "색 보상 코멘트는 300자 이하여야 합니다."
            );
        }

        return normalized;
    }

    public static boolean isReservedColor(
            String colorHex
    ) {
        if (
                colorHex == null
                        || !HEX_COLOR
                        .matcher(colorHex)
                        .matches()
        ) {
            return false;
        }

        return RESERVED_UI_COLORS.contains(
                colorHex.toUpperCase(
                        Locale.ROOT
                )
        );
    }

    private static String normalizeKeyword(
            String keyword
    ) {
        if (
                keyword == null
                        || keyword.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "비어 있는 색 보상 키워드는 사용할 수 없습니다."
            );
        }

        String normalized =
                keyword.trim();

        if (normalized.startsWith("#")) {
            normalized =
                    normalized.substring(1);
        }

        normalized =
                normalized
                        .replaceAll(
                                "\\s+",
                                ""
                        )
                        .trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "비어 있는 색 보상 키워드는 사용할 수 없습니다."
            );
        }

        if (
                normalized.length()
                        > MAX_KEYWORD_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "색 보상 키워드는 20자 이하여야 합니다."
            );
        }

        if (
                FORBIDDEN_DIRECT_NEGATIVE_PREFIXES
                        .stream()
                        .anyMatch(
                                normalized::startsWith
                        )
        ) {
            throw new IllegalArgumentException(
                    "사용자의 감정을 직접적으로 부정 단정하는 키워드는 사용할 수 없습니다."
            );
        }

        return normalized;
    }
}
