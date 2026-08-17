package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService
            notificationService;

    /**
     * 경험조각/피드백/주간보상은 원본 트랜잭션이
     * 실제 커밋된 뒤에만 알림을 저장한다.
     *
     * 스케줄러처럼 트랜잭션 밖에서 발행되는 이벤트도
     * fallbackExecution으로 즉시 처리한다.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(
            InAppNotificationRequested event
    ) {
        try {
            notificationService.create(event);
        } catch (
                DataIntegrityViolationException exception
        ) {
            /*
             * 여러 서버 인스턴스가 같은 알림을 동시에 만들더라도
             * dedup_key UNIQUE 제약이 최종 중복 방지 장치가 된다.
             */
            log.debug(
                    "Duplicate notification ignored: dedupKey={}",
                    event.dedupKey()
            );
        }
    }
}
