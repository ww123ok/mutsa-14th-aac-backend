package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 20)
    private RewardGenerationStatus generationStatus;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "color_name", length = 100)
    private String colorName;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public static DiaryReward createPending(Diary diary) {
        if (diary == null) {
            throw new IllegalArgumentException("보상에 연결할 일기는 필수입니다.");
        }

        return DiaryReward.builder()
                .version(0L)
                .diary(diary)
                .generationStatus(RewardGenerationStatus.PENDING)
                .build();
    }

    public void complete(String colorHex, String colorName) {
        if (colorHex == null || !colorHex.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("색상 코드는 #RRGGBB 형식이어야 합니다.");
        }
        if (colorName == null || colorName.isBlank()) {
            throw new IllegalArgumentException("색상 이름은 필수입니다.");
        }

        this.generationStatus = RewardGenerationStatus.COMPLETED;
        this.colorHex = colorHex.toUpperCase();
        this.colorName = colorName.trim();
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        this.generationStatus = RewardGenerationStatus.FAILED;
        this.colorHex = null;
        this.colorName = null;
        this.failureReason = trimToLength(failureReason, 500);
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength
                ? trimmed
                : trimmed.substring(0, maxLength);
    }
}