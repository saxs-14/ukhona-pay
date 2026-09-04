package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record VendorResponse(
        Long vendorId,
        Long userId,
        String businessName,
        String category,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        String qrCode,
        boolean verified,
        BigDecimal ratingAvg,
        int ratingCount,
        String photoUrl
) {
}
