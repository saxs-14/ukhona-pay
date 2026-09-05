package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.TaxiAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaxiAssociationRepository extends JpaRepository<TaxiAssociation, Long> {
    List<TaxiAssociation> findByNameContainingIgnoreCaseOrderByName(String name);
    List<TaxiAssociation> findAllByOrderByName();
    Optional<TaxiAssociation> findByNameIgnoreCase(String name);

    // No-op if a case-insensitive match already exists - lets two concurrent
    // "create if absent" requests for the same name race safely instead of one
    // throwing a unique-constraint violation (which would otherwise poison the
    // rest of that transaction in Postgres).
    // Returns 1 if this call actually inserted the row, 0 if a match already
    // existed (the caller uses this to decide whether to also create the
    // association's wallet, rather than racing a separate lazy-create later).
    @Modifying
    @Query(value = "INSERT INTO taxi_associations (name) VALUES (:name) ON CONFLICT ((lower(name))) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("name") String name);
}
