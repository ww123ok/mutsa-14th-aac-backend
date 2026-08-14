package mutsa.hackathon.service;

import java.net.URI;
import java.time.Duration;

public interface WeeklyImageStorage {
    StoredWeeklyImage store(
            WeeklyRewardGenerationContext context,
            GeneratedWeeklyImage image
    );

    URI createReadUri(String key, Duration duration);

    void delete(String key);
}