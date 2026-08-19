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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "diary_comment",
        indexes = {
                @Index(
                        name = "idx_diary_comment_diary_created",
                        columnList = "diary_id, created_at"
                )
        }
)
public class DiaryComment extends BaseEntity {

    private static final int MAX_CONTENT_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @Column(
            nullable = false,
            length = MAX_CONTENT_LENGTH
    )
    private String content;

    public static DiaryComment create(
            Diary diary,
            String content
    ) {
        if (diary == null) {
            throw new IllegalArgumentException(
                    "댓글 대상 일기는 필수입니다."
            );
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "댓글 내용은 필수입니다."
            );
        }

        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "댓글은 2000자 이하로 작성해야 합니다."
            );
        }

        return DiaryComment.builder()
                .diary(diary)
                .content(normalized)
                .build();
    }
}
