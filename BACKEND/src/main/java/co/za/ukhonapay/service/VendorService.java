package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.VendorResponse;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final WalletRepository walletRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;

    public VendorService(VendorRepository vendorRepository, WalletRepository walletRepository,
                          TaxiAssociationRepository taxiAssociationRepository) {
        this.vendorRepository = vendorRepository;
        this.walletRepository = walletRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
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
        return vendors.stream().map(v -> toResponse(v, false)).toList();
    }

    // Public lookup (see SecurityConfig - GET /api/vendors/qr/* is unauthenticated
    // so a commuter can look up who they're paying before they have an account).
    // Deliberately omits the wallet balance: an anonymous visitor who scans or
    // guesses a QR code has no business seeing that trader's bank balance.
    public VendorResponse getByQrCode(String qrCode) {
        Vendor vendor = vendorRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for QR code " + qrCode));
        return toResponse(vendor, false);
    }

    // The account owner viewing their own profile - the only case that should
    // ever see the wallet balance.
    public VendorResponse getByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new VendorNotFoundException("No vendor profile for user " + userId));
        return toResponse(vendor, true);
    }

    private VendorResponse toResponse(Vendor v, boolean includeWalletBalance) {
        BigDecimal walletBalance = !includeWalletBalance ? null
                : walletRepository.findByUserId(v.getUserId()).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        String associationName = v.getAssociationId() == null ? null
                : taxiAssociationRepository.findById(v.getAssociationId()).map(TaxiAssociation::getName).orElse(null);

        return new VendorResponse(
                v.getId(), v.getUserId(), v.getBusinessName(), v.getCategory().name(),
                v.getLocationName(), v.getLatitude(), v.getLongitude(), v.getQrCode(),
                v.isVerified(), v.getRatingAvg(), v.getRatingCount(), v.getPhotoUrl(),
                v.getVehicleRegistration(), walletBalance, v.getAssociationId(), associationName);
    }
}
