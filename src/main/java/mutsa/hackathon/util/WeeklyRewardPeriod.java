package mutsa.hackathon.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record WeeklyRewardPeriod(
        LocalDate startDate,
        LocalDate endDate
) {
    public WeeklyRewardPeriod {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("주간 시작일과 종료일은 필수입니다.");
        }
        if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("주간 시작일은 월요일이어야 합니다.");
        }
        if (!endDate.equals(startDate.plusDays(6))) {
            throw new IllegalArgumentException("주간 종료일은 일요일이어야 합니다.");
        }
    }

    public static WeeklyRewardPeriod fromStart(LocalDate monday) {
        if (monday == null) {
            throw new IllegalArgumentException("주간 시작일은 필수입니다.");
        }
        return new WeeklyRewardPeriod(monday, monday.plusDays(6));
    }

    public static WeeklyRewardPeriod previousCompletedWeek(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("기준 날짜는 필수입니다.");
        }
        LocalDate currentWeekStart = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        return fromStart(currentWeekStart.minusWeeks(1));
    }

    public static LocalDate firstCalendarWeekStart(int year, int month) {
        return LocalDate.of(year, month, 1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate lastCalendarWeekStart(int year, int month) {
        return LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}