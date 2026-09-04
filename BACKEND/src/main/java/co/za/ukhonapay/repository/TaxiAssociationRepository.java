package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.TaxiAssociation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxiAssociationRepository extends JpaRepository<TaxiAssociation, Long> {
    List<TaxiAssociation> findByNameContainingIgnoreCaseOrderByName(String name);
    List<TaxiAssociation> findAllByOrderByName();
    Optional<TaxiAssociation> findByNameIgnoreCase(String name);
}
