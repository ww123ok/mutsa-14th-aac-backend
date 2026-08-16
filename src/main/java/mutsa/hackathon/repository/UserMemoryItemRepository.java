package mutsa.hackathon.repository;

import mutsa.hackathon.domain.UserMemoryItem;
import mutsa.hackathon.domain.UserMemoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserMemoryItemRepository
        extends JpaRepository<UserMemoryItem, Long> {

    boolean existsByUserIdAndContentHash(
            Long userId,
            String contentHash
    );

    Optional<UserMemoryItem> findByIdAndUserId(
            Long memoryId,
            Long userId
    );

    List<UserMemoryItem>
    findAllByUserIdAndSourceDiaryIdOrderByIdAsc(
            Long userId,
            Long sourceDiaryId
    );

    List<UserMemoryItem>
    findAllByUserIdAndStatusIn(
            Long userId,
            Collection<UserMemoryStatus> statuses
    );

    List<UserMemoryItem>
    findAllByUserIdAndSourceDiaryIdAndStatusIn(
            Long userId,
            Long sourceDiaryId,
            Collection<UserMemoryStatus> statuses
    );

    /**
     * 개발용 일기 초기화 시 해당 일기에서 파생된
     * 기억 후보를 실제로 삭제
     */
    long deleteAllByUserIdAndSourceDiaryId(
            Long userId,
            Long sourceDiaryId
    );

    /**
     * 승인 상태이면서 아직 만료되지 않은 기억만
     * 질문 생성용으로 조회
     */
    @Query("""
            select memory
            from UserMemoryItem memory
            where memory.user.id = :userId
              and memory.status = :status
              and (
                    memory.expiresAt is null
                    or memory.expiresAt > :now
              )
            order by memory.approvedAt desc, memory.id desc
            """)
    List<UserMemoryItem> findActiveApprovedMemories(
            @Param("userId")
            Long userId,

            @Param("status")
            UserMemoryStatus status,

            @Param("now")
            LocalDateTime now
    );
}
