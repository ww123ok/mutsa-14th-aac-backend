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
        WeeklyRewardGenerationContext context = new WeeklyRewardGenerationContext(
                1L,
                1L,
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9),
                List.of(
                        day(LocalDate.of(2026, 8, 3), "#D6A45C"),
                        day(LocalDate.of(2026, 8, 4), "#6A8FB3"),
                        day(LocalDate.of(2026, 8, 5), "#C9878A")
                )
        );
        WeeklyRewardInsight insight = new WeeklyRewardInsight(
                "한 주의 기록",
                "세 개의 기록이 이어졌습니다.",
                List.of("기록", "흐름"),
                "색 조각이 이어진 장면"
        );

        GeneratedWeeklyImage image = new FallbackWeeklyPosterGenerator()
                .generate(context, insight);

        assertEquals("image/png", image.contentType());
        assertEquals(WeeklyRewardImageSource.FALLBACK, image.source());
        assertTrue(image.bytes().length > 1_000);
        assertArrayEquals(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
                java.util.Arrays.copyOf(image.bytes(), 4)
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