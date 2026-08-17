package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자별 DAYBIT 논리 날짜를 계산하는 단일 기준점.
 * 예: dayStartTime=06:00
 * - 2026-08-14 05:59 -> 2026-08-13
 * - 2026-08-14 06:00 -> 2026-08-14
 * Clock은 애플리케이션의 Asia/Seoul Clock bean을 사용.
 */
@Service
@RequiredArgsConstructor
public class UserDayService {

    private final AppUserRepository
            appUserRepository;

    private final Clock serviceClock;

    @Transactional(readOnly = true)
    public LocalDate currentDay(
            Long userId
    ) {
        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        return resolveDay(
                LocalDateTime.now(
                        serviceClock
                ),
                user.getDayStartTime()
        );
    }

    /**
     * 경계값 테스트 및 향후 주간 보상 등에서 재사용 가능한
     * 순수 날짜 계산 로직.
     */
    LocalDate resolveDay(
            LocalDateTime now,
            LocalTime dayStartTime
    ) {
        LocalTime effectiveStartTime =
                dayStartTime == null
                        ? LocalTime.MIDNIGHT
                        : dayStartTime;

        LocalDate calendarDate =
                now.toLocalDate();

        if (
                now.toLocalTime()
                        .isBefore(
                                effectiveStartTime
                        )
        ) {
            return calendarDate
                    .minusDays(1);
        }

        return calendarDate;
    }
}
