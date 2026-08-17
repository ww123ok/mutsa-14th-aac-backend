package mutsa.hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "notification",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_dedup_key",
                        columnNames = "dedup_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_notification_user_created",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_notification_user_read",
                        columnList = "user_id, read_at"
                )
        }
)
public class Notification extends BaseEntity {

    private static final int MAX_MESSAGE_LENGTH = 300;
    private static final int MAX_DEDUP_KEY_LENGTH = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = MAX_MESSAGE_LENGTH)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(
            name = "dedup_key",
            nullable = false,
            length = MAX_DEDUP_KEY_LENGTH
    )
    private String dedupKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create(
            AppUser user,
            NotificationType type,
            String message,
            Long referenceId,
            String dedupKey
    ) {
        if (user == null || type == null) {
            throw new IllegalArgumentException(
                    "알림 사용자와 타입은 필수입니다."
            );
        }

        return Notification.builder()
                .user(user)
                .type(type)
                .message(
                        normalizeRequired(
                                message,
                                MAX_MESSAGE_LENGTH,
                                "알림 내용은 필수입니다."
                        )
                )
                .referenceId(referenceId)
                .dedupKey(
                        normalizeRequired(
                                dedupKey,
                                MAX_DEDUP_KEY_LENGTH,
                                "알림 중복 방지 키는 필수입니다."
                        )
                )
                .build();
    }

    public void markRead(LocalDateTime readAt) {
        if (this.readAt != null) {
            return;
        }
        if (readAt == null) {
            throw new IllegalArgumentException(
                    "알림 확인 시각은 필수입니다."
            );
        }
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    private static String normalizeRequired(
            String value,
            int maxLength,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
