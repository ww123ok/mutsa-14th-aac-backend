package mutsa.hackathon.service;

import lombok.RequiredArgsConstructor;
import mutsa.hackathon.domain.AppUser;
import mutsa.hackathon.domain.Notification;
import mutsa.hackathon.dto.NotificationReadAllResponse;
import mutsa.hackathon.dto.NotificationResponse;
import mutsa.hackathon.dto.NotificationUnreadCountResponse;
import mutsa.hackathon.global.code.ErrorCode;
import mutsa.hackathon.global.exception.ProjectException;
import mutsa.hackathon.repository.AppUserRepository;
import mutsa.hackathon.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final Clock serviceClock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(
            InAppNotificationRequested event
    ) {
        if (
                notificationRepository
                        .existsByDedupKey(
                                event.dedupKey()
                        )
        ) {
            return;
        }

        AppUser user =
                appUserRepository
                        .findById(event.userId())
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        notificationRepository.save(
                Notification.create(
                        user,
                        event.type(),
                        event.message(),
                        event.referenceId(),
                        event.dedupKey()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(
            Long userId,
            Integer limit
    ) {
        int safeLimit = normalizeLimit(limit);

        return notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(
                                0,
                                safeLimit
                        )
                )
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse
    unreadCount(
            Long userId
    ) {
        return new NotificationUnreadCountResponse(
                notificationRepository
                        .countByUserIdAndReadAtIsNull(
                                userId
                        )
        );
    }

    @Transactional
    public NotificationResponse markRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                notificationRepository
                        .findByIdAndUserId(
                                notificationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ProjectException(
                                        ErrorCode.NOTIFICATION_NOT_FOUND
                                )
                        );

        notification.markRead(
                LocalDateTime.now(
                        serviceClock
                )
        );

        return NotificationResponse.from(
                notification
        );
    }

    @Transactional
    public NotificationReadAllResponse markAllRead(
            Long userId
    ) {
        List<Notification> unread =
                notificationRepository
                        .findAllByUserIdAndReadAtIsNull(
                                userId
                        );

        LocalDateTime readAt =
                LocalDateTime.now(
                        serviceClock
                );

        unread.forEach(
                notification ->
                        notification.markRead(
                                readAt
                        )
        );

        return new NotificationReadAllResponse(
                unread.size()
        );
    }

    private int normalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(
                1,
                Math.min(
                        limit,
                        MAX_LIMIT
                )
        );
    }
}
