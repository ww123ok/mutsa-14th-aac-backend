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
public class ExperienceFragmentGenerationListener {
    private final ExperienceFragmentService experienceFragmentService;

    @Async("diaryMemoryExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ExperienceFragmentGenerationRequested event) {
        log.info("Experience fragment generation started: shareId={}", event.shareId());
        experienceFragmentService.generateDraft(event.shareId());
    }
}
