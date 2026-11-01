package co.za.ukhonapay.repository;
import co.za.ukhonapay.model.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction,Long>{Optional<LedgerTransaction> findByReference(String reference); Optional<LedgerTransaction> findByExternalReference(String externalReference);}