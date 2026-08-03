package mutsa.hackathon.repository;

import mutsa.hackathon.domain.CreditReferenceType;
import mutsa.hackathon.domain.CreditTransaction;
import mutsa.hackathon.domain.CreditTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditTransactionRepository
        extends JpaRepository<CreditTransaction, Long> {

    boolean existsByUserIdAndTransactionTypeAndReferenceTypeAndReferenceId(
            Long userId,
            CreditTransactionType transactionType,
            CreditReferenceType referenceType,
            Long referenceId
    );
}