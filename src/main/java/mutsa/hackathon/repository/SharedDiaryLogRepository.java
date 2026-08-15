package mutsa.hackathon.repository;

import mutsa.hackathon.domain.SharedDiaryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedDiaryLogRepository extends JpaRepository<SharedDiaryLog, Long> {

    boolean existsByReceiverIdAndDiaryShareId(Long receiverId, Long diaryShareId);

    boolean existsByDiaryShareId(Long diaryShareId);

    long deleteAllByDiaryShareId(Long diaryShareId);

    Optional<SharedDiaryLog> findByIdAndReceiverId(Long logId, Long receiverId);
}
