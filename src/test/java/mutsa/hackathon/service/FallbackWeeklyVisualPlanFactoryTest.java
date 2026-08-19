package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackWeeklyVisualPlanFactoryTest {

    private final FallbackWeeklyVisualPlanFactory factory =
            new FallbackWeeklyVisualPlanFactory();

    @Test
    void 직전주가_그래픽포스터면_대체기획도_다른카테고리를_사용한다() {
        WeeklyVisualPlan plan = factory.create(
                context(),
                WeeklyVisualCategory.GRAPHIC_POSTER
        );

        assertEquals(
                WeeklyVisualCategory.OIL_ACRYLIC,
                plan.visualCategory()
        );
    }

    @Test
    void 직전주가_다른카테고리면_기본_그래픽포스터를_사용한다() {
        WeeklyVisualPlan plan = factory.create(
                context(),
                WeeklyVisualCategory.PIXEL_ART
        );

        assertEquals(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                plan.visualCategory()
        );
    }

    private WeeklyRewardGenerationContext context() {
        return new WeeklyRewardGenerationContext(
                10L,
                20L,
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
                List.of(
                        day(LocalDate.of(2026, 8, 3), "#D6A45C"),
                        day(LocalDate.of(2026, 8, 5), "#6A8FB3"),
                        day(LocalDate.of(2026, 8, 7), "#C9878A")
                )
        );
    }

    private WeeklyRewardGenerationContext.DayRecord day(
            LocalDate date,
            String color
    ) {
        return new WeeklyRewardGenerationContext.DayRecord(
                date,
                "테스트 일기",
                color,
                List.of("기록")
        );
    }
}
