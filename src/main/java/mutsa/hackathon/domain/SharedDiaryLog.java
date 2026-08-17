package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "shared_diary_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shared_diary_receiver_share",
                        columnNames = {"receiver_id", "diary_share_id"}
                )
        }
)
public class SharedDiaryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private AppUser receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_share_id", nullable = false)
    private DiaryShare diaryShare;

    @Column(name = "used_credit", nullable = false)
    private int usedCredit;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "feedback_summary", length = 1000)
    private String feedbackSummary;

    @Column(name = "feedback_submitted_at")
    private LocalDateTime feedbackSubmittedAt;

    public static SharedDiaryLog create(
            AppUser receiver,
            DiaryShare diaryShare,
            int usedCredit
    ) {
        if (receiver == null || diaryShare == null) {
            throw new IllegalArgumentException("수신자와 공유 일기는 필수입니다.");
        }
        if (diaryShare.getShareStatus() != DiaryShareStatus.APPROVED) {
            throw new IllegalStateException("승인된 공유 일기만 전달할 수 있습니다.");
        }
        if (diaryShare.getDiary().getUser().getId().equals(receiver.getId())) {
            throw new IllegalStateException("자신의 일기는 수신할 수 없습니다.");
        }
        if (usedCredit <= 0) {
            throw new IllegalArgumentException("사용 크레딧은 1 이상이어야 합니다.");
        }

        return SharedDiaryLog.builder()
                .version(0L)
                .receiver(receiver)
                .diaryShare(diaryShare)
                .usedCredit(usedCredit)
                .build();
    }

    public void markAsRead() {
        if (readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public void recordFeedbackSummary(String feedbackSummary) {
        if (this.feedbackSubmittedAt != null) {
            throw new IllegalStateException("Feedback has already been submitted.");
        }
        if (feedbackSummary == null || feedbackSummary.isBlank()) {
            throw new IllegalArgumentException("Feedback is required.");
        }

        this.feedbackSummary = feedbackSummary.trim();
        this.feedbackSubmittedAt = LocalDateTime.now();
    }

    public boolean hasFeedbackSubmitted() {
        return feedbackSubmittedAt != null;
    }
}
