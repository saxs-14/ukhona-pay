package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.PendingDriverResponse;
import co.za.ukhonapay.dto.VendorResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.model.enums.VendorStatus;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final WalletRepository walletRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final UserRepository userRepository;

    public VendorService(VendorRepository vendorRepository, WalletRepository walletRepository,
                          TaxiAssociationRepository taxiAssociationRepository, UserRepository userRepository) {
        this.vendorRepository = vendorRepository;
        this.walletRepository = walletRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.userRepository = userRepository;
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

    public VendorResponse getById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for ID " + id));
        return toResponse(vendor, false);
    }

    // Drivers awaiting this admin's association's review - see VendorStatus.
    public List<PendingDriverResponse> pendingDriversForAssociation(Long associationId) {
        List<Vendor> pending = vendorRepository.findByAssociationIdAndStatusOrderByCreatedAt(associationId, VendorStatus.PENDING);
        return pending.stream().map(v -> {
            User user = userRepository.findById(v.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found for pending driver"));
            return new PendingDriverResponse(v.getId(), user.getName(), user.getSurname(),
                    user.getPhoneNumber(), v.getVehicleRegistration(), v.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void approveDriver(Long adminAssociationId, Long vendorId) {
        setDriverStatus(adminAssociationId, vendorId, VendorStatus.APPROVED);
    }

    @Transactional
    public void rejectDriver(Long adminAssociationId, Long vendorId) {
        setDriverStatus(adminAssociationId, vendorId, VendorStatus.REJECTED);
    }

    // 404s (rather than 403) if the driver belongs to a different association -
    // this admin shouldn't learn that a vendorId exists at all in that case,
    // let alone be able to probe which association it belongs to.
    private void setDriverStatus(Long adminAssociationId, Long vendorId, VendorStatus status) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (vendor.getAssociationId() == null || !vendor.getAssociationId().equals(adminAssociationId)) {
            throw new ResourceNotFoundException("Driver not found");
        }
        vendor.setStatus(status);
        vendorRepository.save(vendor);
    }

    private VendorResponse toResponse(Vendor v, boolean includeWalletBalance) {
        BigDecimal walletBalance = !includeWalletBalance ? null
                : walletRepository.findByUserId(v.getUserId()).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        String associationName = v.getAssociationId() == null ? null
                : taxiAssociationRepository.findById(v.getAssociationId()).map(TaxiAssociation::getName).orElse(null);

        return new VendorResponse(
                v.getId(), v.getUserId(), v.getBusinessName(), v.getCategory().name(),
                v.getLocationName(), v.getStatus().name(), v.getLatitude(), v.getLongitude(), v.getQrCode(),
                v.isVerified(), v.getPhotoUrl(),
                v.getVehicleRegistration(), walletBalance, v.getAssociationId(), associationName);
    }
}
