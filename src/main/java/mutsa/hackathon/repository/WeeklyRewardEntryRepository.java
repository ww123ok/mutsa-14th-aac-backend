package mutsa.hackathon.repository;

import mutsa.hackathon.domain.WeeklyRewardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WeeklyRewardEntryRepository
        extends JpaRepository<WeeklyRewardEntry, Long> {

    List<WeeklyRewardEntry>
    findAllByWeeklyRewardIdOrderByRecordedDateAsc(
            Long weeklyRewardId
    );

    @Query("""
            select entry
            from WeeklyRewardEntry entry
            join fetch entry.diary
            where entry.weeklyReward.id = :weeklyRewardId
            order by entry.recordedDate asc
            """)
    List<WeeklyRewardEntry> findAllWithDiary(
            @Param("weeklyRewardId") Long weeklyRewardId
    );
}