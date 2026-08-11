package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event
        .TransactionPhase;
import org.springframework.transaction.event
        .TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.openai",
        name = "memory-extraction-enabled",
        havingValue = "true"
)
public class DiaryMemoryExtractionListener {

    private final DiaryMemoryExtractionService
            diaryMemoryExtractionService;

    /**
     * 일기 저장 트랜잭션이 완전히 성공한 뒤
     * 별도 스레드에서 기억 추출을 수행
     */
    @Async("diaryMemoryExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            DiaryMemoryExtractionRequested event
    ) {
        diaryMemoryExtractionService
                .extractAndApply(
                        event.diaryId()
                );
    }
}