package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_withdrawals")
public class BankWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankWithdrawalStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = BankWithdrawalStatus.COMPLETED;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BankWithdrawalStatus getStatus() { return status; }
    public void setStatus(BankWithdrawalStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final BankWithdrawal withdrawal = new BankWithdrawal();
        public Builder userId(Long v) { withdrawal.userId = v; return this; }
        public Builder bankAccountId(Long v) { withdrawal.bankAccountId = v; return this; }
        public Builder reference(String v) { withdrawal.reference = v; return this; }
        public Builder amount(BigDecimal v) { withdrawal.amount = v; return this; }
        public Builder status(BankWithdrawalStatus v) { withdrawal.status = v; return this; }
        public BankWithdrawal build() { return withdrawal; }
    }
}
