package mutsa.hackathon.service;

import java.util.regex.Pattern;

/**
 * 일기 편집기가 자동으로 붙인 작성 시각 메타데이터를
 * 작성 도움 질문용 AI 입력에서만 제거.
 *
 * <p>원본 요청/일기 DB 데이터는 변경하지 않으며,
 * 사용자가 본문 안에 직접 작성한 시간 표현은 보존.</p>
 */
final class WritingHelpContentSanitizer {

    private static final Pattern
            EDITOR_TIMESTAMP_LINE =
            Pattern.compile(
                    "(?im)^[\\t\\p{Zs}]*"
                            + "(?:AM|PM|오전|오후)"
                            + "[\\t\\p{Zs}]+"
                            + "(?:0?[1-9]|1[0-2]):[0-5]\\d"
                            + "[\\t\\p{Zs}]*"
                            + "(?:\\n|\\z)"
            );

    private static final Pattern
            WHITESPACE_ONLY_LINE =
            Pattern.compile(
                    "(?m)^[\\t\\p{Zs}]+$"
            );

    private static final Pattern
            EXCESSIVE_BLANK_LINES =
            Pattern.compile(
                    "\\n{3,}"
            );

    private WritingHelpContentSanitizer() {
    }

    static String sanitize(
            String content
    ) {
        if (content == null) {
            return "";
        }

        String normalizedLineBreaks =
                content
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                '\r',
                                '\n'
                        );

        String withoutEditorTimestamps =
                EDITOR_TIMESTAMP_LINE
                        .matcher(
                                normalizedLineBreaks
                        )
                        .replaceAll("");

        String withoutWhitespaceOnlyLines =
                WHITESPACE_ONLY_LINE
                        .matcher(
                                withoutEditorTimestamps
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
