package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryShare;
import mutsa.hackathon.domain.DiaryShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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
}