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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "weekly_reward_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_reward_entry_diary",
                        columnNames = {"weekly_reward_id", "diary_id"}
                ),
                @UniqueConstraint(
                        name = "uk_weekly_reward_entry_date",
                        columnNames = {"weekly_reward_id", "recorded_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_weekly_reward_entry_reward_date",
                        columnList = "weekly_reward_id, recorded_date"
                )
        }
)
public class WeeklyRewardEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_reward_id", nullable = false)
    private WeeklyReward weeklyReward;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_source", nullable = false, length = 20)
    private WeeklyEntryColorSource colorSource;

    @Column(name = "keyword_1", length = 20)
    private String keyword1;

    @Column(name = "keyword_2", length = 20)
    private String keyword2;

    @Column(name = "keyword_3", length = 20)
    private String keyword3;

    public static WeeklyRewardEntry from(
            WeeklyReward weeklyReward,
            DiaryReward dailyReward
    ) {
        if (weeklyReward == null || dailyReward == null || dailyReward.getDiary() == null) {
            throw new IllegalArgumentException("주간 보상과 일간 보상은 필수입니다.");
        }
        if (dailyReward.getGenerationStatus() != RewardGenerationStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 일간 색 보상만 주간 보상에 포함할 수 있습니다.");
        }
        if (
                dailyReward.getDiary().getRecordedDate().isBefore(weeklyReward.getWeekStartDate())
                        || dailyReward.getDiary().getRecordedDate().isAfter(weeklyReward.getWeekEndDate())
        ) {
            throw new IllegalArgumentException("일기가 주간 보상 기간에 포함되지 않습니다.");
        }

        List<String> keywords = dailyReward.getKeywords();
        return WeeklyRewardEntry.builder()
                .weeklyReward(weeklyReward)
                .diary(dailyReward.getDiary())
                .recordedDate(dailyReward.getDiary().getRecordedDate())
                .colorHex(DiaryRewardPolicy.normalizeColorHex(dailyReward.getColorHex()))
                .colorSource(WeeklyEntryColorSource.DAILY)
                .keyword1(keywords.isEmpty() ? null : keywords.get(0))
                .keyword2(keywords.size() > 1 ? keywords.get(1) : null)
                .keyword3(keywords.size() > 2 ? keywords.get(2) : null)
                .build();
    }

    public static WeeklyRewardEntry fallback(
            WeeklyReward weeklyReward,
            Diary diary,
            String colorHex
    ) {
        if (weeklyReward == null || diary == null) {
            throw new IllegalArgumentException("주간 보상과 일기는 필수입니다.");
        }
        if (
                diary.getRecordedDate().isBefore(weeklyReward.getWeekStartDate())
                        || diary.getRecordedDate().isAfter(weeklyReward.getWeekEndDate())
        ) {
            throw new IllegalArgumentException("일기가 주간 보상 기간에 포함되지 않습니다.");
        }
        return WeeklyRewardEntry.builder()
                .weeklyReward(weeklyReward)
                .diary(diary)
                .recordedDate(diary.getRecordedDate())
                .colorHex(DiaryRewardPolicy.normalizeColorHex(colorHex))
                .colorSource(WeeklyEntryColorSource.FALLBACK)
                .keyword1("기록")
                .build();
    }

    public List<String> getKeywords() {
        return Stream.of(keyword1, keyword2, keyword3)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }
}