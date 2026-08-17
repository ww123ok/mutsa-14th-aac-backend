package mutsa.hackathon.service;

import mutsa.hackathon.domain.WeeklyRewardImageSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackWeeklyPosterGeneratorTest {

    @Test
    void createsPngPosterWithoutExternalApi() {
        WeeklyRewardGenerationContext context =
                new WeeklyRewardGenerationContext(
                        1L,
                        1L,
                        LocalDate.of(2026, 8, 3),
                        LocalDate.of(2026, 8, 9),
                        List.of(
                                day(
                                        LocalDate.of(2026, 8, 3),
                                        "#D6A45C"
                                ),
                                day(
                                        LocalDate.of(2026, 8, 4),
                                        "#6A8FB3"
                                ),
                                day(
                                        LocalDate.of(2026, 8, 5),
                                        "#C9878A"
                                )
                        )
                );

        WeeklyVisualPlan visualPlan =
                new WeeklyVisualPlan(
                        WeeklyVisualCategory.GRAPHIC_POSTER,
                        "Create one portrait graphic poster with a single asymmetric central mass and "
                                + "three supporting planes. Preserve broad white negative space and use "
                                + "subtle halftone grain to connect the forms. Assign the first weekly color "
                                + "to the dominant mass, the second to the supporting structure, and the "
                                + "remaining colors to small accents. Keep the composition flat, controlled, "
                                + "and intentionally non-photographic. Do not show a person, face, room, "
                                + "building, desk, paper, bus, landscape, readable text, logo, collage, or "
                                + "calendar. Produce one coherent mobile archive image with a clear focal "
                                + "hierarchy and restrained print texture."
                );

        GeneratedWeeklyImage image =
                new FallbackWeeklyPosterGenerator()
                        .generate(context, visualPlan);

        assertEquals(
                "image/png",
                image.contentType()
        );
        assertEquals(
                WeeklyRewardImageSource.FALLBACK,
                image.source()
        );
        assertTrue(image.bytes().length > 1_000);
        assertArrayEquals(
                new byte[]{
                        (byte) 0x89,
                        0x50,
                        0x4E,
                        0x47
                },
                java.util.Arrays.copyOf(
                        image.bytes(),
                        4
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