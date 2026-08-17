package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiaryReflectionContentSanitizerTest {

    @Test
    void 영문_AM_PM_타임스탬프_줄만_제거한다() {
        String diaryContent = """
                AM 2:12
                첫 번째 기록

                PM 10:03
                오늘 점심으로 파스타를 먹었다. 맛있었다.
                """;

        String sanitized =
                DiaryReflectionContentSanitizer
                        .sanitize(
                                diaryContent
                        );

        assertEquals(
                """
                첫 번째 기록

                오늘 점심으로 파스타를 먹었다. 맛있었다.
                """.trim(),
                sanitized
        );
    }

    @Test
    void 한글_오전_오후_타임스탬프도_제거한다() {
        String diaryContent = """
                오전 2:12
                새벽에 잠깐 기록했다.

                오후 10:03
                하루를 마무리하며 다시 적었다.
                """;

        String sanitized =
                DiaryReflectionContentSanitizer
                        .sanitize(
                                diaryContent
                        );

        assertEquals(
                """
                새벽에 잠깐 기록했다.

                하루를 마무리하며 다시 적었다.
                """.trim(),
                sanitized
        );
    }

    @Test
    void 사용자가_본문에_직접_쓴_시간표현은_보존한다() {
        String diaryContent = """
                PM 10:03
                오늘 점심으로 파스타를 먹었다.
                오전 11시에 친구를 만났고 오후 3시쯤 집에 왔다.
                """;

        String sanitized =
                DiaryReflectionContentSanitizer
                        .sanitize(
                                diaryContent
                        );

        assertEquals(
                """
                오늘 점심으로 파스타를 먹었다.
                오전 11시에 친구를 만났고 오후 3시쯤 집에 왔다.
                """.trim(),
                sanitized
        );
    }

    @Test
    void 공백과_CRLF가_섞인_타임스탬프도_제거한다() {
        String diaryContent =
                "  AM\u00A02:12  \r\n"
                        + "첫 기록\r\n\r\n"
                        + "\tPM 10:03\t\r\n"
                        + "두 번째 기록";

        String sanitized =
                DiaryReflectionContentSanitizer
                        .sanitize(
                                diaryContent
                        );

        assertEquals(
                "첫 기록\n\n두 번째 기록",
                sanitized
        );
    }

    @Test
    void 타임스탬프만_있으면_빈_문자열이_된다() {
        String diaryContent = """
                AM 2:12
                PM 10:03
                """;

        assertEquals(
                "",
                DiaryReflectionContentSanitizer
                        .sanitize(
                                diaryContent
                        )
        );
    }
}
