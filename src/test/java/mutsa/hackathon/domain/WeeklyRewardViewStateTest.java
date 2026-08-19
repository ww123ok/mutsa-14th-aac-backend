package mutsa.hackathon.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyRewardViewStateTest {

    @Test
    void marksCompletedWeeklyRewardAsViewed() {
        AppUser user = AppUser.createLocalUser("weekly@example.com", "encoded-password");
        WeeklyReward reward = WeeklyReward.createPending(
                user,
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23)
        );
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
        reward.claimGeneration(now, 2, Duration.ofMinutes(10));
        reward.complete(
                "이번 주의 기록",
                "이번 주를 정리한 문장입니다.",
                "그래픽 포스터",
                List.of("기록", "일상", "흐름"),
                "weekly-rewards/test.png",
                "image/png",
                WeeklyRewardImageSource.FALLBACK,
                now
        );

        assertFalse(reward.isViewed());

        reward.markViewed();

        assertTrue(reward.isViewed());
        assertNotNull(reward.getViewedAt());
    }
}
