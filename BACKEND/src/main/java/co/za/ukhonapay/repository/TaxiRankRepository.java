package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.TaxiRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaxiRankRepository extends JpaRepository<TaxiRank, Long> {
    List<TaxiRank> findAllByOrderByName();
    Optional<TaxiRank> findByNameIgnoreCase(String name);

    // Same race-safe "create if absent" pattern as TaxiAssociationRepository.
    @Modifying
    @Query(value = "INSERT INTO taxi_ranks (name, location_name) VALUES (:name, :locationName) ON CONFLICT ((lower(name))) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("name") String name, @Param("locationName") String locationName);
}
