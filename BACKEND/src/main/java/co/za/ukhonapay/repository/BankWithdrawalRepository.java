package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface BankWithdrawalRepository extends JpaRepository<BankWithdrawal, Long> {
    List<BankWithdrawal> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<BankWithdrawal> findByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from BankWithdrawal w where w.reference = :reference")
    Optional<BankWithdrawal> findByReferenceForUpdate(String reference);

    boolean existsByUserIdAndStatus(Long userId, BankWithdrawalStatus status);
}