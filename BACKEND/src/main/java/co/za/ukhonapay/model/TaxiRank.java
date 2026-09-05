package co.za.ukhonapay.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "taxi_ranks")
public class TaxiRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Uniqueness is case-insensitive, enforced by a LOWER(name) index rather
    // than a plain unique column - see schema.sql.
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
