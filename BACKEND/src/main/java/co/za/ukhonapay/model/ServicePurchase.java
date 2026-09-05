package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.ServicePurchaseType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// A real, server-recorded prepaid voucher / bill payment - airtime, prepaid
// electricity, or a Pay@ bill. Money leaves the wallet the same way a bank
// withdrawal does (it doesn't credit any other UKHONA PAY wallet - it's
// paying out to a real-world third party), and the voucher/token is
// generated and stored here server-side rather than fabricated in the
// browser, so it survives a page refresh and can be looked up again later.
@Entity
@Table(name = "service_purchases")
public class ServicePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServicePurchaseType type;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // AIRTIME only.
    @Column(length = 20)
    private String network;
    @Column(name = "recipient_phone", length = 15)
    private String recipientPhone;

    // ELECTRICITY only.
    @Column(name = "meter_number", length = 20)
    private String meterNumber;
    @Column(length = 100)
    private String municipality;

    // PAYAT_BILL only.
    @Column(name = "biller_name", length = 100)
    private String billerName;
    @Column(name = "biller_category", length = 60)
    private String billerCategory;
    @Column(name = "payat_reference", length = 30)
    private String payAtReference;
    @Column(name = "account_name", length = 150)
    private String accountName;

    // The simulated voucher/token, generated server-side at purchase time -
    // see ServicePurchaseService. Never a real telecom/utility API call
    // (none is available here), but a real, stable, re-lookupable record.
    @Column(name = "voucher_token", nullable = false, length = 40)
    private String voucherToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ServicePurchaseType getType() { return type; }
    public void setType(ServicePurchaseType type) { this.type = type; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }
    public String getBillerName() { return billerName; }
    public void setBillerName(String billerName) { this.billerName = billerName; }
    public String getBillerCategory() { return billerCategory; }
    public void setBillerCategory(String billerCategory) { this.billerCategory = billerCategory; }
    public String getPayAtReference() { return payAtReference; }
    public void setPayAtReference(String payAtReference) { this.payAtReference = payAtReference; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getVoucherToken() { return voucherToken; }
    public void setVoucherToken(String voucherToken) { this.voucherToken = voucherToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final ServicePurchase p = new ServicePurchase();
        public Builder userId(Long v) { p.userId = v; return this; }
        public Builder type(ServicePurchaseType v) { p.type = v; return this; }
        public Builder reference(String v) { p.reference = v; return this; }
        public Builder amount(BigDecimal v) { p.amount = v; return this; }
        public Builder network(String v) { p.network = v; return this; }
        public Builder recipientPhone(String v) { p.recipientPhone = v; return this; }
        public Builder meterNumber(String v) { p.meterNumber = v; return this; }
        public Builder municipality(String v) { p.municipality = v; return this; }
        public Builder billerName(String v) { p.billerName = v; return this; }
        public Builder billerCategory(String v) { p.billerCategory = v; return this; }
        public Builder payAtReference(String v) { p.payAtReference = v; return this; }
        public Builder accountName(String v) { p.accountName = v; return this; }
        public Builder voucherToken(String v) { p.voucherToken = v; return this; }
        public ServicePurchase build() { return p; }
    }
}
