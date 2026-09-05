package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AdminReferenceUpdateRequest;
import co.za.ukhonapay.dto.AdminUserResponse;
import co.za.ukhonapay.dto.AdminUserUpdateRequest;
import co.za.ukhonapay.dto.AdminVendorResponse;
import co.za.ukhonapay.dto.AdminVendorUpdateRequest;
import co.za.ukhonapay.dto.TaxiAssociationResponse;
import co.za.ukhonapay.dto.TaxiRankResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.TaxiRank;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.enums.UserType;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.model.enums.VendorStatus;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backs the platform-admin surface: full add/remove/update control over the
 * app's reference and identity data (users, vendor/driver profiles, taxi
 * associations, taxi ranks). Deliberately does not touch the transaction
 * ledger, which stays immutable by design (see TRANSACTIONS comment in
 * schema.sql) - an admin can see money move (existing /analytics/platform)
 * but not rewrite it.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final TaxiRankRepository taxiRankRepository;

    public AdminService(UserRepository userRepository,
                         VendorRepository vendorRepository,
                         TaxiAssociationRepository taxiAssociationRepository,
                         TaxiRankRepository taxiRankRepository) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.taxiRankRepository = taxiRankRepository;
    }

    // ---- Users ----------------------------------------------------------

    public List<AdminUserResponse> listUsers() {
        Map<Long, String> associationNames = allAssociationNames();
        Map<Long, String> rankNames = allRankNames();
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> toAdminUserResponse(u, associationNames, rankNames))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.associationId() != null) {
            taxiAssociationRepository.findById(req.associationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));
        }
        if (req.rankId() != null) {
            taxiRankRepository.findById(req.rankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Taxi rank not found"));
        }

        user.setName(req.name());
        user.setSurname(req.surname());
        user.setEmail(req.email());
        user.setAssociationId(req.associationId());
        user.setRankId(req.rankId());
        user = userRepository.save(user);

        return toAdminUserResponse(user, allAssociationNames(), allRankNames());
    }

    @Transactional
    public void deleteUser(Long userId, Long callingAdminId) {
        if (userId.equals(callingAdminId)) {
            throw new IllegalArgumentException("You cannot delete your own admin account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getUserType() == UserType.ADMIN && userRepository.countByUserType(UserType.ADMIN) <= 1) {
            throw new IllegalArgumentException("Cannot delete the last remaining admin account");
        }
        // Cascades to the user's wallet and vendor profile (ON DELETE CASCADE
        // in schema.sql) - transactions keep the id as a plain historical
        // reference, they don't cascade-delete.
        userRepository.deleteById(userId);
    }

    // ---- Vendors / drivers ------------------------------------------------

    public List<AdminVendorResponse> listVendors() {
        Map<Long, String> associationNames = allAssociationNames();
        Map<Long, String> rankNames = allRankNames();
        List<Vendor> vendors = vendorRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, User> ownersById = userRepository.findAllById(vendors.stream().map(Vendor::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        return vendors.stream().map(v -> {
            User owner = ownersById.get(v.getUserId());
            return new AdminVendorResponse(
                    v.getId(), v.getUserId(),
                    owner != null ? owner.getName() + " " + owner.getSurname() : "Unknown",
                    owner != null ? owner.getPhoneNumber() : "—",
                    v.getBusinessName(), v.getCategory().name(), v.getStatus().name(), v.getLocationName(),
                    v.isVerified(), v.getVehicleRegistration(),
                    v.getAssociationId(), v.getAssociationId() != null ? associationNames.get(v.getAssociationId()) : null,
                    v.getRankId(), v.getRankId() != null ? rankNames.get(v.getRankId()) : null,
                    v.getCreatedAt());
        }).toList();
    }

    @Transactional
    public AdminVendorResponse updateVendor(Long vendorId, AdminVendorUpdateRequest req) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        VendorCategory category = VendorCategory.valueOf(req.category().toUpperCase());
        VendorStatus status = VendorStatus.valueOf(req.status().toUpperCase());
        if (req.associationId() != null) {
            taxiAssociationRepository.findById(req.associationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));
        }
        if (req.rankId() != null) {
            taxiRankRepository.findById(req.rankId())
                    .orElseThrow(() -> new ResourceNotFoundException("Taxi rank not found"));
        }

        vendor.setBusinessName(req.businessName());
        vendor.setCategory(category);
        vendor.setStatus(status);
        vendor.setLocationName(req.locationName());
        vendor.setVerified(req.verified());
        vendor.setVehicleRegistration(req.vehicleRegistration());
        vendor.setAssociationId(req.associationId());
        vendor.setRankId(req.rankId());
        vendor = vendorRepository.save(vendor);

        Map<Long, String> associationNames = allAssociationNames();
        Map<Long, String> rankNames = allRankNames();
        User owner = userRepository.findById(vendor.getUserId()).orElse(null);
        return new AdminVendorResponse(
                vendor.getId(), vendor.getUserId(),
                owner != null ? owner.getName() + " " + owner.getSurname() : "Unknown",
                owner != null ? owner.getPhoneNumber() : "—",
                vendor.getBusinessName(), vendor.getCategory().name(), vendor.getStatus().name(), vendor.getLocationName(),
                vendor.isVerified(), vendor.getVehicleRegistration(),
                vendor.getAssociationId(), vendor.getAssociationId() != null ? associationNames.get(vendor.getAssociationId()) : null,
                vendor.getRankId(), vendor.getRankId() != null ? rankNames.get(vendor.getRankId()) : null,
                vendor.getCreatedAt());
    }

    @Transactional
    public void deleteVendor(Long vendorId) {
        if (!vendorRepository.existsById(vendorId)) {
            throw new ResourceNotFoundException("Vendor not found");
        }
        // Removes only the business/driver profile - the underlying user
        // account (and its wallet) is untouched, matching "remove a listing"
        // rather than "remove a person".
        vendorRepository.deleteById(vendorId);
    }

    // ---- Taxi associations / ranks ---------------------------------------

    @Transactional
    public TaxiAssociationResponse updateAssociation(Long id, AdminReferenceUpdateRequest req) {
        TaxiAssociation association = taxiAssociationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));
        association.setName(req.name().trim());
        association = taxiAssociationRepository.save(association);
        return new TaxiAssociationResponse(association.getId(), association.getName());
    }

    @Transactional
    public void deleteAssociation(Long id) {
        if (!taxiAssociationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Taxi association not found");
        }
        // No ON DELETE CASCADE from users/vendors/wallets/transactions to
        // taxi_associations - deleting one still linked to any of those fails
        // with a foreign-key violation, translated by GlobalExceptionHandler
        // into a clean 409 rather than a raw SQL error.
        taxiAssociationRepository.deleteById(id);
    }

    @Transactional
    public TaxiRankResponse updateRank(Long id, AdminReferenceUpdateRequest req) {
        TaxiRank rank = taxiRankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi rank not found"));
        rank.setName(req.name().trim());
        rank.setLocationName(req.locationName());
        rank = taxiRankRepository.save(rank);
        return new TaxiRankResponse(rank.getId(), rank.getName(), rank.getLocationName());
    }

    @Transactional
    public void deleteRank(Long id) {
        if (!taxiRankRepository.existsById(id)) {
            throw new ResourceNotFoundException("Taxi rank not found");
        }
        taxiRankRepository.deleteById(id);
    }

    // ---- helpers ----------------------------------------------------------

    private Map<Long, String> allAssociationNames() {
        return taxiAssociationRepository.findAll().stream()
                .collect(Collectors.toMap(TaxiAssociation::getId, TaxiAssociation::getName));
    }

    private Map<Long, String> allRankNames() {
        return taxiRankRepository.findAll().stream()
                .collect(Collectors.toMap(TaxiRank::getId, TaxiRank::getName));
    }

    private AdminUserResponse toAdminUserResponse(User u, Map<Long, String> associationNames, Map<Long, String> rankNames) {
        return new AdminUserResponse(
                u.getId(), u.getPhoneNumber(), u.getUserType().name(), u.getName(), u.getSurname(), u.getEmail(),
                u.getAssociationId(), u.getAssociationId() != null ? associationNames.get(u.getAssociationId()) : null,
                u.getRankId(), u.getRankId() != null ? rankNames.get(u.getRankId()) : null,
                u.getCreatedAt());
    }
}
