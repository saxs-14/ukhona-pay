package co.za.ukhonapay.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "association_id", unique = true)
    private Long associationId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "cashback_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal cashbackBalance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAssociationId() { return associationId; }
    public void setAssociationId(Long associationId) { this.associationId = associationId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getCashbackBalance() { return cashbackBalance; }
    public void setCashbackBalance(BigDecimal cashbackBalance) { this.cashbackBalance = cashbackBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Wallet wallet = new Wallet();
        public Builder userId(Long v) { wallet.userId = v; return this; }
        public Builder associationId(Long v) { wallet.associationId = v; return this; }
        public Builder balance(BigDecimal v) { wallet.balance = v; return this; }
        public Builder cashbackBalance(BigDecimal v) { wallet.cashbackBalance = v; return this; }
        public Builder currency(String v) { wallet.currency = v; return this; }
        public Wallet build() { return wallet; }
    }
}
