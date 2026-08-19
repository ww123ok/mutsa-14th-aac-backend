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
                "그래픽 포스터",
                List.of("조용한", "행복", "운동", "작업 정리", "저녁 산책")
        );

        assertEquals("그래픽 포스터", result.categoryKeyword());
        assertEquals(5, result.keywords().size());
        assertEquals("저녁 산책", result.keywords().get(4));
    }

    @Test
    void 요약이_한문장이면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 세 개의 기록을 작성했습니다.",
                        "유화",
                        List.of("주간 기록", "일상", "기록")
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
                        "LP커버",
                        List.of("#주간 기록", "일상", "기록")
                )
        );
    }

    @Test
    void 하단_키워드가_두개면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 여러 기록이 담겼습니다. "
                                + "이미지에는 그 기록의 흐름이 반영되었습니다.",
                        "3D캐릭터",
                        List.of("조용한", "운동")
                )
        );
    }

    @Test
    void 하단_키워드가_여섯개면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 여러 기록이 담겼습니다. "
                                + "이미지에는 그 기록의 흐름이 반영되었습니다.",
                        "유화",
                        List.of("조용한", "행복", "운동", "축구", "야근", "휴식")
                )
        );
    }

    @Test
    void 픽셀아트와_실사풍경_상단카테고리를_허용한다() {
        WeeklyRewardResultText pixelArt = new WeeklyRewardResultText(
                "공간을 사용한 한 주",
                "이번 주에는 익숙한 공간에서 여러 활동을 했습니다. "
                        + "이미지에는 공간에서 이어진 활동의 흔적이 반영되었습니다.",
                "픽셀아트",
                List.of("공간", "활동", "일상")
        );
        WeeklyRewardResultText photoLandscape = new WeeklyRewardResultText(
                "풍경을 바라본 한 주",
                "이번 주에는 여러 장소의 풍경을 바라본 기록이 있었습니다. "
                        + "이미지에는 빛과 거리의 모습이 주요 장면으로 반영되었습니다.",
                "실사 풍경",
                List.of("풍경", "빛", "거리")
        );

        assertEquals("픽셀아트", pixelArt.categoryKeyword());
        assertEquals("실사 풍경", photoLandscape.categoryKeyword());
    }

    @Test
    void 허용되지_않은_상단_카테고리는_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyRewardResultText(
                        "한 주의 기록",
                        "이번 주에는 여러 기록이 담겼습니다. "
                                + "이미지에는 그 기록의 흐름이 반영되었습니다.",
                        "1인칭 장면묘사",
                        List.of("조용한", "운동", "휴식")
                )
        );
    }
}
