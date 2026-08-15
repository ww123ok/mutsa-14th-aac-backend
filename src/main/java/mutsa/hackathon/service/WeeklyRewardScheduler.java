package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.util.WeeklyRewardPeriod;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardScheduler {

    private final WeeklyRewardBatchService batchService;
    private final Clock weeklyRewardClock;

    /**
     * 월요일에 서버가 중단되어 cron 자체를 놓친 경우에도 다음 기동 시
     * 직전 완료 주차를 한 번 복구합니다. 이미 완료된 보상은 DB UNIQUE와
     * Claim 정책에 의해 다시 생성되지 않습니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterRestart() {
        runPreviousWeek("startup");
    }

    /**
     * 일요일 기록까지 포함해야 하므로 일요일 시작 00:00이 아니라,
     * 일요일이 끝난 뒤인 월요일 00:05 KST에 지난주 생성을 시작합니다.
     */
    @Scheduled(
            cron = "${app.weekly-reward.initial-cron:0 5 0 * * MON}",
            zone = "Asia/Seoul"
    )
    public void createPreviousWeek() {
        runPreviousWeek("initial");
    }

    /**
     * 서버 재시작, 일간 색 보상 지연, S3 일시 장애를 복구하기 위해
     * 월요일에는 매시간 15분에 동일 주차를 재검사합니다.
     * DB UNIQUE와 생성 Claim이 중복 생성을 차단합니다.
     */
    @Scheduled(
            cron = "${app.weekly-reward.retry-cron:0 15 * * * MON}",
            zone = "Asia/Seoul"
    )
    public void retryPreviousWeek() {
        runPreviousWeek("retry");
    }

    private void runPreviousWeek(String trigger) {
        try {
            LocalDate today = LocalDate.now(weeklyRewardClock);
            WeeklyRewardPeriod period = WeeklyRewardPeriod.previousCompletedWeek(today);
            WeeklyRewardBatchService.BatchResult result = batchService.generateWeek(
                    period.startDate()
            );
            log.info(
                    "Weekly reward batch finished: trigger={}, weekStart={}, eligible={}, prepared={}, dispatched={}",
                    trigger,
                    result.weekStartDate(),
                    result.eligibleUserCount(),
                    result.preparedCount(),
                    result.dispatchedCount()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Weekly reward batch failed safely: trigger={}, reason={}",
                    trigger,
                    exception.getClass().getSimpleName()
            );
        }
    }
}