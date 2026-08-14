package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
public class WeeklyRewardClaimService {

    private final WeeklyRewardRepository weeklyRewardRepository;
    private final WeeklyRewardEntryRepository weeklyRewardEntryRepository;
    private final Clock weeklyRewardClock;

    @Value("${app.weekly-reward.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.weekly-reward.stale-minutes:30}")
    private long staleMinutes;

    @Transactional
    public ClaimResult claim(Long weeklyRewardId) {
        WeeklyReward reward = weeklyRewardRepository
                .findByIdForUpdate(weeklyRewardId)
                .orElse(null);

        if (reward == null) {
            return ClaimResult.notClaimed();
        }

        boolean claimed = reward.claimGeneration(
                LocalDateTime.now(weeklyRewardClock),
                maxAttempts,
                Duration.ofMinutes(staleMinutes)
        );
        if (!claimed) {
            return ClaimResult.notClaimed();
        }

        List<WeeklyRewardEntry> entries = weeklyRewardEntryRepository
                .findAllWithDiary(reward.getId());

        WeeklyRewardGenerationContext context = new WeeklyRewardGenerationContext(
                reward.getId(),
                reward.getUser().getId(),
                reward.getWeekStartDate(),
                reward.getWeekEndDate(),
                entries.stream()
                        .map(entry -> new WeeklyRewardGenerationContext.DayRecord(
                                entry.getRecordedDate(),
                                entry.getDiary().getContent(),
                                entry.getColorHex(),
                                entry.getKeywords()
                        ))
                        .toList()
        );

        return ClaimResult.claimed(context);
    }

    public record ClaimResult(
            boolean claimed,
            WeeklyRewardGenerationContext context
    ) {
        public static ClaimResult claimed(WeeklyRewardGenerationContext context) {
            return new ClaimResult(true, context);
        }

        public static ClaimResult notClaimed() {
            return new ClaimResult(false, null);
        }
    }
}