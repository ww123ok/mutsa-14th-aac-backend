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
        name = "credit_transaction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_credit_transaction_reference",
                        columnNames = {
                                "user_id",
                                "transaction_type",
                                "reference_type",
                                "reference_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_credit_transaction_user_created",
                        columnList = "user_id, created_at"
                )
        }
)
public class CreditTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private CreditTransactionType transactionType;

    @Column(nullable = false)
    private int amount;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private CreditReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(length = 500)
    private String description;

    public static CreditTransaction create(
            AppUser user,
            CreditTransactionType transactionType,
            int amount,
            int balanceAfter,
            CreditReferenceType referenceType,
            Long referenceId,
            String description
    ) {
        if (user == null || transactionType == null || referenceType == null) {
            throw new IllegalArgumentException("크레딧 거래 필수값이 누락되었습니다.");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("크레딧 변동량은 0일 수 없습니다.");
        }
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("거래 후 크레딧은 음수가 될 수 없습니다.");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("크레딧 거래 참조 ID는 필수입니다.");
        }

        return CreditTransaction.builder()
                .user(user)
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description == null ? null : description.trim())
                .build();
    }
}