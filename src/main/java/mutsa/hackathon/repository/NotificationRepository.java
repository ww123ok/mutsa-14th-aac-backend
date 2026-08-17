package mutsa.hackathon.repository;

import mutsa.hackathon.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    boolean existsByDedupKey(
            String dedupKey
    );

    Optional<Notification> findByIdAndUserId(
            Long notificationId,
            Long userId
    );

    List<Notification>
    findAllByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    long countByUserIdAndReadAtIsNull(
            Long userId
    );

    List<Notification>
    findAllByUserIdAndReadAtIsNull(
            Long userId
    );
}
