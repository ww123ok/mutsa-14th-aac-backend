package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.WeeklyRewardTriggerResponse;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyRewardUserScheduleServiceTest {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    private AppUserRepository appUserRepository;
    private WeeklyRewardBatchService batchService;
    private UserDayService userDayService;
    private WeeklyRewardUserScheduleService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        batchService = mock(WeeklyRewardBatchService.class);
        userDayService = new UserDayService(
                appUserRepository,
                Clock.system(SERVICE_ZONE)
        );

        service = new WeeklyRewardUserScheduleService(
                appUserRepository,
                batchService,
                userDayService,
                Clock.system(SERVICE_ZONE)
        );

        ReflectionTestUtils.setField(
                service,
                "scheduleDelayMinutes",
                5L
        );
    }

    @Test
    void 시작시간이_06시이면_월요일_06시05분에_직전주차를_생성한다() {
        AppUser first = createUser(
                "weekly-boundary-1",
                LocalTime.of(6, 0),
                11L
        );
        AppUser second = createUser(
                "weekly-boundary-2",
                LocalTime.of(6, 0),
                12L
        );

        when(
                appUserRepository.findAllByDayStartTime(
                        LocalTime.of(6, 0)
                )
        ).thenReturn(List.of(first, second));

        when(
                batchService.generateForUser(
                        11L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        101L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        when(
                batchService.generateForUser(
                        12L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.notEligible(
                        LocalDate.of(2026, 8, 10)
                )
        );

        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.generateUsersAtBoundary(
                        LocalDateTime.of(
                                2026, 8, 17,
                                6, 5
                        )
                );

        assertEquals(2, result.candidateCount());
        assertEquals(1, result.eligibleCount());
        assertEquals(0, result.failedCount());

        verify(batchService).generateForUser(
                11L,
                LocalDate.of(2026, 8, 10)
        );
        verify(batchService).generateForUser(
                12L,
                LocalDate.of(2026, 8, 10)
        );
    }

    @Test
    void 자정_사용자와_기존_null_사용자는_월요일_00시05분에_함께_처리한다() {
        AppUser midnightUser = createUser(
                "weekly-midnight",
                LocalTime.MIDNIGHT,
                21L
        );
        AppUser legacyUser = createUser(
                "weekly-legacy",
                LocalTime.MIDNIGHT,
                22L
        );
        ReflectionTestUtils.setField(
                legacyUser,
                "dayStartTime",
                null
        );

        when(
                appUserRepository.findAllByDayStartTime(
                        LocalTime.MIDNIGHT
                )
        ).thenReturn(List.of(midnightUser));

        when(
                appUserRepository.findAllByDayStartTimeIsNull()
        ).thenReturn(List.of(legacyUser));

        when(
                batchService.generateForUser(
                        21L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        201L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        when(
                batchService.generateForUser(
                        22L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        202L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.generateUsersAtBoundary(
                        LocalDateTime.of(
                                2026, 8, 17,
                                0, 5
                        )
                );

        assertEquals(2, result.candidateCount());
        assertEquals(2, result.eligibleCount());
        assertEquals(0, result.failedCount());
    }

    @Test
    void 시작시간이_23시59분이면_화요일_00시04분에도_월요일경계로_생성된다() {
        AppUser lateUser = createUser(
                "weekly-late",
                LocalTime.of(23, 59),
                31L
        );

        when(
                appUserRepository.findAllByDayStartTime(
                        LocalTime.of(23, 59)
                )
        ).thenReturn(List.of(lateUser));

        when(
                batchService.generateForUser(
                        31L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        301L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.generateUsersAtBoundary(
                        LocalDateTime.of(
                                2026, 8, 18,
                                0, 4
                        )
                );

        assertEquals(1, result.candidateCount());
        assertEquals(1, result.eligibleCount());
    }

    @Test
    void 월요일경계가_아닌_시각에는_자동생성을_시도하지_않는다() {
        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.generateUsersAtBoundary(
                        LocalDateTime.of(
                                2026, 8, 16,
                                6, 5
                        )
                );

        assertEquals(0, result.candidateCount());
        verify(
                appUserRepository,
                never()
        ).findAllByDayStartTime(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 복구검사에서는_사용자_월요일시작_5분전에는_새주차를_생성하지_않는다() {
        AppUser user = createUser(
                "weekly-user-1",
                LocalTime.of(6, 0),
                41L
        );

        when(appUserRepository.findAll())
                .thenReturn(List.of(user));

        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.catchUpDueUsers(
                        LocalDateTime.of(
                                2026, 8, 17,
                                6, 4
                        )
                );

        assertEquals(0, result.candidateCount());
        verify(
                batchService,
                never()
        ).generateForUser(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void 복구검사에서는_사용자_월요일시작_5분후_직전주차를_생성한다() {
        AppUser user = createUser(
                "weekly-user-2",
                LocalTime.of(6, 0),
                42L
        );

        when(appUserRepository.findAll())
                .thenReturn(List.of(user));

        when(
                batchService.generateForUser(
                        42L,
                        LocalDate.of(2026, 8, 10)
                )
        ).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        402L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        WeeklyRewardUserScheduleService.ScheduleResult result =
                service.catchUpDueUsers(
                        LocalDateTime.of(
                                2026, 8, 17,
                                6, 5
                        )
                );

        assertEquals(1, result.candidateCount());
        assertEquals(1, result.eligibleCount());
        assertEquals(0, result.failedCount());

        verify(batchService).generateForUser(
                42L,
                LocalDate.of(2026, 8, 10)
        );
    }

    @Test
    void 전주_일기의_색보상이_늦게_완료되면_주간생성시각_이후_즉시_재시도한다() {
        AppUser user = createUser(
                "weekly-deferred",
                LocalTime.of(1, 0),
                51L
        );

        when(appUserRepository.findById(51L))
                .thenReturn(java.util.Optional.of(user));
        when(batchService.generateForUser(
                51L,
                LocalDate.of(2026, 8, 10)
        )).thenReturn(
                WeeklyRewardTriggerResponse.eligible(
                        501L,
                        LocalDate.of(2026, 8, 10)
                )
        );

        Clock fixedClock = Clock.fixed(
                java.time.Instant.parse("2026-08-16T16:10:00Z"),
                SERVICE_ZONE
        );
        UserDayService fixedUserDayService = new UserDayService(
                appUserRepository,
                fixedClock
        );
        WeeklyRewardUserScheduleService fixedService =
                new WeeklyRewardUserScheduleService(
                        appUserRepository,
                        batchService,
                        fixedUserDayService,
                        fixedClock
                );
        ReflectionTestUtils.setField(
                fixedService,
                "scheduleDelayMinutes",
                5L
        );

        java.util.Optional<WeeklyRewardTriggerResponse> response =
                fixedService.generateForCompletedDiaryIfDue(
                        51L,
                        LocalDate.of(2026, 8, 16)
                );

        assertEquals(true, response.isPresent());
        verify(batchService).generateForUser(
                51L,
                LocalDate.of(2026, 8, 10)
        );
    }


    @Test
    void 전주_일기의_색보상이_완료되어도_주간생성시각_전이면_재시도하지_않는다() {
        AppUser user = createUser(
                "weekly-not-due",
                LocalTime.of(1, 0),
                52L
        );

        when(appUserRepository.findById(52L))
                .thenReturn(java.util.Optional.of(user));

        Clock fixedClock = Clock.fixed(
                java.time.Instant.parse("2026-08-16T16:04:00Z"),
                SERVICE_ZONE
        );
        UserDayService fixedUserDayService = new UserDayService(
                appUserRepository,
                fixedClock
        );
        WeeklyRewardUserScheduleService fixedService =
                new WeeklyRewardUserScheduleService(
                        appUserRepository,
                        batchService,
                        fixedUserDayService,
                        fixedClock
                );
        ReflectionTestUtils.setField(
                fixedService,
                "scheduleDelayMinutes",
                5L
        );

        java.util.Optional<WeeklyRewardTriggerResponse> response =
                fixedService.generateForCompletedDiaryIfDue(
                        52L,
                        LocalDate.of(2026, 8, 16)
                );

        assertEquals(true, response.isEmpty());
        verify(batchService, never()).generateForUser(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private AppUser createUser(
            String providerId,
            LocalTime dayStartTime,
            Long id
    ) {
        AppUser user = AppUser.createKakaoUser(
                providerId,
                "데이빗",
                null,
                null
        );
        user.updateDayStartTime(dayStartTime);
        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );
        return user;
    }
}
