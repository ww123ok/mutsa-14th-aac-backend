package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardEntry;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import mutsa.hackathon.dto.WeeklyRewardArchiveResponse;
import mutsa.hackathon.dto.WeeklyRewardResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.WeeklyRewardEntryRepository;
import mutsa.hackathon.repository.WeeklyRewardRepository;
import mutsa.hackathon.util.WeeklyRewardPeriod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.weekly-reward",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class WeeklyRewardQueryService {

    private final WeeklyRewardRepository weeklyRewardRepository;

    private final WeeklyRewardEntryRepository
            weeklyRewardEntryRepository;

    private final WeeklyImageStorage imageStorage;

    private final Clock weeklyRewardClock;

    @Value(
            "${app.weekly-reward.image-url-expiration-minutes:15}"
    )
    private long imageUrlExpirationMinutes;

    @Transactional(readOnly = true)
    public WeeklyRewardArchiveResponse getMonthlyArchive(
            Long userId,
            int year,
            int month
    ) {
        List<WeeklyReward> rewards =
                weeklyRewardRepository
                        .findAllByUserIdAndWeekStartDateBetweenOrderByWeekStartDateAsc(
                                userId,
                                WeeklyRewardPeriod
                                        .firstCalendarWeekStart(
                                                year,
                                                month
                                        ),
                                WeeklyRewardPeriod
                                        .lastCalendarWeekStart(
                                                year,
                                                month
                                        )
                        );

        List<WeeklyRewardResponse> items =
                rewards.stream()
                        .map(this::toResponse)
                        .toList();

        return new WeeklyRewardArchiveResponse(
                year,
                month,
                items
        );
    }

    @Transactional(readOnly = true)
    public WeeklyRewardResponse getOne(
            Long userId,
            Long weeklyRewardId
    ) {
        WeeklyReward reward =
                weeklyRewardRepository
                        .findByIdAndUserId(
                                weeklyRewardId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode
                                                .WEEKLY_REWARD_NOT_FOUND
                                )
                        );

        return toResponse(reward);
    }

    @Transactional
    public WeeklyRewardResponse markViewed(
            Long userId,
            Long weeklyRewardId
    ) {
        WeeklyReward reward = weeklyRewardRepository
                .findByIdAndUserId(weeklyRewardId, userId)
                .orElseThrow(() -> new ProjectException(ErrorCode.WEEKLY_REWARD_NOT_FOUND));

        if (reward.getGenerationStatus() != WeeklyRewardStatus.COMPLETED
                || reward.getImageKey() == null
                || reward.getImageKey().isBlank()) {
            throw new ProjectException(ErrorCode.WEEKLY_REWARD_NOT_VIEWABLE);
        }

        reward.markViewed();
        return toResponse(reward);
    }

    private WeeklyRewardResponse toResponse(
            WeeklyReward reward
    ) {
        List<WeeklyRewardEntry> entries =
                weeklyRewardEntryRepository
                        .findAllByWeeklyRewardIdOrderByRecordedDateAsc(
                                reward.getId()
                        );

        String imageUrl = null;
        Instant expiresAt = null;

        if (
                reward.getGenerationStatus()
                        == WeeklyRewardStatus.COMPLETED
                        && reward.getImageKey() != null
        ) {
            Duration duration =
                    Duration.ofMinutes(
                            imageUrlExpirationMinutes
                    );

            try {
                URI uri =
                        imageStorage.createReadUri(
                                reward.getImageKey(),
                                duration
                        );

                imageUrl = uri.toString();

                expiresAt =
                        weeklyRewardClock
                                .instant()
                                .plus(duration);

            } catch (RuntimeException exception) {
                log.warn(
                        "Weekly reward image URL creation failed: weeklyRewardId={}, reason={}",
                        reward.getId(),
                        exception
                                .getClass()
                                .getSimpleName()
                );
            }
        }

        return WeeklyRewardResponse.from(
                reward,
                entries,
                imageUrl,
                expiresAt
        );
    }
}
