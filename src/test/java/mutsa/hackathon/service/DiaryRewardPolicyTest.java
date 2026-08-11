package mutsa.hackathon.service;

import mutsa.hackathon.domain.DiaryRewardPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiaryRewardPolicyTest {

    private static final List<String> RESERVED_UI_COLORS =
            List.of(
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

    @Test
    void 정상_색상은_대문자_HEX로_정규화한다() {
        assertEquals(
                "#73D8B4",
                DiaryRewardPolicy.normalizeColorHex(
                        "#73d8b4"
                )
        );
    }

    @Test
    void DAYBIT_UI_예약색상_12종은_대소문자와_관계없이_모두_거부한다() {
        for (String reservedColor : RESERVED_UI_COLORS) {

            assertTrue(
                    DiaryRewardPolicy.isReservedColor(
                            reservedColor
                    )
            );

            assertTrue(
                    DiaryRewardPolicy.isReservedColor(
                            reservedColor.toLowerCase()
                    )
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            DiaryRewardPolicy
                                    .normalizeColorHex(
                                            reservedColor
                                    )
            );
        }
    }

    @Test
    void 예약색이_아닌_정상색은_허용한다() {
        assertFalse(
                DiaryRewardPolicy.isReservedColor(
                        "#D99A7A"
                )
        );

        assertEquals(
                "#D99A7A",
                DiaryRewardPolicy.normalizeColorHex(
                        "#d99a7a"
                )
        );
    }

    @Test
    void 키워드는_해시태그와_공백을_제거하고_입력순서를_유지한다() {
        List<String> normalized =
                DiaryRewardPolicy.normalizeKeywords(
                        List.of(
                                "#새벽비",
                                " 팀 프로젝트 ",
                                "따뜻한"
                        )
                );

        assertEquals(
                List.of(
                        "새벽비",
                        "팀프로젝트",
                        "따뜻한"
                ),
                normalized
        );
    }

    @Test
    void 중복_키워드는_한번만_남긴다() {
        assertEquals(
                List.of(
                        "집중",
                        "차분한"
                ),
                DiaryRewardPolicy.normalizeKeywords(
                        List.of(
                                "집중",
                                "#집중",
                                "차분한"
                        )
                )
        );
    }

    @Test
    void 키워드는_최소_1개_최대_3개여야_한다() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DiaryRewardPolicy
                                .normalizeKeywords(
                                        List.of()
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DiaryRewardPolicy
                                .normalizeKeywords(
                                        List.of(
                                                "하나",
                                                "둘",
                                                "셋",
                                                "넷"
                                        )
                                )
        );
    }

    @Test
    void 비어있거나_너무_긴_키워드는_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DiaryRewardPolicy
                                .normalizeKeywords(
                                        List.of("#")
                                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        DiaryRewardPolicy
                                .normalizeKeywords(
                                        List.of(
                                                "가".repeat(21)
                                        )
                                )
        );
    }

    @Test
    void 사용자의_감정을_직접적으로_부정_단정하는_대표_표현은_거부한다() {
        for (
                String keyword
                : List.of(
                "외로운밤",
                "슬픈하루",
                "우울한마음",
                "불행한순간"
        )
        ) {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            DiaryRewardPolicy
                                    .normalizeKeywords(
                                            List.of(keyword)
                                    )
            );
        }
    }
}