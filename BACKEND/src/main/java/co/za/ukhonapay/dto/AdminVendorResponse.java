package co.za.ukhonapay.dto;

import java.time.LocalDateTime;

public record AdminVendorResponse(
        Long vendorId,
        Long userId,
        String ownerName,
        String ownerPhone,
        String businessName,
        String category,
        String status,
        String locationName,
        boolean verified,
        String vehicleRegistration,
        Long associationId,
        String associationName,
        Long rankId,
        String rankName,
        LocalDateTime createdAt
) {
}
