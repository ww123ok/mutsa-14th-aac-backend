package mutsa.hackathon.repository;

import mutsa.hackathon.domain.SharedDiaryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SharedDiaryLogRepository extends JpaRepository<SharedDiaryLog, Long> {

    boolean existsByReceiverIdAndDiaryShareId(Long receiverId, Long diaryShareId);

    boolean existsByDiaryShareId(Long diaryShareId);

    Optional<SharedDiaryLog> findByIdAndReceiverId(Long logId, Long receiverId);

    List<SharedDiaryLog> findTop3ByDiaryShareIdAndFeedbackSummaryIsNotNullOrderByFeedbackSubmittedAtAscIdAsc(
            Long diaryShareId
    );
}
