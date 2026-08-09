package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiaryRewardRepository
        extends JpaRepository<DiaryReward, Long> {

    Optional<DiaryReward> findByDiaryId(
            Long diaryId
    );

    List<DiaryReward> findAllByDiaryIdIn(
            List<Long> diaryIds
    );

    /**
     * 비동기 처리 스레드에서 Diary 본문까지
     * 안전하게 사용할 수 있도록 fetch join
     */
    @Query("""
            select reward
            from DiaryReward reward
            join fetch reward.diary
            where reward.id = :rewardId
            """)
    Optional<DiaryReward> findByIdWithDiary(
            @Param("rewardId")
            Long rewardId
    );

    long deleteAllByDiaryId(
            Long diaryId
    );
}