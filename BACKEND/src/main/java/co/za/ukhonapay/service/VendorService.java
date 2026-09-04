package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.VendorResponse;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public List<VendorResponse> search(String category, String name) {
        List<Vendor> vendors;
        if (category != null && !category.isBlank()) {
            vendors = vendorRepository.findByCategory(VendorCategory.valueOf(category.toUpperCase()));
        } else if (name != null && !name.isBlank()) {
            vendors = vendorRepository.findByBusinessNameContainingIgnoreCase(name);
        } else {
            vendors = vendorRepository.findAll();
        }
        return vendors.stream().map(this::toResponse).toList();
    }

    public VendorResponse getByQrCode(String qrCode) {
        Vendor vendor = vendorRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for QR code " + qrCode));
        return toResponse(vendor);
    }

    public VendorResponse getByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new VendorNotFoundException("No vendor profile for user " + userId));
        return toResponse(vendor);
    }

    private VendorResponse toResponse(Vendor v) {
        return new VendorResponse(
                v.getId(), v.getUserId(), v.getBusinessName(), v.getCategory().name(),
                v.getLocationName(), v.getLatitude(), v.getLongitude(), v.getQrCode(),
                v.isVerified(), v.getRatingAvg(), v.getRatingCount(), v.getPhotoUrl(),
                v.getVehicleRegistration());
    }
}
