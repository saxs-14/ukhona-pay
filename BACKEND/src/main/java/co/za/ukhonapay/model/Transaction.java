package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.TransactionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "cashback_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashbackAmount;

    @Column(name = "cashback_rate", nullable = false, precision = 4, scale = 3)
    private BigDecimal cashbackRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.COMPLETED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCashbackAmount() { return cashbackAmount; }
    public void setCashbackAmount(BigDecimal cashbackAmount) { this.cashbackAmount = cashbackAmount; }
    public BigDecimal getCashbackRate() { return cashbackRate; }
    public void setCashbackRate(BigDecimal cashbackRate) { this.cashbackRate = cashbackRate; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Transaction transaction = new Transaction();
        public Builder reference(String v) { transaction.reference = v; return this; }
        public Builder senderId(Long v) { transaction.senderId = v; return this; }
        public Builder receiverId(Long v) { transaction.receiverId = v; return this; }
        public Builder vendorId(Long v) { transaction.vendorId = v; return this; }
        public Builder amount(BigDecimal v) { transaction.amount = v; return this; }
        public Builder cashbackAmount(BigDecimal v) { transaction.cashbackAmount = v; return this; }
        public Builder cashbackRate(BigDecimal v) { transaction.cashbackRate = v; return this; }
        public Builder status(TransactionStatus v) { transaction.status = v; return this; }
        public Builder description(String v) { transaction.description = v; return this; }
        public Transaction build() { return transaction; }
    }
}
