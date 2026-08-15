package mutsa.hackathon.repository;

import mutsa.hackathon.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 추후 비동기 개인화 기억 추출 과정에서
     * 사용자 설정까지 안전하게 사용할 수 있도록
     * User를 fetch join
     */
    @Query("""
            select diary
            from Diary diary
            join fetch diary.user
            where diary.id = :diaryId
            """)
    Optional<Diary> findByIdWithUser(
            @Param("diaryId")
            Long diaryId
    );

    /**
     * 개발용 하드 삭제에서는 Soft Delete된 일기도
     * 찾아야 하므로 deleted 조건을 사용하지 않음.
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

    @Query("""
        select diary.user.id
        from Diary diary
        where diary.deleted = false
          and diary.recordedDate between :startDate and :endDate
        group by diary.user.id
        having count(diary.id) >= :minimumCount
        order by diary.user.id asc
        """)
    List<Long> findEligibleUserIdsForWeek(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minimumCount") long minimumCount
    );
}