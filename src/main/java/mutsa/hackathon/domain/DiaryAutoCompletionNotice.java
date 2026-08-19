package mutsa.hackathon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "diary_auto_completion_notice",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_auto_completion_notice_diary",
                        columnNames = "diary_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_diary_auto_completion_notice_user_viewed",
                        columnList = "user_id, viewed_at"
                )
        }
)
public class DiaryAutoCompletionNotice
        extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * 일기와 FK를 직접 묶지 않음.
     * 사용자가 자동 완료된 일기를 나중에 영구 삭제하더라도
     * 삭제 생명주기를 불필요하게 결합하지 않기 위함.
     */
    @Column(name = "diary_id", nullable = false)
    private Long diaryId;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "auto_completed_at", nullable = false)
    private LocalDateTime autoCompletedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    public static DiaryAutoCompletionNotice create(
            AppUser user,
            Long diaryId,
            LocalDate recordedDate,
            LocalDateTime autoCompletedAt
    ) {
        if (
                user == null
                        || diaryId == null
                        || recordedDate == null
                        || autoCompletedAt == null
        ) {
            throw new IllegalArgumentException(
                    "자동 완료 안내 정보는 필수입니다."
            );
        }

        return DiaryAutoCompletionNotice.builder()
                .user(user)
                .diaryId(diaryId)
                .recordedDate(recordedDate)
                .autoCompletedAt(autoCompletedAt)
                .build();
    }

    public void markViewed(
            LocalDateTime viewedAt
    ) {
        if (this.viewedAt != null) {
            return;
        }
        if (viewedAt == null) {
            throw new IllegalArgumentException(
                    "자동 완료 안내 확인 시각은 필수입니다."
            );
        }
        this.viewedAt = viewedAt;
    }

    public boolean isViewed() {
        return viewedAt != null;
    }
}
