package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTransactionId(Long transactionId);

    @Query("""
        select coalesce(sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end), 0)
          from LedgerEntry e
         where e.account.accountCode = :accountCode
        """)
    BigDecimal getSignedBalanceByAccountCode(@Param("accountCode") String accountCode);
}
