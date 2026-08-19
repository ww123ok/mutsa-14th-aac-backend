package mutsa.hackathon.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A short-lived, private representation of a diary that may receive one
 * relevant experience fragment. The original diary content is not copied here.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "experience_match_query",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_experience_match_query_diary",
                columnNames = "diary_id"
        ),
        indexes = @Index(
                name = "idx_experience_match_query_active",
                columnList = "expires_at, matched_at"
        )
)
public class ExperienceMatchQuery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private AppUser receiver;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "matching_text", nullable = false, columnDefinition = "TEXT")
    private String matchingText;

    @ElementCollection
    @CollectionTable(
            name = "experience_match_query_keyword",
            joinColumns = @JoinColumn(name = "experience_match_query_id")
    )
    @Column(name = "keyword", nullable = false, length = 50)
    @OrderColumn(name = "keyword_order")
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Column(name = "embedding_json", nullable = false, columnDefinition = "TEXT")
    private String embeddingJson;

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    public static ExperienceMatchQuery waiting(
            AppUser receiver,
            Diary diary,
            String matchingText,
            List<String> keywords,
            String embeddingJson,
            String embeddingModel,
            LocalDateTime expiresAt
    ) {
        if (receiver == null || diary == null || expiresAt == null) {
            throw new IllegalArgumentException("Experience match query data is required.");
        }
        return ExperienceMatchQuery.builder()
                .version(0L)
                .receiver(receiver)
                .diary(diary)
                .matchingText(requireText(matchingText))
                .keywords(normalizeKeywords(keywords))
                .embeddingJson(requireText(embeddingJson))
                .embeddingModel(requireText(embeddingModel))
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isActiveAt(LocalDateTime now) {
        return matchedAt == null && expiresAt.isAfter(now) && !diary.isDeleted();
    }

    public void markMatched() {
        if (matchedAt == null) {
            matchedAt = LocalDateTime.now();
        }
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Experience match query text is required.");
        }
        return value.trim();
    }

    private static List<String> normalizeKeywords(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
    }
}
