package mutsa.hackathon.service;

import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import mutsa.hackathon.repository.DiaryRewardRepository;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyRewardPreparationDraftGuardTest {

    @Test
    void 전주에_미완성_draft가_남아있으면_주간보상_준비를_보류한다() {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        DiaryRepository diaryRepository = mock(DiaryRepository.class);
        DiaryDraftService diaryDraftService = mock(DiaryDraftService.class);
        DiaryRewardRepository diaryRewardRepository = mock(DiaryRewardRepository.class);
        WeeklyRewardRepository weeklyRewardRepository = mock(WeeklyRewardRepository.class);
        WeeklyRewardEntryRepository entryRepository = mock(WeeklyRewardEntryRepository.class);
        WeeklyFallbackDailyColorGenerator fallback = mock(WeeklyFallbackDailyColorGenerator.class);

        WeeklyRewardPreparationService service = new WeeklyRewardPreparationService(
                appUserRepository,
                diaryRepository,
                diaryDraftService,
                diaryRewardRepository,
                weeklyRewardRepository,
                entryRepository,
                fallback,
                Clock.system(ZoneId.of("Asia/Seoul"))
        );
        ReflectionTestUtils.setField(service, "minimumDiaryCount", 3);
        ReflectionTestUtils.setField(service, "dailyRewardWaitMinutes", 10L);

        LocalDate weekStart = LocalDate.of(2026, 8, 10);
        when(weeklyRewardRepository.findByUserIdAndWeekStartDate(1L, weekStart))
                .thenReturn(Optional.empty());
        when(diaryDraftService.hasUnfinishedDraftInPeriod(
                1L,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 16)
        )).thenReturn(true);

        Optional<Long> result = service.prepare(1L, weekStart);

        assertTrue(result.isEmpty());
        verify(diaryRepository, never())
                .findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
                        1L,
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 16)
                );
    }
}
