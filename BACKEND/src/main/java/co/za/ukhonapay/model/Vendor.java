package co.za.ukhonapay.model;

import co.za.ukhonapay.model.enums.VendorCategory;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorCategory category;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    // Driver-only.
    @Column(name = "vehicle_registration", length = 20)
    private String vehicleRegistration;

    @Column(name = "association_id")
    private Long associationId;

    // Vendor-only.
    @Column(name = "rank_id")
    private Long rankId;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "qr_code", nullable = false, unique = true, length = 64)
    private String qrCode;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "rating_avg", nullable = false, precision = 2, scale = 1)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (ratingAvg == null) {
            ratingAvg = BigDecimal.ZERO;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public VendorCategory getCategory() { return category; }
    public void setCategory(VendorCategory category) { this.category = category; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getVehicleRegistration() { return vehicleRegistration; }
    public void setVehicleRegistration(String vehicleRegistration) { this.vehicleRegistration = vehicleRegistration; }
    public Long getAssociationId() { return associationId; }
    public void setAssociationId(Long associationId) { this.associationId = associationId; }
    public Long getRankId() { return rankId; }
    public void setRankId(Long rankId) { this.rankId = rankId; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Vendor vendor = new Vendor();
        public Builder userId(Long v) { vendor.userId = v; return this; }
        public Builder businessName(String v) { vendor.businessName = v; return this; }
        public Builder category(VendorCategory v) { vendor.category = v; return this; }
        public Builder locationName(String v) { vendor.locationName = v; return this; }
        public Builder vehicleRegistration(String v) { vendor.vehicleRegistration = v; return this; }
        public Builder associationId(Long v) { vendor.associationId = v; return this; }
        public Builder rankId(Long v) { vendor.rankId = v; return this; }
        public Builder latitude(BigDecimal v) { vendor.latitude = v; return this; }
        public Builder longitude(BigDecimal v) { vendor.longitude = v; return this; }
        public Builder qrCode(String v) { vendor.qrCode = v; return this; }
        public Builder verified(boolean v) { vendor.verified = v; return this; }
        public Builder ratingAvg(BigDecimal v) { vendor.ratingAvg = v; return this; }
        public Builder ratingCount(int v) { vendor.ratingCount = v; return this; }
        public Builder photoUrl(String v) { vendor.photoUrl = v; return this; }
        public Vendor build() { return vendor; }
    }
}
