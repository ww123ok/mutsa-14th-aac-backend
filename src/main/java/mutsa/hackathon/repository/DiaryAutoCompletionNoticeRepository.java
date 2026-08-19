package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryAutoCompletionNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryAutoCompletionNoticeRepository
        extends JpaRepository<DiaryAutoCompletionNotice, Long> {

    Optional<DiaryAutoCompletionNotice>
    findFirstByUserIdAndViewedAtIsNullOrderByAutoCompletedAtAscIdAsc(
            Long userId
    );

    Optional<DiaryAutoCompletionNotice>
    findByIdAndUserId(
            Long noticeId,
            Long userId
    );
}
