package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.experience-sharing",
        name = "auto-approval-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ExperienceFragmentAutoApprovalScheduler {

    private final ExperienceFragmentService experienceFragmentService;

    @Scheduled(
            cron = "${app.experience-sharing.auto-approval-cron:0 0 * * * *}",
            zone = "Asia/Seoul"
    )
    public void approveExpiredReviews() {
        experienceFragmentService.autoApproveExpiredReviews();
    }
}
