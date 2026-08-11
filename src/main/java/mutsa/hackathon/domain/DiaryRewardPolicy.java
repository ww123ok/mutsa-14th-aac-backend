package mutsa.hackathon.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DAYBIT 일간 색 보상의 공통 값 정책.
 * Entity와 AI 생성 결과가 동일한 검증 규칙을 사용하도록
 * 색상/키워드 정규화와 서비스 예약 색상 차단을 한 곳에서 담당.
 */
public final class DiaryRewardPolicy {

    public static final int MIN_KEYWORD_COUNT = 1;
    public static final int MAX_KEYWORD_COUNT = 3;
    public static final int MAX_KEYWORD_LENGTH = 20;

    private static final Pattern HEX_COLOR =
            Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /**
     * DAYBIT UI에서 기본적으로 사용하는 색상.
     * 색 보상은 사용자에게 독립적인 보상으로 보여야 하므로
     * 아래 색상과 정확히 동일한 HEX는 생성할 수 없도록 함.
     */
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

    /**
     * 기획에서 직접적으로 피하기로 한
     * 직관적인 부정 감정 라벨.
     * 모든 감정 표현을 금지하는 것은 아님.
     * 사용자를 직접적으로 규정하는 대표 표현만
     * 서버에서 최종 방어.
     */
    private static final List<String>
            FORBIDDEN_DIRECT_NEGATIVE_PREFIXES =
            List.of(
                    "외로운",
                    "슬픈",
                    "우울한",
                    "불행한"
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

        /*
         * AI가 실수로 #차분한 형태로 반환하더라도
         * API에서는 "차분한"으로 통일.
         * 실제 # 표시는 프론트엔드가 담당.
         */
        if (normalized.startsWith("#")) {
            normalized =
                    normalized.substring(1);
        }

        /*
         * 해시태그 형태로 사용할 것이므로
         * "팀 프로젝트" → "팀프로젝트"처럼 정규화
         */
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