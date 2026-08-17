package mutsa.hackathon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A private inbox notification. It deliberately contains no experience-fragment body;
 * the body is exposed only after the receiver spends credit to receive it.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "experience_fragment_arrival",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_experience_fragment_arrival_receiver_share",
                        columnNames = {"receiver_id", "diary_share_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_experience_fragment_arrival_receiver_status_created",
                        columnList = "receiver_id, status, created_at"
                )
        }
)
public class ExperienceFragmentArrival extends BaseEntity {

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
    @JoinColumn(name = "query_diary_id", nullable = false)
    private Diary queryDiary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_share_id", nullable = false)
    private DiaryShare diaryShare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperienceFragmentArrivalStatus status;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public static ExperienceFragmentArrival pending(
            AppUser receiver,
            Diary queryDiary,
            DiaryShare diaryShare
    ) {
        if (receiver == null || queryDiary == null || diaryShare == null) {
            throw new IllegalArgumentException("Arrival receiver, diary, and share are required.");
        }

        return ExperienceFragmentArrival.builder()
                .version(0L)
                .receiver(receiver)
                .queryDiary(queryDiary)
                .diaryShare(diaryShare)
                .status(ExperienceFragmentArrivalStatus.PENDING)
                .build();
    }

    public void markReceived() {
        if (status != ExperienceFragmentArrivalStatus.PENDING) {
            throw new IllegalStateException("This experience fragment has already been received.");
        }
        this.status = ExperienceFragmentArrivalStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }
}
