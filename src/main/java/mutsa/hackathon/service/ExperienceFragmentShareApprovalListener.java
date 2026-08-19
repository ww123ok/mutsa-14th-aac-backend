package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExperienceFragmentShareApprovalListener {

    private final ExperienceFragmentService experienceFragmentService;

    @Async("diaryMemoryExecutor")
    @EventListener
    public void handle(ExperienceFragmentShareApproved event) {
        try {
            experienceFragmentService.matchPendingQueriesForApprovedShare(event.shareId());
        } catch (RuntimeException exception) {
            log.warn("Pending experience query matching failed: shareId={}, reason={}",
                    event.shareId(), exception.getClass().getSimpleName());
        }
    }
}
