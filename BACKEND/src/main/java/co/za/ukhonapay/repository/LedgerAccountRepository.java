package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByAccountCode(String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LedgerAccount> findWithLockByAccountCode(String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LedgerAccount> findWithLockByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LedgerAccount> findWithLockByAssociationId(Long associationId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO ledger_accounts
            (account_code, account_type, user_id, association_id, currency, active, created_at)
        VALUES
            (:code, :type, :userId, :associationId, 'ZAR', TRUE, now())
        ON CONFLICT (account_code) DO NOTHING
        """, nativeQuery = true)
    int ensureAccount(@Param("code") String code,
                      @Param("type") String type,
                      @Param("userId") Long userId,
                      @Param("associationId") Long associationId);
}
