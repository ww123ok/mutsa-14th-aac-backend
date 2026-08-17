package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExperienceFragmentMatchingListener {
    private final ExperienceFragmentService experienceFragmentService;

    @Async("diaryMemoryExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExperienceFragmentMatchingRequested event) {
        try {
            experienceFragmentService.createInboxArrival(event.diaryId());
        } catch (RuntimeException exception) {
            log.warn("Experience fragment matching failed: diaryId={}, reason={}", event.diaryId(),
                    exception.getClass().getSimpleName());
        }
    }
}
