package mutsa.hackathon.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WeeklyRewardDailyCompletionListenerTest {

    @Test
    void 색보상_완료이벤트를_받으면_해당_사용자의_주간보상을_재검사한다() {
        WeeklyRewardUserScheduleService scheduleService =
                mock(WeeklyRewardUserScheduleService.class);
        WeeklyRewardDailyCompletionListener listener =
                new WeeklyRewardDailyCompletionListener(scheduleService);

        listener.handle(
                new DiaryRewardCompletedEvent(
                        1L,
                        10L,
                        20L,
                        LocalDate.of(2026, 8, 16)
                )
        );

        verify(scheduleService).generateForCompletedDiaryIfDue(
                1L,
                LocalDate.of(2026, 8, 16)
        );
    }
}
