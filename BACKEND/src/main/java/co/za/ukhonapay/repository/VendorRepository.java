package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.model.enums.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUserId(Long userId);
    Optional<Vendor> findByQrCode(String qrCode);
    List<Vendor> findByCategory(VendorCategory category);
    List<Vendor> findByBusinessNameContainingIgnoreCase(String name);
    List<Vendor> findByAssociationIdAndStatusOrderByCreatedAt(Long associationId, VendorStatus status);
<<<<<<< HEAD
=======
    List<Vendor> findByAssociationIdOrderByCreatedAt(Long associationId);
    List<Vendor> findAllByOrderByCreatedAtDesc();
>>>>>>> origin/main
}
