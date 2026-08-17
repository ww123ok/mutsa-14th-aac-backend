package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyImageQualityReviewTest {

    @Test
    void 검수불가_결과는_생성흐름을_막지_않는다() {
        WeeklyImageQualityReview review =
                WeeklyImageQualityReview.skipped();

        assertFalse(review.reviewed());
        assertTrue(review.approved());
        assertTrue(review.violations().isEmpty());
        assertTrue(review.correctionPrompt().isEmpty());
    }

    @Test
    void 위반사항과_교정문은_허용길이로_정규화한다() {
        WeeklyImageQualityReview review =
                new WeeklyImageQualityReview(
                        true,
                        false,
                        List.of(
                                "  recognizable photo collage  ",
                                "",
                                "visible Korean headline"
                        ),
                        "  rebuild as flat graphic planes  "
                );

        assertTrue(review.reviewed());
        assertFalse(review.approved());
        assertEquals(
                List.of(
                        "recognizable photo collage",
                        "visible Korean headline"
                ),
                review.violations()
        );
        assertEquals(
                "rebuild as flat graphic planes",
                review.correctionPrompt()
        );
    }
}
