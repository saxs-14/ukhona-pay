package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.ServicePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicePurchaseRepository extends JpaRepository<ServicePurchase, Long> {
    List<ServicePurchase> findByUserIdOrderByCreatedAtDesc(Long userId);
}
