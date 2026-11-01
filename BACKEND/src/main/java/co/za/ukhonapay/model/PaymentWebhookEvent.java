package co.za.ukhonapay.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="payment_webhook_events",uniqueConstraints=@UniqueConstraint(name="uq_payment_webhook_provider_event",columnNames={"provider","provider_event_id"}))
public class PaymentWebhookEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=40) private String provider;
 @Column(name="provider_event_id",nullable=false,length=160) private String providerEventId;
 @Column(name="event_type",nullable=false,length=120) private String eventType;
 @Column(name="signature_verified",nullable=false) private boolean signatureVerified;
 @Column(name="processing_status",nullable=false,length=20) private String processingStatus="RECEIVED";
 @Column(nullable=false,columnDefinition="TEXT") private String payload;
 @Column(name="received_at",nullable=false,updatable=false) private LocalDateTime receivedAt;
 @Column(name="processed_at") private LocalDateTime processedAt;
 @Column(name="error_message",length=500) private String errorMessage;
 @PrePersist void onCreate(){if(receivedAt==null)receivedAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
 public String getProviderEventId(){return providerEventId;} public void setProviderEventId(String v){providerEventId=v;} public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
 public boolean isSignatureVerified(){return signatureVerified;} public void setSignatureVerified(boolean v){signatureVerified=v;} public String getProcessingStatus(){return processingStatus;} public void setProcessingStatus(String v){processingStatus=v;}
 public String getPayload(){return payload;} public void setPayload(String v){payload=v;} public LocalDateTime getReceivedAt(){return receivedAt;} public LocalDateTime getProcessedAt(){return processedAt;} public void setProcessedAt(LocalDateTime v){processedAt=v;}
 public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
}