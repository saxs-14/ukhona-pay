package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.WithdrawalStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "atm_location_id", nullable = false)
    private Long atmLocationId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "withdrawal_pin", nullable = false, length = 4)
    private String withdrawalPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) {
            status = WithdrawalStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAtmLocationId() { return atmLocationId; }
    public void setAtmLocationId(Long atmLocationId) { this.atmLocationId = atmLocationId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getWithdrawalPin() { return withdrawalPin; }
    public void setWithdrawalPin(String withdrawalPin) { this.withdrawalPin = withdrawalPin; }
    public WithdrawalStatus getStatus() { return status; }
    public void setStatus(WithdrawalStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Withdrawal withdrawal = new Withdrawal();
        public Builder userId(Long v) { withdrawal.userId = v; return this; }
        public Builder atmLocationId(Long v) { withdrawal.atmLocationId = v; return this; }
        public Builder amount(BigDecimal v) { withdrawal.amount = v; return this; }
        public Builder withdrawalPin(String v) { withdrawal.withdrawalPin = v; return this; }
        public Builder status(WithdrawalStatus v) { withdrawal.status = v; return this; }
        public Builder expiresAt(LocalDateTime v) { withdrawal.expiresAt = v; return this; }
        public Withdrawal build() { return withdrawal; }
    }
}
