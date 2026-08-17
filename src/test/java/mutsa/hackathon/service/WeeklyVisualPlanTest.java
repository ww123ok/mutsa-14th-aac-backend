package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyVisualPlanTest {

    @Test
    void 영어_시각디렉션은_80에서_220단어를_허용한다() {
        WeeklyVisualPlan plan = new WeeklyVisualPlan(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                words(80)
        );

        assertEquals(
                WeeklyVisualCategory.GRAPHIC_POSTER,
                plan.visualCategory()
        );
    }

    @Test
    void 시각디렉션이_80단어보다_짧으면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyVisualPlan(
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        words(79)
                )
        );
    }

    @Test
    void 시각디렉션이_220단어보다_길면_거부한다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyVisualPlan(
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        words(221)
                )
        );
    }

    @Test
    void 시각디렉션에_한글이_포함되면_거부한다() {
        String motif = words(79) + " 장면";

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyVisualPlan(
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        motif
                )
        );
    }

    private String words(int count) {
        return String.join(
                " ",
                java.util.Collections.nCopies(
                        count,
                        "visual"
                )
        );
    }
}
