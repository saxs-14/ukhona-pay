package co.za.ukhonapay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payment_refunds")
public class PaymentRefund {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="provider_refund_reference",nullable=false,unique=true,length=120)
    private String providerRefundReference;
    @Column(name="provider_transaction_reference",nullable=false,length=120)
    private String providerTransactionReference;
    @Column(name="payment_intent_id",nullable=false)
    private Long paymentIntentId;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal amount;
    @Column(nullable=false,length=3)
    private String currency="ZAR";
    @Column(nullable=false,length=20)
    private String status="PENDING";
    @Column(length=255)
    private String reason;
    @Column(name="created_at",nullable=false,updatable=false)
    private LocalDateTime createdAt;
    @Column(name="completed_at")
    private LocalDateTime completedAt;

    @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();}

    public Long getId(){return id;}
    public String getProviderRefundReference(){return providerRefundReference;}
    public void setProviderRefundReference(String v){providerRefundReference=v;}
    public String getProviderTransactionReference(){return providerTransactionReference;}
    public void setProviderTransactionReference(String v){providerTransactionReference=v;}
    public Long getPaymentIntentId(){return paymentIntentId;}
    public void setPaymentIntentId(Long v){paymentIntentId=v;}
    public BigDecimal getAmount(){return amount;}
    public void setAmount(BigDecimal v){amount=v;}
    public String getCurrency(){return currency;}
    public void setCurrency(String v){currency=v;}
    public String getStatus(){return status;}
    public void setStatus(String v){status=v;}
    public String getReason(){return reason;}
    public void setReason(String v){reason=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getCompletedAt(){return completedAt;}
    public void setCompletedAt(LocalDateTime v){completedAt=v;}
}
