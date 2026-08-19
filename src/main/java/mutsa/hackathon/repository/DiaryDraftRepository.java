package mutsa.hackathon.repository;

import mutsa.hackathon.domain.DiaryDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryDraftRepository
        extends JpaRepository<DiaryDraft, Long> {

    Optional<DiaryDraft>
    findByUserIdAndRecordedDate(
            Long userId,
            LocalDate recordedDate
    );

    long deleteByUserIdAndRecordedDate(
            Long userId,
            LocalDate recordedDate
    );

    @Query("""
            select draft
            from DiaryDraft draft
            join fetch draft.user
            order by draft.id asc
            """)
    List<DiaryDraft> findAllWithUser();

    @Query("""
            select draft
            from DiaryDraft draft
            join fetch draft.user
            where draft.id = :draftId
            """)
    Optional<DiaryDraft> findByIdWithUser(
            @Param("draftId")
            Long draftId
    );
}
