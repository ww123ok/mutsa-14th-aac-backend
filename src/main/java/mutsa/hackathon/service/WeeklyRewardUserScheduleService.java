package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.dto.WeeklyRewardTriggerResponse;
import mutsa.hackathon.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자별 DAYBIT 하루 시작 시간을 기준으로 주간 보상 생성 시점을 결정한다.
 *
 * 예: dayStartTime=06:00, delay=5분
 * - 월요일 06:04: 아직 전주 보상 자동 생성을 시작하지 않음
 * - 월요일 06:05: 직전 월~일 주차 보상 생성을 시작
 *
 * 기존 사용자의 null dayStartTime은 00:00으로 취급한다.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardUserScheduleService {

    private final AppUserRepository appUserRepository;
    private final WeeklyRewardBatchService batchService;
    private final UserDayService userDayService;
    private final Clock weeklyRewardClock;

    @Value("${app.weekly-reward.schedule-delay-minutes:5}")
    private long scheduleDelayMinutes;

    /**
     * 매분 호출되는 정상 경로.
     * 현재 시각에서 delay를 뺀 시각이 월요일의 사용자 dayStartTime과
     * 정확히 일치하는 사용자만 선택하여 생성한다.
     */
    public ScheduleResult generateUsersAtCurrentBoundary() {
        return generateUsersAtBoundary(
                LocalDateTime.now(weeklyRewardClock)
        );
    }

    ScheduleResult generateUsersAtBoundary(
            LocalDateTime now
    ) {
        validateDelay();

        LocalDateTime boundary = normalizeToMinute(
                now.minusMinutes(scheduleDelayMinutes)
        );

        if (boundary.getDayOfWeek() != DayOfWeek.MONDAY) {
            return ScheduleResult.empty();
        }

        LocalTime targetStartTime = boundary.toLocalTime();
        Set<Long> userIds = new LinkedHashSet<>();
        appUserRepository.findAllByDayStartTime(
                        targetStartTime
                )
                .stream()
                .map(AppUser::getId)
                .forEach(userIds::add);

        if (targetStartTime.equals(LocalTime.MIDNIGHT)) {
            appUserRepository.findAllByDayStartTimeIsNull()
                    .stream()
                    .map(AppUser::getId)
                    .forEach(userIds::add);
        }

        LocalDate rewardWeekStart = boundary
                .toLocalDate()
                .minusWeeks(1);

        return triggerUsers(
                userIds,
                rewardWeekStart
        );
    }

    /**
     * 하루 경계를 넘겨 작성하던 전주 일기의 색 보상이 늦게 완료된 경우,
     * 정규 +5분 스케줄을 이미 지났다면 해당 사용자만 즉시 재검사한다.
     */
    public Optional<WeeklyRewardTriggerResponse>
    generateForCompletedDiaryIfDue(
            Long userId,
            LocalDate diaryRecordedDate
    ) {
        validateDelay();

        AppUser user = appUserRepository.findById(userId)
                .orElse(null);
        if (user == null || diaryRecordedDate == null) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now(weeklyRewardClock);
        LocalDate rewardWeekStart = diaryRecordedDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate currentLogicalWeekStart = userDayService.resolveDay(
                        now,
                        user.getDayStartTime()
                )
                .with(
                        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                );

        if (!rewardWeekStart.equals(currentLogicalWeekStart.minusWeeks(1))) {
            return Optional.empty();
        }

        LocalDateTime generationDueAt = LocalDateTime.of(
                        rewardWeekStart.plusWeeks(1),
                        user.getDayStartTime()
                )
                .plusMinutes(scheduleDelayMinutes);

        if (now.isBefore(generationDueAt)) {
            return Optional.empty();
        }

        return Optional.of(
                batchService.generateForUser(
                        userId,
                        rewardWeekStart
                )
        );
    }

    /**
     * 서버 재시작 또는 일간 색 보상 지연 등으로 정확한 +5분 시점을 놓친 경우
     * 현재 사용자 설정 기준으로 이미 생성 시각이 지난 사용자만 복구한다.
     *
     * 동일 주차 중복 생성은 WeeklyReward UNIQUE/Claim 정책이 차단한다.
     */
    public ScheduleResult catchUpDueUsers() {
        return catchUpDueUsers(
                LocalDateTime.now(weeklyRewardClock)
        );
    }

    ScheduleResult catchUpDueUsers(
            LocalDateTime now
    ) {
        validateDelay();

        int candidateCount = 0;
        int eligibleCount = 0;
        int failedCount = 0;

        for (AppUser user : appUserRepository.findAll()) {
            LocalTime dayStartTime = user.getDayStartTime();
            LocalDate logicalDay = userDayService.resolveDay(
                    now,
                    dayStartTime
            );

            LocalDate logicalWeekStart = logicalDay.with(
                    TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                    )
            );

            LocalDateTime generationDueAt = LocalDateTime.of(
                            logicalWeekStart,
                            dayStartTime
                    )
                    .plusMinutes(scheduleDelayMinutes);

            if (now.isBefore(generationDueAt)) {
                continue;
            }

            LocalDate rewardWeekStart = logicalWeekStart
                    .minusWeeks(1);
            candidateCount++;

            try {
                WeeklyRewardTriggerResponse response =
                        batchService.generateForUser(
                                user.getId(),
                                rewardWeekStart
                        );
                if (response.eligible()) {
                    eligibleCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn(
                        "Weekly reward user catch-up failed: userId={}, weekStart={}, reason={}",
                        user.getId(),
                        rewardWeekStart,
                        exception.getClass().getSimpleName()
                );
            }
        }

        return new ScheduleResult(
                candidateCount,
                eligibleCount,
                failedCount
        );
    }

    private ScheduleResult triggerUsers(
            Iterable<Long> userIds,
            LocalDate rewardWeekStart
    ) {
        int candidateCount = 0;
        int eligibleCount = 0;
        int failedCount = 0;

        for (Long userId : userIds) {
            candidateCount++;
            try {
                WeeklyRewardTriggerResponse response =
                        batchService.generateForUser(
                                userId,
                                rewardWeekStart
                        );
                if (response.eligible()) {
                    eligibleCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn(
                        "Weekly reward user schedule failed: userId={}, weekStart={}, reason={}",
                        userId,
                        rewardWeekStart,
                        exception.getClass().getSimpleName()
                );
            }
        }

        return new ScheduleResult(
                candidateCount,
                eligibleCount,
                failedCount
        );
    }

    private LocalDateTime normalizeToMinute(
            LocalDateTime dateTime
    ) {
        return dateTime
                .withSecond(0)
                .withNano(0);
    }

    private void validateDelay() {
        if (scheduleDelayMinutes < 0) {
            throw new IllegalStateException(
                    "주간 보상 생성 지연 시간은 0분 이상이어야 합니다."
            );
        }
    }

    public record ScheduleResult(
            int candidateCount,
            int eligibleCount,
            int failedCount
    ) {
        public static ScheduleResult empty() {
            return new ScheduleResult(0, 0, 0);
        }
    }
}
