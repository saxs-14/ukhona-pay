package co.za.ukhonapay.repository;
import co.za.ukhonapay.model.PaymentIntent; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType; import java.util.Optional;
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent,Long>{Optional<PaymentIntent> findByInternalReference(String v); @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<PaymentIntent> findByInternalReferenceForUpdate(String v); Optional<PaymentIntent> findByProviderReference(String v); Optional<PaymentIntent> findByIdempotencyKey(String v);}