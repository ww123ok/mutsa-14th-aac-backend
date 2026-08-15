package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDayServiceTest {

    private static final ZoneId SERVICE_ZONE =
            ZoneId.of("Asia/Seoul");

    @Test
    void 시작시간이_06시이면_05시59분은_전날이다() {
        AppUserRepository repository =
                mock(AppUserRepository.class);

        AppUser user = createUser();
        user.updateDayStartTime(
                LocalTime.of(6, 0)
        );

        when(
                repository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-13T20:59:00Z"
                ),
                SERVICE_ZONE
        );

        UserDayService service =
                new UserDayService(
                        repository,
                        clock
                );

        assertEquals(
                LocalDate.of(
                        2026,
                        8,
                        13
                ),
                service.currentDay(1L)
        );
    }

    @Test
    void 시작시간이_06시이면_06시00분부터_당일이다() {
        AppUserRepository repository =
                mock(AppUserRepository.class);

        AppUser user = createUser();
        user.updateDayStartTime(
                LocalTime.of(6, 0)
        );

        when(
                repository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-13T21:00:00Z"
                ),
                SERVICE_ZONE
        );

        UserDayService service =
                new UserDayService(
                        repository,
                        clock
                );

        assertEquals(
                LocalDate.of(
                        2026,
                        8,
                        14
                ),
                service.currentDay(1L)
        );
    }

    @Test
    void 기본_시작시간은_00시라서_기존_날짜기준과_호환된다() {
        AppUserRepository repository =
                mock(AppUserRepository.class);

        AppUser user = createUser();

        when(
                repository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-13T15:00:00Z"
                ),
                SERVICE_ZONE
        );

        UserDayService service =
                new UserDayService(
                        repository,
                        clock
                );

        assertEquals(
                LocalTime.MIDNIGHT,
                user.getDayStartTime()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        8,
                        14
                ),
                service.currentDay(1L)
        );
    }

    @Test
    void 기존_사용자의_null_시작시간은_00시로_해석한다()
            throws Exception {
        AppUser user = createUser();

        java.lang.reflect.Field field =
                AppUser.class
                        .getDeclaredField(
                                "dayStartTime"
                        );

        field.setAccessible(true);
        field.set(
                user,
                null
        );

        assertEquals(
                LocalTime.MIDNIGHT,
                user.getDayStartTime()
        );
    }

    @Test
    void 존재하지_않는_사용자는_오늘을_계산할_수_없다() {
        AppUserRepository repository =
                mock(AppUserRepository.class);

        when(
                repository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        UserDayService service =
                new UserDayService(
                        repository,
                        Clock.system(
                                SERVICE_ZONE
                        )
                );

        ProjectException exception =
                assertThrows(
                        ProjectException.class,
                        () ->
                                service.currentDay(
                                        999L
                                )
                );

        assertEquals(
                ErrorCode.USER_NOT_FOUND,
                exception.getErrorCode()
        );
    }

    private AppUser createUser() {
        return AppUser.createKakaoUser(
                "user-day-test",
                "데이빗",
                null,
                null
        );
    }
}
