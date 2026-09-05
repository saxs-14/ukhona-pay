package co.za.ukhonapay.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxi_associations")
public class TaxiAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Uniqueness is case-insensitive, enforced by a LOWER(name) index rather
    // than a plain unique column - see schema.sql.
    @Column(nullable = false, length = 150)
    private String name;

    // Once-off membership/registration due - the association sets this for
    // itself and revisits it rarely (e.g. yearly, as costs change).
    @Column(name = "dues_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal duesAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (duesAmount == null) {
            duesAmount = new BigDecimal("250.00");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDuesAmount() { return duesAmount; }
    public void setDuesAmount(BigDecimal duesAmount) { this.duesAmount = duesAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
