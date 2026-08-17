package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.DiaryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DiaryReminderNotificationScheduler {

    private final AppUserRepository
            appUserRepository;

    private final DiaryRepository
            diaryRepository;

    private final UserDayService
            userDayService;

    private final ApplicationEventPublisher
            eventPublisher;

    private final Clock serviceClock;

    /**
     * 사용자별 알림 시간이 다르므로 1분마다 현재 분에 해당하는
     * 사용자만 조회한다. 알림 시간은 온보딩에서 HH:mm 단위로 저장된다.
     */
    @Scheduled(
            cron = "${app.notification.diary-reminder-cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    @Transactional(readOnly = true)
    public void createDueDiaryReminders() {
        LocalDateTime now =
                LocalDateTime.now(
                        serviceClock
                );

        LocalTime currentMinute =
                now.toLocalTime()
                        .withSecond(0)
                        .withNano(0);

        List<AppUser> users =
                appUserRepository
                        .findAllByDiaryReminderTime(
                                currentMinute
                        );

        for (AppUser user : users) {
            LocalDate logicalDate =
                    userDayService.resolveDay(
                            now,
                            user.getDayStartTime()
                    );

            boolean completed =
                    diaryRepository
                            .existsByUserIdAndRecordedDate(
                                    user.getId(),
                                    logicalDate
                            );

            if (completed) {
                continue;
            }

            eventPublisher.publishEvent(
                    InAppNotificationRequested
                            .diaryReminder(
                                    user.getId(),
                                    logicalDate
                            )
            );
        }
    }
}
