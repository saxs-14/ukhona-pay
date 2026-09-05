package co.za.ukhonapay.dto;

import java.time.LocalDateTime;

// A driver linked to the calling association admin's association, at any
// review status - unlike PendingDriverResponse (PENDING only), this backs a
// full roster view so the admin can see everyone registered to their
// association, not just the current approval queue.
public record AssociationDriverResponse(
        Long vendorId,
        String name,
        String surname,
        String phoneNumber,
        String vehicleRegistration,
        String status,
        LocalDateTime registeredAt
) {
}
