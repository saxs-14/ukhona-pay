package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record VendorResponse(
        Long vendorId,
        Long userId,
        String businessName,
        String category,
        String locationName,
        String status,
        BigDecimal latitude,
        BigDecimal longitude,
        String qrCode,
        boolean verified,
        String photoUrl,
        String vehicleRegistration,
        BigDecimal walletBalance,
        Long associationId,
        String associationName
) {
}
