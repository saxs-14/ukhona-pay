package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByProviderAndProviderEventId(String provider, String eventId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO payment_webhook_events
            (provider, provider_event_id, event_type, signature_verified, processing_status, payload, received_at)
        VALUES
            (:provider, :eventId, :eventType, :verified, 'RECEIVED', :payload, now())
        ON CONFLICT (provider, provider_event_id) DO NOTHING
        """, nativeQuery = true)
    int claimIfNew(@Param("provider") String provider,
                   @Param("eventId") String eventId,
                   @Param("eventType") String eventType,
                   @Param("verified") boolean verified,
                   @Param("payload") String payload);

    @Modifying
    @Transactional
    @Query("""
        update PaymentWebhookEvent e
           set e.processingStatus = 'RECEIVED',
               e.payload = :payload,
               e.signatureVerified = true,
               e.errorMessage = null,
               e.processedAt = null
         where e.provider = :provider
           and e.providerEventId = :eventId
           and e.processingStatus = 'FAILED'
        """)
    int retryFailed(@Param("provider") String provider,
                    @Param("eventId") String eventId,
                    @Param("payload") String payload);
}
