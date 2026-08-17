package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardScheduler {

    private final WeeklyRewardUserScheduleService
            userScheduleService;

    /**
     * 서버가 사용자별 정확한 생성 시점을 놓친 경우에도
     * 기동 시 이미 +5분 경계가 지난 사용자만 복구
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterRestart() {
        runCatchUp("startup");
    }

    /**
     * 매분 사용자별 DAYBIT 시작 시간을 확인.
     * 실제 보상 생성은 각 사용자의 월요일 시작 시간 + delay(기본 5분)에만 수행.
     * 예: 06:00 사용자 -> 월요일 06:05 생성
     */
    @Scheduled(
            cron = "${app.weekly-reward.boundary-cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    public void createAtUserBoundary() {
        try {
            WeeklyRewardUserScheduleService.ScheduleResult result =
                    userScheduleService
                            .generateUsersAtCurrentBoundary();

            if (result.candidateCount() > 0) {
                log.info(
                        "Weekly reward user boundary finished: candidates={}, eligible={}, failed={}",
                        result.candidateCount(),
                        result.eligibleCount(),
                        result.failedCount()
                );
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Weekly reward user boundary failed safely: reason={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    /**
     * 일간 색 보상 지연, S3 일시 장애, 서버 중단 등을 복구하기 위해
     * 월·화요일 매시간 15분에 이미 생성 시각이 지난 사용자만 재검사
     */
    @Scheduled(
            cron = "${app.weekly-reward.retry-cron:0 15 * * * MON,TUE}",
            zone = "Asia/Seoul"
    )
    public void retryDueUsers() {
        runCatchUp("retry");
    }

    private void runCatchUp(String trigger) {
        try {
            WeeklyRewardUserScheduleService.ScheduleResult result =
                    userScheduleService.catchUpDueUsers();

            log.info(
                    "Weekly reward user catch-up finished: trigger={}, candidates={}, eligible={}, failed={}",
                    trigger,
                    result.candidateCount(),
                    result.eligibleCount(),
                    result.failedCount()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Weekly reward user catch-up failed safely: trigger={}, reason={}",
                    trigger,
                    exception.getClass().getSimpleName()
            );
        }
    }
}
