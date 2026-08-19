package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardDailyCompletionListener {

    private final WeeklyRewardUserScheduleService
            userScheduleService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            DiaryRewardCompletedEvent event
    ) {
        try {
            userScheduleService.generateForCompletedDiaryIfDue(
                    event.userId(),
                    event.recordedDate()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Deferred weekly reward trigger failed safely: userId={}, diaryId={}, reason={}",
                    event.userId(),
                    event.diaryId(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
