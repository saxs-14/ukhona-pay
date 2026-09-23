package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_withdrawals")
public class BankWithdrawal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "bank_account_id", nullable = false) private Long bankAccountId;
    @Column(nullable = false, unique = true, length = 30) private String reference;
    @Column(name = "provider_reference", length = 120) private String providerReference;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BankWithdrawalStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now; updatedAt = now;
        if (status == null) status = BankWithdrawalStatus.PENDING;
    }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId(){return id;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getBankAccountId(){return bankAccountId;} public void setBankAccountId(Long v){bankAccountId=v;}
    public String getReference(){return reference;} public void setReference(String v){reference=v;}
    public String getProviderReference(){return providerReference;} public void setProviderReference(String v){providerReference=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public BankWithdrawalStatus getStatus(){return status;} public void setStatus(BankWithdrawalStatus v){status=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}

    public static Builder builder(){return new Builder();}
    public static final class Builder {
        private final BankWithdrawal withdrawal = new BankWithdrawal();
        public Builder userId(Long v){withdrawal.userId=v;return this;}
        public Builder bankAccountId(Long v){withdrawal.bankAccountId=v;return this;}
        public Builder reference(String v){withdrawal.reference=v;return this;}
        public Builder amount(BigDecimal v){withdrawal.amount=v;return this;}
        public Builder status(BankWithdrawalStatus v){withdrawal.status=v;return this;}
        public BankWithdrawal build(){return withdrawal;}
    }
}