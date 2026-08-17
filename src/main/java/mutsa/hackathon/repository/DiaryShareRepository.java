package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.DiaryShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryShareRepository
        extends JpaRepository<DiaryShare, Long> {

    Optional<DiaryShare> findByDiaryId(
            Long diaryId
    );

    boolean existsByDiaryId(
            Long diaryId
    );

    Optional<DiaryShare> findByIdAndDiaryUserId(
            Long shareId,
            Long userId
    );

    List<DiaryShare> findAllByShareStatus(
            DiaryShareStatus shareStatus
    );

    boolean existsByShareStatus(DiaryShareStatus shareStatus);

    @Query("""
            select share from DiaryShare share
            join fetch share.diary diary
            join fetch diary.user
            where share.id = :shareId
            """)
    Optional<DiaryShare> findByIdWithDiaryAndUser(
            @Param("shareId") Long shareId
    );

    List<DiaryShare> findAllByDiaryUserIdOrderByCreatedAtDesc(Long userId);
}
