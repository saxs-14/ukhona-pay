package co.za.ukhonapay.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="payment_intents")
public class PaymentIntent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="internal_reference",nullable=false,unique=true,length=40) private String internalReference;
 @Column(nullable=false,length=40) private String provider;
 @Column(name="provider_payment_reference",length=120) private String providerPaymentReference;
 @Column(name="provider_reference",unique=true,length=120) private String providerReference;
 @Column(name="idempotency_key",nullable=false,unique=true,length=120) private String idempotencyKey;
 @Column(name="vendor_id",nullable=false) private Long vendorId;
 @Column(name="payer_user_id") private Long payerUserId;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Column(nullable=false,length=3) private String currency="ZAR";
 @Column(nullable=false,length=20) private String status="PENDING";
 @Column(name="refunded_amount",nullable=false,precision=12,scale=2) private BigDecimal refundedAmount=BigDecimal.ZERO;
 @Column(name="failure_reason",length=255) private String failureReason;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(status==null)status="PENDING";if(currency==null)currency="ZAR";}
 public Long getId(){return id;} public String getInternalReference(){return internalReference;} public void setInternalReference(String v){internalReference=v;}
 public String getProvider(){return provider;} public void setProvider(String v){provider=v;} public String getProviderPaymentReference(){return providerPaymentReference;} public void setProviderPaymentReference(String v){providerPaymentReference=v;} public String getProviderReference(){return providerReference;} public void setProviderReference(String v){providerReference=v;}
 public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;} public Long getVendorId(){return vendorId;} public void setVendorId(Long v){vendorId=v;}
 public Long getPayerUserId(){return payerUserId;} public void setPayerUserId(Long v){payerUserId=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public BigDecimal getRefundedAmount(){return refundedAmount;} public void setRefundedAmount(BigDecimal v){refundedAmount=v;}
 public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;} public LocalDateTime getCreatedAt(){return createdAt;}
 public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;}
}