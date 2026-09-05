package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;

// Self-service vendor/driver profile edit - deliberately excludes category,
// status, verified, locationName, associationId, and rankId (those affect
// trust/placement and stay admin-controlled, see AdminVendorUpdateRequest).
public record VendorSelfUpdateRequest(
        @NotBlank String businessName,
        // Driver-only; blank/absent for a plain VENDOR.
        String vehicleRegistration
) {
}
