package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExperienceMatchQueryCleanupScheduler {

    private final ExperienceFragmentService experienceFragmentService;

    @Scheduled(
            cron = "${app.experience-sharing.pending-query-cleanup-cron:0 10 * * * *}",
            zone = "Asia/Seoul"
    )
    public void removeExpiredQueries() {
        experienceFragmentService.removeExpiredMatchQueries();
    }
}
