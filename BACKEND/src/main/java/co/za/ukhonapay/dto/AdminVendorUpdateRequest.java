package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminVendorUpdateRequest(
        @NotBlank String businessName,
        @NotBlank String category,
        @NotBlank String locationName,
        @NotBlank String status,
        boolean verified,
        String vehicleRegistration,
        Long associationId,
        Long rankId
) {
}
