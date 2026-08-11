package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "diary_reward",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_diary_reward_diary",
                        columnNames = "diary_id"
                )
        }
)
public class DiaryReward extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "diary_id",
            nullable = false
    )
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "generation_status",
            nullable = false,
            length = 20
    )
    private RewardGenerationStatus
            generationStatus;

    @Column(
            name = "color_hex",
            length = 7
    )
    private String colorHex;

    @Column(
            name = "keyword_1",
            length = 20
    )
    private String keyword1;

    @Column(
            name = "keyword_2",
            length = 20
    )
    private String keyword2;

    @Column(
            name = "keyword_3",
            length = 20
    )
    private String keyword3;

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;

    public static DiaryReward createPending(
            Diary diary
    ) {
        if (diary == null) {
            throw new IllegalArgumentException(
                    "보상에 연결할 일기는 필수입니다."
            );
        }

        return DiaryReward.builder()
                .version(0L)
                .diary(diary)
                .generationStatus(
                        RewardGenerationStatus.PENDING
                )
                .build();
    }

    public void complete(
            String colorHex,
            List<String> keywords
    ) {
        String normalizedColorHex =
                DiaryRewardPolicy
                        .normalizeColorHex(
                                colorHex
                        );

        List<String> normalizedKeywords =
                DiaryRewardPolicy
                        .normalizeKeywords(
                                keywords
                        );

        this.generationStatus =
                RewardGenerationStatus.COMPLETED;

        this.colorHex =
                normalizedColorHex;

        this.keyword1 =
                normalizedKeywords.get(0);

        this.keyword2 =
                normalizedKeywords.size() >= 2
                        ? normalizedKeywords.get(1)
                        : null;

        this.keyword3 =
                normalizedKeywords.size() >= 3
                        ? normalizedKeywords.get(2)
                        : null;

        this.failureReason = null;
    }

    public void fail(
            String failureReason
    ) {
        this.generationStatus =
                RewardGenerationStatus.FAILED;

        this.colorHex = null;

        clearKeywords();

        this.failureReason =
                trimToLength(
                        failureReason,
                        500
                );
    }

    /**
     * DB에서는 최대 세 개의 단순 컬럼으로 저장하지만
     * 서비스/API 계층에는 List 형태로 노출함.
     * 따라서 프론트엔드는 keyword1/2/3 구조를
     * 알 필요가 없음.
     */
    public List<String> getKeywords() {
        return Stream.of(
                        keyword1,
                        keyword2,
                        keyword3
                )
                .filter(keyword ->
                        keyword != null
                                && !keyword.isBlank()
                )
                .toList();
    }

    private void clearKeywords() {
        this.keyword1 = null;
        this.keyword2 = null;
        this.keyword3 = null;
    }

    private static String trimToLength(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.length() <= maxLength
                ? trimmed
                : trimmed.substring(
                0,
                maxLength
        );
    }
}