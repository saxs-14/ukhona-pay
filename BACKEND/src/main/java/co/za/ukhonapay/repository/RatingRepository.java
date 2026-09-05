package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    boolean existsByVendorIdAndTransactionId(Long vendorId, Long transactionId);
}
