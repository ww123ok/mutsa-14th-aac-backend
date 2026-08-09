package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiQuestionRepository
        extends JpaRepository<AiQuestion, Long> {

    long countByUserIdAndQuestionTypeAndAskedDate(
            Long userId,
            AiQuestionType questionType,
            LocalDate askedDate
    );

    Optional<AiQuestion> findByIdAndUserId(
            Long questionId,
            Long userId
    );

    Optional<AiQuestion>
    findByDiaryIdAndQuestionType(
            Long diaryId,
            AiQuestionType questionType
    );

    /**
     * 특정 일기에 연결된 성찰 질문을 하드 삭제.
     * 작성 도움 질문은 diary_id가 null이므로
     * 이 메서드의 영향을 받지 않음.
     */
    long deleteAllByDiaryId(
            Long diaryId
    );
}