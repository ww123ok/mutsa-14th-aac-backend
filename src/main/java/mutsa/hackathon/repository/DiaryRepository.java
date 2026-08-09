package mutsa.hackathon.repository;

import mutsa.hackathon.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository
        extends JpaRepository<Diary, Long> {

    boolean existsByUserIdAndRecordedDate(
            Long userId,
            LocalDate recordedDate
    );

    Optional<Diary>
    findByIdAndUserIdAndDeletedFalse(
            Long diaryId,
            Long userId
    );

    Optional<Diary>
    findByUserIdAndRecordedDateAndDeletedFalse(
            Long userId,
            LocalDate recordedDate
    );

    /**
     * 개발용 하드 삭제에서는 Soft Delete된 일기도
     * 찾아야 하므로 deleted 조건을 사용하지 않음
     */
    Optional<Diary>
    findByUserIdAndRecordedDate(
            Long userId,
            LocalDate recordedDate
    );

    List<Diary>
    findAllByUserIdAndRecordedDateBetweenAndDeletedFalseOrderByRecordedDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}