package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackWeeklyRewardResultTextFactoryTest {

    @Test
    void 대체문구도_반영된_일기내용과_이미지형식을_두문장으로_설명한다() {
        WeeklyRewardResultText result =
                new FallbackWeeklyRewardResultTextFactory()
                        .create(
                                context(),
                                visualPlan()
                        );

        assertTrue(result.summary().contains("작업과 산책"));
        assertTrue(result.summary().contains("그래픽 디자인 포스터"));
        assertTrue(result.summary().contains("주간 이미지가 구성되었습니다"));
        assertEquals(List.of("작업", "산책", "휴식"), result.keywords());
        assertFalse(result.keywords().stream().anyMatch(value -> value.contains("#")));
    }

    private WeeklyRewardGenerationContext context() {
        return new WeeklyRewardGenerationContext(
                10L,
                20L,
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
                List.of(
                        day(LocalDate.of(2026, 8, 3), "#D6A45C", "작업"),
                        day(LocalDate.of(2026, 8, 5), "#6A8FB3", "산책"),
                        day(LocalDate.of(2026, 8, 7), "#C9878A", "휴식")
                )
        );
    }

    private WeeklyRewardGenerationContext.DayRecord day(
            LocalDate date,
            String color,
            String keyword
    ) {
        return new WeeklyRewardGenerationContext.DayRecord(
                date,
                "테스트 일기",
                color,
                List.of(keyword)
        );
    }

    private WeeklyVisualPlan visualPlan() {
        return new WeeklyVisualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                String.join(
                        " ",
                        java.util.Collections.nCopies(80, "visual")
                )
        );
    }
}
