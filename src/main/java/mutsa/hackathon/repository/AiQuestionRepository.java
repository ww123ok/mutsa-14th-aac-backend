package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiQuestionRepository extends JpaRepository<AiQuestion, Long> {

    long countByUserIdAndQuestionTypeAndAskedDate(
            Long userId,
            AiQuestionType questionType,
            LocalDate askedDate
    );

    Optional<AiQuestion> findByIdAndUserId(Long questionId, Long userId);

    Optional<AiQuestion> findByDiaryIdAndQuestionType(
            Long diaryId,
            AiQuestionType questionType
    );
}