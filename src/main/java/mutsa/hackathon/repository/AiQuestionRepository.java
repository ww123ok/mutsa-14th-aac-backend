package mutsa.hackathon.repository;

import mutsa.hackathon.domain.AiQuestion;
import mutsa.hackathon.domain.AiQuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AiQuestionRepository
        extends JpaRepository<AiQuestion, Long> {

    long countByUserIdAndQuestionTypeAndAskedDate(
            Long userId,
            AiQuestionType questionType,
            LocalDate askedDate
    );

    /**
     * 같은 날 먼저 생성된 작성 도움 질문을
     * 순서대로 조회.
     * 다음 질문 생성 시 이전 질문과 다른 방향의
     * 질문을 만들기 위한 컨텍스트로 사용.
     */
    List<AiQuestion>
    findAllByUserIdAndQuestionTypeAndAskedDateOrderByQuestionOrderAsc(
            Long userId,
            AiQuestionType questionType,
            LocalDate askedDate
    );

    List<AiQuestion>
    findTop12ByUserIdAndQuestionTypeAndAskedDateBeforeOrderByAskedDateDescQuestionOrderDesc(
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

    long deleteAllByDiaryId(
            Long diaryId
    );

    long deleteAllByUserIdAndQuestionTypeAndAskedDate(
            Long userId,
            AiQuestionType questionType,
            LocalDate askedDate
    );
}
