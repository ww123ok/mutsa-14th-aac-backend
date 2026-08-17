package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyRewardResultTextTest {

    @Test
    void 한국어_요약_두문장과_해시태그없는_키워드를_허용한다() {
        WeeklyRewardResultText result = new WeeklyRewardResultText(
                "작업과 산책이 이어진 한 주",
                "이번 주에는 작업을 정리하는 날이 있었습니다. "
                        + "저녁에는 동네를 걷거나 집에서 쉬었습니다.",
                List.of("작업 정리", "저녁 산책")
        );

        assertEquals(2, result.keywords().size());
        assertEquals("저녁 산책", result.keywords().get(1));
    }

    @Test
    void 요약이_한문장이면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 세 개의 기록을 작성했습니다.",
                        List.of("주간 기록")
                )
        );
    }

    @Test
    void 키워드에_해시태그가_있으면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 세 개의 기록을 작성했습니다. "
                                + "각 기록은 서로 다른 일상을 담았습니다.",
                        List.of("#주간 기록")
                )
        );
    }
}
