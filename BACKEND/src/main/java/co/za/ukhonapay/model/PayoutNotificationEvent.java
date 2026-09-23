package co.za.ukhonapay.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_notification_events")
public class PayoutNotificationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40) private String provider;
    @Column(name = "payout_reference", nullable = false, length = 120) private String payoutReference;
    @Column(name = "merchant_reference", nullable = false, length = 30) private String merchantReference;
    @Column(nullable = false) private Integer status;
    @Column(name = "sub_status", nullable = false) private Integer subStatus;
    @Column(name = "hash_verified", nullable = false) private boolean hashVerified;
    @Column(name = "processing_status", nullable = false, length = 20) private String processingStatus;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(name = "received_at", nullable = false, updatable = false) private LocalDateTime receivedAt;
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "error_message", length = 500) private String errorMessage;

    @PrePersist void onCreate() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
        if (processingStatus == null) processingStatus = "RECEIVED";
    }

    public Long getId(){return id;}
    public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
    public String getPayoutReference(){return payoutReference;} public void setPayoutReference(String v){payoutReference=v;}
    public String getMerchantReference(){return merchantReference;} public void setMerchantReference(String v){merchantReference=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public Integer getSubStatus(){return subStatus;} public void setSubStatus(Integer v){subStatus=v;}
    public boolean isHashVerified(){return hashVerified;} public void setHashVerified(boolean v){hashVerified=v;}
    public String getProcessingStatus(){return processingStatus;} public void setProcessingStatus(String v){processingStatus=v;}
    public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
    public LocalDateTime getReceivedAt(){return receivedAt;}
    public LocalDateTime getProcessedAt(){return processedAt;} public void setProcessedAt(LocalDateTime v){processedAt=v;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
}