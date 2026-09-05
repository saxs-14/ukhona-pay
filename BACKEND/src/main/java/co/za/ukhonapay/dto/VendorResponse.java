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
<<<<<<< HEAD
        BigDecimal walletBalance
=======
        BigDecimal walletBalance,
        Long associationId,
        String associationName
>>>>>>> 64b97030878b67831e527c719de4297ec8551cac
) {
}
