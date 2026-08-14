package mutsa.hackathon.repository;

import jakarta.persistence.LockModeType;
import mutsa.hackathon.domain.WeeklyReward;
import mutsa.hackathon.domain.WeeklyRewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WeeklyRewardRepository
        extends JpaRepository<WeeklyReward, Long> {

    Optional<WeeklyReward> findByUserIdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    Optional<WeeklyReward> findByIdAndUserId(
            Long weeklyRewardId,
            Long userId
    );

    List<WeeklyReward>
    findAllByUserIdAndWeekStartDateBetweenOrderByWeekStartDateAsc(
            Long userId,
            LocalDate startWeek,
            LocalDate endWeek
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reward
            from WeeklyReward reward
            join fetch reward.user
            where reward.id = :rewardId
            """)
    Optional<WeeklyReward> findByIdForUpdate(
            @Param("rewardId") Long rewardId
    );

    @Query("""
            select reward.id
            from WeeklyReward reward
            where reward.weekStartDate = :weekStartDate
              and reward.generationStatus in :statuses
            order by reward.id asc
            """)
    List<Long> findIdsByWeekAndStatuses(
            @Param("weekStartDate") LocalDate weekStartDate,
            @Param("statuses") Collection<WeeklyRewardStatus> statuses
    );
}