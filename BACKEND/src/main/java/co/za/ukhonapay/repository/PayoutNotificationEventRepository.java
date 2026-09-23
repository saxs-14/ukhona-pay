package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.PayoutNotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PayoutNotificationEventRepository extends JpaRepository<PayoutNotificationEvent, Long> {
    Optional<PayoutNotificationEvent> findByProviderAndPayoutReferenceAndStatusAndSubStatus(
            String provider, String payoutReference, Integer status, Integer subStatus);
}