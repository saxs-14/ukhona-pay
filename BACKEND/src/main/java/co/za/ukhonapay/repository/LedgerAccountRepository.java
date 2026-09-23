package co.za.ukhonapay.repository;
import co.za.ukhonapay.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount,Long>{
 Optional<LedgerAccount> findByAccountCode(String accountCode);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<LedgerAccount> findWithLockByAccountCode(String accountCode);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<LedgerAccount> findWithLockByUserId(Long userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<LedgerAccount> findWithLockByAssociationId(Long associationId);
}