package mutsa.hackathon.repository;

import mutsa.hackathon.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    boolean existsByUserIdAndRecordedDate(Long userId, LocalDate recordedDate);

    Optional<Diary> findByIdAndUserIdAndDeletedFalse(Long diaryId, Long userId);

    Optional<Diary> findByUserIdAndRecordedDateAndDeletedFalse(
            Long userId,
            LocalDate recordedDate
    );

    List<Diary> findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}