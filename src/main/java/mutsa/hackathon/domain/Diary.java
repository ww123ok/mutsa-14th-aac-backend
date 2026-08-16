package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "diary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_user_recorded_date",
                        columnNames = {"user_id", "recorded_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_diary_user_recorded_date",
                        columnList = "user_id, recorded_date"
                )
        }
)
public class Diary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "recorded_date", nullable = false, updatable = false)
    private LocalDate recordedDate;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 기존 운영 DB 사용자는 컬럼 추가 직후 null일 수 있으므로
     * nullable 상태를 허용하고 isHidden()에서 false로 해석합니다.
     */
    @Column(name = "is_hidden")
    private Boolean hidden;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "memory_applied_at")
    private LocalDateTime memoryAppliedAt;

    public static Diary create(AppUser user, String content, LocalDate recordedDate) {
        if (user == null) {
            throw new IllegalArgumentException("일기 작성자는 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("일기 내용은 필수입니다.");
        }
        if (recordedDate == null) {
            throw new IllegalArgumentException("일기 작성일은 필수입니다.");
        }

        return Diary.builder()
                .version(0L)
                .user(user)
                .content(content.trim())
                .recordedDate(recordedDate)
                .deleted(false)
                .hidden(false)
                .build();
    }

    public boolean isHidden() {
        return Boolean.TRUE.equals(hidden);
    }

    public void hide() {
        if (deleted || isHidden()) {
            return;
        }
        this.hidden = true;
        this.hiddenAt = LocalDateTime.now();
    }

    public void unhide() {
        if (deleted || !isHidden()) {
            return;
        }
        this.hidden = false;
        this.hiddenAt = null;
    }

    public void softDelete() {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.hidden = false;
        this.hiddenAt = null;
    }

    public void restore() {
        if (!deleted) {
            return;
        }
        this.deleted = false;
        this.deletedAt = null;
        this.hidden = false;
        this.hiddenAt = null;
    }

    public void markMemoryApplied() {
        if (memoryAppliedAt == null) {
            this.memoryAppliedAt = LocalDateTime.now();
        }
    }
}