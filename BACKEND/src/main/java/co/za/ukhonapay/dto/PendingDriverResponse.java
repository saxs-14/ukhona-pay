package co.za.ukhonapay.dto;

import java.time.LocalDateTime;

public record PendingDriverResponse(
        Long vendorId,
        String name,
        String surname,
        String phoneNumber,
        String vehicleRegistration,
        LocalDateTime registeredAt
) {
}
