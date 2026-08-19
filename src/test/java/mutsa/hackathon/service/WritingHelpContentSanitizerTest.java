package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WritingHelpContentSanitizerTest {

    @Test
    void 편집기_타임스탬프_단독줄만_제거한다() {
        String content = """
                AM 5:11
                새벽에 쓴 일기가 아니라 친구와 카페에 갔던 이야기다.

                PM 8:53
                창가 자리에 앉아서 디저트를 먹었다.
                """;

        String sanitized =
                WritingHelpContentSanitizer
                        .sanitize(content);

        assertEquals(
                """
                        새벽에 쓴 일기가 아니라 친구와 카페에 갔던 이야기다.

                        창가 자리에 앉아서 디저트를 먹었다.""",
                sanitized
        );
    }

    @Test
    void 한글_타임스탬프와_CRLF도_제거한다() {
        String content =
                "오전 5:11\r\n산책을 했다.\r\n오후 8:53\r\n집에 돌아왔다.";

        String sanitized =
                WritingHelpContentSanitizer
                        .sanitize(content);

        assertEquals(
                "산책을 했다.\n집에 돌아왔다.",
                sanitized
        );
    }

    @Test
    void 사용자가_본문에_직접_쓴_시간표현은_보존한다() {
        String content = """
                오전 11시에 친구를 만났다.
                PM 8:53에 알람이 울렸다.
                밤늦게 작업을 마쳤다.
                """;

        String sanitized =
                WritingHelpContentSanitizer
                        .sanitize(content);

        assertTrue(
                sanitized.contains(
                        "오전 11시에 친구를 만났다."
                )
        );
        assertTrue(
                sanitized.contains(
                        "PM 8:53에 알람이 울렸다."
                )
        );
        assertTrue(
                sanitized.contains(
                        "밤늦게 작업을 마쳤다."
                )
        );
    }

    @Test
    void 타임스탬프만_있으면_빈문자열이_된다() {
        String sanitized =
                WritingHelpContentSanitizer
                        .sanitize(
                                "AM 5:11\n\nPM 8:53"
                        );

        assertTrue(sanitized.isBlank());
    }
}
