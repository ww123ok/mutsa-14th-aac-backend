package mutsa.hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "user_memory_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name =
                                "uk_user_memory_user_content_hash",
                        columnNames = {
                                "user_id",
                                "content_hash"
                        }
                )
        },
        indexes = {
                @Index(
                        name =
                                "idx_user_memory_user_status",
                        columnList = "user_id, status"
                ),
                @Index(
                        name =
                                "idx_user_memory_diary_status",
                        columnList =
                                "source_diary_id, status"
                )
        }
)
public class UserMemoryItem extends BaseEntity {

    private static final int MEMORY_TEXT_MAX_LENGTH =
            500;

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private AppUser user;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "source_diary_id",
            nullable = false
    )
    private Diary sourceDiary;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private UserMemoryCategory category;

    /**
     * 원본 일기 내용이 아니라,
     * AI가 안전하게 일반화한 한 문장만 저장
     */
    @Column(
            name = "memory_text",
            nullable = false,
            length = MEMORY_TEXT_MAX_LENGTH
    )
    private String memoryText;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private UserMemoryStatus status;

    /**
     * 사용자별 동일 기억의 중복 생성을 막는
     * SHA-256 해시
     */
    @Column(
            name = "content_hash",
            nullable = false,
            length = 64
    )
    private String contentHash;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * ONGOING_TOPIC처럼 일시적인 기억의 만료 시각.
     * PET, INTEREST 등 안정적인 기억은 null.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public static UserMemoryItem createCandidate(
            AppUser user,
            Diary sourceDiary,
            UserMemoryCategory category,
            String memoryText,
            String contentHash,
            LocalDateTime expiresAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "기억 대상 사용자는 필수입니다."
            );
        }

        if (sourceDiary == null) {
            throw new IllegalArgumentException(
                    "기억 후보의 출처 일기는 필수입니다."
            );
        }

        if (
                user.getId() == null
                        || sourceDiary.getUser().getId() == null
                        || !Objects.equals(
                        user.getId(),
                        sourceDiary.getUser().getId()
                )
        ) {
            throw new IllegalArgumentException(
                    "사용자와 출처 일기의 소유자가 일치하지 않습니다."
            );
        }

        if (category == null) {
            throw new IllegalArgumentException(
                    "기억 분류는 필수입니다."
            );
        }

        String normalizedMemoryText =
                normalizeRequired(
                        memoryText,
                        "기억 내용은 필수입니다."
                );

        if (
                normalizedMemoryText.length()
                        > MEMORY_TEXT_MAX_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "기억 내용은 500자 이하여야 합니다."
            );
        }

        String normalizedHash =
                normalizeRequired(
                        contentHash,
                        "기억 중복 확인 해시는 필수입니다."
                );

        if (normalizedHash.length() != 64) {
            throw new IllegalArgumentException(
                    "기억 중복 확인 해시는 SHA-256 형식이어야 합니다."
            );
        }

        return UserMemoryItem.builder()
                .version(0L)
                .user(user)
                .sourceDiary(sourceDiary)
                .category(category)
                .memoryText(normalizedMemoryText)
                .status(UserMemoryStatus.PENDING)
                .contentHash(normalizedHash)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 사용자가 해당 기억을 향후 질문에 활용하도록 승인
     */
    public void approve() {
        if (status == UserMemoryStatus.APPROVED) {
            return;
        }

        requirePending();

        this.status = UserMemoryStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.rejectedAt = null;
        this.revokedAt = null;
    }

    /**
     * 사용자가 해당 기억의 저장을 거절
     */
    public void reject() {
        if (status == UserMemoryStatus.REJECTED) {
            return;
        }

        requirePending();

        this.status = UserMemoryStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.approvedAt = null;
        this.revokedAt = null;
    }

    /**
     * 전역 동의 철회 또는 출처 일기 삭제 시
     * 더 이상 기억을 사용하지 못하게 함.
     */
    public void revoke() {
        if (
                status == UserMemoryStatus.REVOKED
                        || status == UserMemoryStatus.REJECTED
        ) {
            return;
        }

        this.status = UserMemoryStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * 실제 AI 질문 생성에 사용했을 때 호출
     */
    public void markUsed() {
        LocalDateTime now = LocalDateTime.now();

        if (status != UserMemoryStatus.APPROVED) {
            throw new IllegalStateException(
                    "승인된 기억만 질문 생성에 사용할 수 있습니다."
            );
        }

        if (isExpired(now)) {
            throw new IllegalStateException(
                    "만료된 기억은 질문 생성에 사용할 수 없습니다."
            );
        }

        this.lastUsedAt = now;
    }

    public boolean isExpired(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "만료 여부 확인 시각은 필수입니다."
            );
        }

        return expiresAt != null
                && !expiresAt.isAfter(now);
    }

    private void requirePending() {
        if (status != UserMemoryStatus.PENDING) {
            throw new IllegalStateException(
                    "이미 검토가 완료된 기억 후보입니다."
            );
        }
    }

    private static String normalizeRequired(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}