package mutsa.hackathon.repository;

import mutsa.hackathon.domain.ExperienceFragmentArrival;
import mutsa.hackathon.domain.ExperienceFragmentArrivalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExperienceFragmentArrivalRepository extends JpaRepository<ExperienceFragmentArrival, Long> {

    boolean existsByReceiverIdAndDiaryShareId(Long receiverId, Long diaryShareId);

    long deleteAllByQueryDiaryId(Long queryDiaryId);

    long deleteAllByDiaryShareId(Long diaryShareId);

    @Query("""
            select arrival from ExperienceFragmentArrival arrival
            join fetch arrival.diaryShare share
            where arrival.receiver.id = :receiverId
            and arrival.status = :status
            order by arrival.createdAt desc
            """)
    List<ExperienceFragmentArrival> findAllByReceiverIdAndStatusWithShare(
            @Param("receiverId") Long receiverId,
            @Param("status") ExperienceFragmentArrivalStatus status
    );

    @Query("""
            select arrival from ExperienceFragmentArrival arrival
            join fetch arrival.receiver
            join fetch arrival.diaryShare share
            join fetch share.diary diary
            join fetch diary.user
            where arrival.id = :arrivalId
            """)
    Optional<ExperienceFragmentArrival> findByIdWithReceiverAndShare(
            @Param("arrivalId") Long arrivalId
    );
}
