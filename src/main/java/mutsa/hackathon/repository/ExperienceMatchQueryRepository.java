package mutsa.hackathon.repository;

import mutsa.hackathon.domain.ExperienceMatchQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExperienceMatchQueryRepository extends JpaRepository<ExperienceMatchQuery, Long> {

    Optional<ExperienceMatchQuery> findByDiaryId(Long diaryId);

    List<ExperienceMatchQuery> findAllByMatchedAtIsNullAndExpiresAtAfter(LocalDateTime now);

    long deleteAllByExpiresAtBefore(LocalDateTime now);
}
