package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PaymentRefund r where r.providerRefundReference = :reference")
    Optional<PaymentRefund> findByProviderRefundReferenceForUpdate(String reference);
}
