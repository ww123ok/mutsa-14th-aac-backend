package mutsa.hackathon.service;

import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardImageSource;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyRewardCompletionNotificationTest {

    @Mock
    private WeeklyRewardRepository
            weeklyRewardRepository;

    @Mock
    private ApplicationEventPublisher
            eventPublisher;

    @Test
    void publishesNotificationWhenWeeklyImageCompletes() {
        Clock clock = Clock.fixed(
                Instant.parse(
                        "2026-08-10T00:00:00Z"
                ),
                ZoneId.of(
                        "Asia/Seoul"
                )
        );

        WeeklyRewardCompletionService service =
                new WeeklyRewardCompletionService(
                        weeklyRewardRepository,
                        clock,
                        eventPublisher
                );

        AppUser user =
                AppUser.createKakaoUser(
                        "provider-1",
                        "사용자",
                        null,
                        null
                );
        ReflectionTestUtils.setField(
                user,
                "id",
                1L
        );

        WeeklyReward reward =
                WeeklyReward.createPending(
                        user,
                        LocalDate.of(
                                2026,
                                8,
                                3
                        ),
                        LocalDate.of(
                                2026,
                                8,
                                9
                        )
                );
        ReflectionTestUtils.setField(
                reward,
                "id",
                30L
        );

        reward.claimGeneration(
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        8,
                        0
                ),
                3,
                Duration.ofHours(1)
        );

        when(
                weeklyRewardRepository
                        .findByIdForUpdate(30L)
        ).thenReturn(Optional.of(reward));

        service.complete(
                30L,
                new WeeklyRewardResultText(
                        "이번 주의 기록",
                        "이번 주의 기록을 한 장의 이미지로 정리했습니다. 주요 순간을 이미지에 담았습니다.",
                        List.of("기록")
                ),
                new GeneratedWeeklyImage(
                        new byte[]{1},
                        "image/webp",
                        "webp",
                        WeeklyRewardImageSource.AI
                ),
                new StoredWeeklyImage(
                        "weekly/30.webp",
                        "image/webp"
                )
        );

        verify(eventPublisher)
                .publishEvent(
                        InAppNotificationRequested
                                .weeklyRewardCompleted(
                                        1L,
                                        30L
                                )
                );
    }
}
