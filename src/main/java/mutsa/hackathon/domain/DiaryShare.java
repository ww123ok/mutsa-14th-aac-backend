package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "diary_share",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_share_diary",
                        columnNames = "diary_id"
                )
        }
)
public class DiaryShare extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_status", nullable = false, length = 30)
    private DiaryShareStatus shareStatus;

    @Column(name = "anonymized_content", columnDefinition = "TEXT")
    private String anonymizedContent;

    @Column(name = "general_topic", length = 100)
    private String generalTopic;

    @ElementCollection
    @CollectionTable(
            name = "diary_share_keyword",
            joinColumns = @JoinColumn(name = "diary_share_id")
    )
    @Column(name = "keyword", nullable = false, length = 50)
    @OrderColumn(name = "keyword_order")
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Column(name = "matching_text", columnDefinition = "TEXT")
    private String matchingText;

    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "earned_credit", nullable = false)
    private int earnedCredit;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "review_available_at")
    private LocalDateTime reviewAvailableAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    public static DiaryShare request(Diary diary) {
        if (diary == null) {
            throw new IllegalArgumentException("공유할 일기는 필수입니다.");
        }
        if (diary.isDeleted()) {
            throw new IllegalStateException("삭제된 일기는 공유할 수 없습니다.");
        }

        return DiaryShare.builder()
                .version(0L)
                .diary(diary)
                .shareStatus(DiaryShareStatus.REQUESTED)
                .earnedCredit(0)
                .build();
    }

    public void requireReview(
            String anonymizedContent,
            String generalTopic,
            List<String> keywords,
            String matchingText
    ) {
        if (shareStatus != DiaryShareStatus.REQUESTED) {
            throw new IllegalStateException("익명화 요청 상태에서만 검토 대기로 변경할 수 있습니다.");
        }
        if (anonymizedContent == null || anonymizedContent.isBlank()) {
            throw new IllegalArgumentException("익명화된 일기 내용은 필수입니다.");
        }

        this.anonymizedContent = anonymizedContent.trim();
        this.generalTopic = normalizeRequired(generalTopic);
        this.keywords = normalizeKeywords(keywords);
        this.matchingText = normalizeRequired(matchingText);
        this.shareStatus = DiaryShareStatus.REVIEW_REQUIRED;
        this.reviewAvailableAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void approve(
            String embeddingJson,
            String embeddingModel
    ) {
        if (shareStatus != DiaryShareStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("검토 대기 상태에서만 공유를 승인할 수 있습니다.");
        }

        this.embeddingJson = normalizeRequired(embeddingJson);
        this.embeddingModel = normalizeRequired(embeddingModel);
        this.shareStatus = DiaryShareStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (shareStatus != DiaryShareStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("검토 대기 상태에서만 공유를 거절할 수 있습니다.");
        }

        this.shareStatus = DiaryShareStatus.REJECTED;
        this.rejectionReason = reason == null ? null : reason.trim();
    }

    public void markRewarded(int earnedCredit) {
        if (shareStatus != DiaryShareStatus.APPROVED) {
            throw new IllegalStateException("승인된 공유만 크레딧을 지급할 수 있습니다.");
        }
        if (rewardedAt != null) {
            throw new IllegalStateException("이미 공유 보상 크레딧이 지급되었습니다.");
        }
        if (earnedCredit <= 0) {
            throw new IllegalArgumentException("공유 보상 크레딧은 1 이상이어야 합니다.");
        }

        this.earnedCredit = earnedCredit;
        this.rewardedAt = LocalDateTime.now();
    }

    public void withdraw() {
        if (shareStatus != DiaryShareStatus.APPROVED
                && shareStatus != DiaryShareStatus.REVIEW_REQUIRED) {
            throw new IllegalStateException("검토 중이거나 승인된 공유만 철회할 수 있습니다.");
        }
        this.shareStatus = DiaryShareStatus.WITHDRAWN;
    }

    public void block(String reason) {
        if (shareStatus != DiaryShareStatus.REQUESTED) {
            throw new IllegalStateException("Only a requested share can be blocked.");
        }
        this.shareStatus = DiaryShareStatus.BLOCKED;
        this.rejectionReason = reason == null ? null : reason.trim();
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Experience fragment value is required.");
        }
        return value.trim();
    }

    private List<String> normalizeKeywords(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("At least one keyword is required.");
        }
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one keyword is required.");
        }
        return new ArrayList<>(normalized);
    }
}
