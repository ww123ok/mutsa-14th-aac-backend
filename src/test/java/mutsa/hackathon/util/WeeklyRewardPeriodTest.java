package mutsa.hackathon.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyRewardPeriodTest {

    @Test
    void previousCompletedWeekUsesMondayToSunday() {
        WeeklyRewardPeriod period = WeeklyRewardPeriod.previousCompletedWeek(
                LocalDate.of(2026, 8, 10)
        );

        assertEquals(LocalDate.of(2026, 8, 3), period.startDate());
        assertEquals(LocalDate.of(2026, 8, 9), period.endDate());
    }

    @Test
    void startDateMustBeMonday() {
        assertThrows(
                IllegalArgumentException.class,
                () -> WeeklyRewardPeriod.fromStart(LocalDate.of(2026, 8, 4))
        );
    }
}