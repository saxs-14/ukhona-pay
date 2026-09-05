package co.za.ukhonapay.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false)
    private short stars;

    @Column(length = 500)
    private String review;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public short getStars() { return stars; }
    public void setStars(short stars) { this.stars = stars; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Rating rating = new Rating();
        public Builder vendorId(Long v) { rating.vendorId = v; return this; }
        public Builder reviewerId(Long v) { rating.reviewerId = v; return this; }
        public Builder transactionId(Long v) { rating.transactionId = v; return this; }
        public Builder stars(short v) { rating.stars = v; return this; }
        public Builder review(String v) { rating.review = v; return this; }
        public Rating build() { return rating; }
    }
}
