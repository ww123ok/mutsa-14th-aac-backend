package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryReminderNotificationSchedulerTest {

    @Mock
    private AppUserRepository
            appUserRepository;

    @Mock
    private DiaryRepository
            diaryRepository;

    @Mock
    private ApplicationEventPublisher
            eventPublisher;

    private DiaryReminderNotificationScheduler
            scheduler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-18T12:30:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        UserDayService userDayService =
                new UserDayService(
                        appUserRepository,
                        clock
                );

        scheduler =
                new DiaryReminderNotificationScheduler(
                        appUserRepository,
                        diaryRepository,
                        userDayService,
                        eventPublisher,
                        clock
                );
    }

    @Test
    void sendsReminderAtConfiguredTimeWhenDiaryIsMissing() {
        AppUser user = user();

        when(
                appUserRepository
                        .findAllByDiaryReminderTime(
                                LocalTime.of(
                                        21,
                                        30
                                )
                        )
        ).thenReturn(List.of(user));

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                LocalDate.of(
                                        2026,
                                        8,
                                        18
                                )
                        )
        ).thenReturn(false);

        scheduler.createDueDiaryReminders();

        verify(eventPublisher)
                .publishEvent(
                        InAppNotificationRequested
                                .diaryReminder(
                                        1L,
                                        LocalDate.of(
                                                2026,
                                                8,
                                                18
                                        )
                                )
                );
    }

    @Test
    void doesNotSendReminderAfterDiaryWasCompleted() {
        AppUser user = user();

        when(
                appUserRepository
                        .findAllByDiaryReminderTime(
                                LocalTime.of(
                                        21,
                                        30
                                )
                        )
        ).thenReturn(List.of(user));

        when(
                diaryRepository
                        .existsByUserIdAndRecordedDate(
                                1L,
                                LocalDate.of(
                                        2026,
                                        8,
                                        18
                                )
                        )
        ).thenReturn(true);

        scheduler.createDueDiaryReminders();

        verify(
                eventPublisher,
                never()
        ).publishEvent(
                org.mockito.ArgumentMatchers.any()
        );
    }

    private AppUser user() {
        AppUser user =
                AppUser.createKakaoUser(
                        "provider-1",
                        "사용자",
                        null,
                        null
                );

        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        user.updatePersonalSettings(
                "사용자",
                "학생",
                LocalTime.of(
                        21,
                        30
                ),
                LocalTime.of(
                        6,
                        0
                ),
                false
        );

        return user;
    }
}
