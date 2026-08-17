package mutsa.hackathon.repository;

import mutsa.hackathon.domain.SharedDiaryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface SharedDiaryLogRepository extends JpaRepository<SharedDiaryLog, Long> {

    boolean existsByReceiverIdAndDiaryShareId(Long receiverId, Long diaryShareId);

    boolean existsByDiaryShareId(Long diaryShareId);

    long deleteAllByDiaryShareId(Long diaryShareId);

    Optional<SharedDiaryLog> findByIdAndReceiverId(Long logId, Long receiverId);

    @Query("""
            select distinct delivery
            from SharedDiaryLog delivery
            join fetch delivery.diaryShare share
            left join fetch share.keywords
            where delivery.receiver.id = :receiverId
            order by delivery.createdAt desc, delivery.id desc
            """)
    List<SharedDiaryLog> findAllReceivedByReceiverId(
            @Param("receiverId") Long receiverId
    );

    List<SharedDiaryLog> findTop3ByDiaryShareIdAndFeedbackSummaryIsNotNullOrderByFeedbackSubmittedAtAscIdAsc(
            Long diaryShareId
    );
}
