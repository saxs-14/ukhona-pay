package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record AtmLocationResponse(
        Long id,
        String name,
        String address,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        String bank,
        Double distanceKm
) {
}
