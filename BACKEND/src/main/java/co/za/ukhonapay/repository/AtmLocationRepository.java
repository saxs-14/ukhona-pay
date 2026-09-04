package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.AtmLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtmLocationRepository extends JpaRepository<AtmLocation, Long> {
}
