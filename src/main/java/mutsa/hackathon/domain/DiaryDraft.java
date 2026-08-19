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
        name = "diary_draft",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_draft_user_recorded_date",
                        columnNames = {
                                "user_id",
                                "recorded_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_diary_draft_user_recorded_date",
                        columnList = "user_id, recorded_date"
                )
        }
)
public class DiaryDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(
            name = "recorded_date",
            nullable = false,
            updatable = false
    )
    private LocalDate recordedDate;

    @Column(
            name = "personalization_uses_diary_content",
            nullable = false
    )
    private boolean personalizationUsesDiaryContent;

    /**
     * 프론트 편집기가 살아 있음을 나타내는 짧은 lease.
     * 기존 row 호환을 위해 nullable로 두며 null/만료 값은 비활성으로 해석한다.
     */
    @Column(name = "editing_active_until")
    private LocalDateTime editingActiveUntil;

    public static DiaryDraft create(
            AppUser user,
            LocalDate recordedDate,
            String content,
            boolean personalizationUsesDiaryContent
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "임시 저장 사용자는 필수입니다."
            );
        }
        if (recordedDate == null) {
            throw new IllegalArgumentException(
                    "임시 저장 날짜는 필수입니다."
            );
        }

        return DiaryDraft.builder()
                .user(user)
                .recordedDate(recordedDate)
                .content(normalizeContent(content))
                .personalizationUsesDiaryContent(
                        personalizationUsesDiaryContent
                )
                .build();
    }

    public void update(
            String content,
            boolean personalizationUsesDiaryContent
    ) {
        this.content = normalizeContent(content);
        this.personalizationUsesDiaryContent =
                personalizationUsesDiaryContent;
    }

    public boolean shouldUseDiaryContentForPersonalization() {
        return personalizationUsesDiaryContent;
    }

    public void markEditingActiveUntil(
            LocalDateTime activeUntil
    ) {
        if (activeUntil == null) {
            throw new IllegalArgumentException(
                    "작성 활성 만료 시각은 필수입니다."
            );
        }
        this.editingActiveUntil = activeUntil;
    }

    public void stopEditing() {
        this.editingActiveUntil = null;
    }

    public boolean isEditingActiveAt(
            LocalDateTime now
    ) {
        return now != null
                && editingActiveUntil != null
                && editingActiveUntil.isAfter(now);
    }

    private static String normalizeContent(
            String content
    ) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "임시 저장 내용은 필수입니다."
            );
        }
        return content.trim();
    }
}
