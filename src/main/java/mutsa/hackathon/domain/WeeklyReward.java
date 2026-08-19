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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "weekly_reward",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_reward_user_week",
                        columnNames = {"user_id", "week_start_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_weekly_reward_user_week",
                        columnList = "user_id, week_start_date"
                ),
                @Index(
                        name = "idx_weekly_reward_week_status",
                        columnList = "week_start_date, generation_status"
                )
        }
)
public class WeeklyReward extends BaseEntity {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_SUMMARY_LENGTH = 1000;
    private static final int MAX_KEYWORD_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "week_start_date", nullable = false, updatable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false, updatable = false)
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 20)
    private WeeklyRewardStatus generationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_source", length = 20)
    private WeeklyRewardImageSource imageSource;

    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Column(length = MAX_SUMMARY_LENGTH)
    private String summary;

    @Column(name = "category_keyword", length = MAX_KEYWORD_LENGTH)
    private String categoryKeyword;

    @Column(name = "keyword_1", length = MAX_KEYWORD_LENGTH)
    private String keyword1;

    @Column(name = "keyword_2", length = MAX_KEYWORD_LENGTH)
    private String keyword2;

    @Column(name = "keyword_3", length = MAX_KEYWORD_LENGTH)
    private String keyword3;

    @Column(name = "keyword_4", length = MAX_KEYWORD_LENGTH)
    private String keyword4;

    @Column(name = "keyword_5", length = MAX_KEYWORD_LENGTH)
    private String keyword5;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "generation_started_at")
    private LocalDateTime generationStartedAt;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    public static WeeklyReward createPending(
            AppUser user,
            LocalDate weekStartDate,
            LocalDate weekEndDate
    ) {
        if (user == null) {
            throw new IllegalArgumentException("주간 보상 사용자는 필수입니다.");
        }
        validateWeek(weekStartDate, weekEndDate);

        return WeeklyReward.builder()
                .version(0L)
                .user(user)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .generationStatus(WeeklyRewardStatus.PENDING)
                .attemptCount(0)
                .build();
    }

    public boolean claimGeneration(
            LocalDateTime now,
            int maxAttempts,
            Duration staleAfter
    ) {
        if (now == null || staleAfter == null || staleAfter.isNegative() || staleAfter.isZero()) {
            throw new IllegalArgumentException("생성 시각과 작업 만료 시간은 필수입니다.");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("최대 생성 시도 횟수는 1 이상이어야 합니다.");
        }
        if (generationStatus == WeeklyRewardStatus.COMPLETED) {
            return false;
        }
        if (attemptCount >= maxAttempts) {
            return false;
        }
        if (
                generationStatus == WeeklyRewardStatus.GENERATING
                        && generationStartedAt != null
                        && generationStartedAt.isAfter(now.minus(staleAfter))
        ) {
            return false;
        }

        generationStatus = WeeklyRewardStatus.GENERATING;
        generationStartedAt = now;
        attemptCount++;
        failureReason = null;
        return true;
    }

    public void complete(
            String title,
            String summary,
            String categoryKeyword,
            List<String> keywords,
            String imageKey,
            String imageContentType,
            WeeklyRewardImageSource imageSource,
            LocalDateTime generatedAt
    ) {
        if (generationStatus != WeeklyRewardStatus.GENERATING) {
            throw new IllegalStateException("생성 중인 주간 보상만 완료할 수 있습니다.");
        }

        this.title = normalizeRequired(title, MAX_TITLE_LENGTH, "주간 보상 제목은 필수입니다.");
        this.summary = normalizeRequired(summary, MAX_SUMMARY_LENGTH, "주간 보상 설명은 필수입니다.");
        this.categoryKeyword = normalizeRequired(
                categoryKeyword,
                MAX_KEYWORD_LENGTH,
                "주간 이미지 카테고리 키워드는 필수입니다."
        );
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        this.keyword1 = normalizedKeywords.get(0);
        this.keyword2 = normalizedKeywords.get(1);
        this.keyword3 = normalizedKeywords.get(2);
        this.keyword4 = normalizedKeywords.size() > 3 ? normalizedKeywords.get(3) : null;
        this.keyword5 = normalizedKeywords.size() > 4 ? normalizedKeywords.get(4) : null;
        this.imageKey = normalizeRequired(imageKey, 500, "주간 보상 이미지 키는 필수입니다.");
        this.imageContentType = normalizeRequired(
                imageContentType,
                100,
                "주간 보상 이미지 형식은 필수입니다."
        );
        if (imageSource == null || generatedAt == null) {
            throw new IllegalArgumentException("이미지 출처와 생성 완료 시각은 필수입니다.");
        }
        this.imageSource = imageSource;
        this.generatedAt = generatedAt;
        this.generationStatus = WeeklyRewardStatus.COMPLETED;
        this.generationStartedAt = null;
        this.failureReason = null;
    }

    public void fail(String reason) {
        if (generationStatus == WeeklyRewardStatus.COMPLETED) {
            return;
        }
        generationStatus = WeeklyRewardStatus.FAILED;
        generationStartedAt = null;
        failureReason = trimToLength(reason, 500);
    }

    public void markViewed() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }

    public boolean isViewed() {
        return viewedAt != null;
    }

    public List<String> getKeywords() {
        return java.util.stream.Stream.of(
                        keyword1,
                        keyword2,
                        keyword3,
                        keyword4,
                        keyword5
                )
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private static void validateWeek(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("주간 보상의 시작일과 종료일은 필수입니다.");
        }
        if (start.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("주간 보상 시작일은 월요일이어야 합니다.");
        }
        if (!end.equals(start.plusDays(6))) {
            throw new IllegalArgumentException("주간 보상 종료일은 시작일로부터 6일 뒤여야 합니다.");
        }
    }

    private static List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            throw new IllegalArgumentException("주간 키워드는 필수입니다.");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String value = keyword.trim().replaceFirst("^#", "");
            if (value.length() > MAX_KEYWORD_LENGTH) {
                value = value.substring(0, MAX_KEYWORD_LENGTH);
            }
            if (!value.isBlank()) {
                normalized.add(value);
            }
            if (normalized.size() == 5) {
                break;
            }
        }
        if (normalized.size() < 3) {
            throw new IllegalArgumentException("주간 하단 키워드는 3개 이상 필요합니다.");
        }
        return List.copyOf(normalized);
    }

    private static String normalizeRequired(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }
}
