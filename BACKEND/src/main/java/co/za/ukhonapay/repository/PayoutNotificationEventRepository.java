package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.PayoutNotificationEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PayoutNotificationEventRepository extends JpaRepository<PayoutNotificationEvent, Long> {
    Optional<PayoutNotificationEvent> findByProviderAndPayoutReferenceAndStatusAndSubStatus(
            String provider, String payoutReference, Integer status, Integer subStatus);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO payout_notification_events
            (provider, payout_reference, merchant_reference, status, sub_status,
             hash_verified, processing_status, payload, received_at)
        VALUES
            (:provider, :payoutReference, :merchantReference, :status, :subStatus,
             TRUE, 'RECEIVED', :payload, now())
        ON CONFLICT (provider, payout_reference, status, sub_status) DO NOTHING
        """, nativeQuery = true)
    int claimIfNew(@Param("provider") String provider,
                   @Param("payoutReference") String payoutReference,
                   @Param("merchantReference") String merchantReference,
                   @Param("status") int status,
                   @Param("subStatus") int subStatus,
                   @Param("payload") String payload);

    @Modifying
    @Transactional
    @Query("""
        update PayoutNotificationEvent e
           set e.processingStatus = 'PROCESSING',
               e.payload = :payload,
               e.hashVerified = true,
               e.errorMessage = null,
               e.processedAt = null
         where e.provider = :provider
           and e.payoutReference = :payoutReference
           and e.status = :status
           and e.subStatus = :subStatus
           and e.processingStatus in ('RECEIVED','FAILED')
        """)
    int claimForProcessing(@Param("provider") String provider,
                           @Param("payoutReference") String payoutReference,
                           @Param("status") int status,
                           @Param("subStatus") int subStatus,
                           @Param("payload") String payload);
}
