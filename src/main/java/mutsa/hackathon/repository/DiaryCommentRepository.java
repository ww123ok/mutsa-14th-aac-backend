package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryCommentRepository
        extends JpaRepository<DiaryComment, Long> {

    List<DiaryComment>
    findAllByDiaryIdOrderByCreatedAtAscIdAsc(
            Long diaryId
    );

    long deleteAllByDiaryId(
            Long diaryId
    );
}
