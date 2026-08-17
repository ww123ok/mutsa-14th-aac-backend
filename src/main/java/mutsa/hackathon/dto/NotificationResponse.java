package mutsa.hackathon.dto;

import mutsa.hackathon.domain.Notification;
import mutsa.hackathon.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(
            Notification notification
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
