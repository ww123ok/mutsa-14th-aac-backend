package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryRewardRepository extends JpaRepository<DiaryReward, Long> {

    Optional<DiaryReward> findByDiaryId(Long diaryId);
}