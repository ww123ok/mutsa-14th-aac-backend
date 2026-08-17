package mutsa.hackathon.service;

import java.util.regex.Pattern;

/**
 * 일기 편집기가 자동으로 붙인 타임스탬프를
 * 성찰 질문용 AI 입력에서만 제거합니다.
 *
 * <p>원본 일기 내용은 변경하지 않으며,
 * 사용자가 본문 안에 직접 작성한 시간 표현은 보존합니다.</p>
 */
final class DiaryReflectionContentSanitizer {

    private static final Pattern
            SYSTEM_TIMESTAMP_LINE =
            Pattern.compile(
                    "(?im)^[\\t\\p{Zs}]*"
                            + "(?:AM|PM|오전|오후)"
                            + "[\\t\\p{Zs}]+"
                            + "(?:0?[1-9]|1[0-2]):[0-5]\\d"
                            + "[\\t\\p{Zs}]*$"
            );

    private static final Pattern
            BLANK_LINE_WITH_SPACES =
            Pattern.compile(
                    "(?m)^[\\t\\p{Zs}]+$"
            );

    private static final Pattern
            EXCESSIVE_BLANK_LINES =
            Pattern.compile(
                    "\\n{3,}"
            );

    private DiaryReflectionContentSanitizer() {
    }

    static String sanitize(
            String diaryContent
    ) {
        if (diaryContent == null) {
            return "";
        }

        String normalizedLineBreaks =
                diaryContent
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                '\r',
                                '\n'
                        );

        String withoutSystemTimestamps =
                SYSTEM_TIMESTAMP_LINE
                        .matcher(
                                normalizedLineBreaks
                        )
                        .replaceAll("");

        String withoutWhitespaceOnlyLines =
                BLANK_LINE_WITH_SPACES
                        .matcher(
                                withoutSystemTimestamps
                        )
                        .replaceAll("");

        return EXCESSIVE_BLANK_LINES
                .matcher(
                        withoutWhitespaceOnlyLines
                )
                .replaceAll(
                        "\n\n"
                )
                .trim();
    }
}
