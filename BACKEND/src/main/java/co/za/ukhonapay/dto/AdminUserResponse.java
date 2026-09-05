package co.za.ukhonapay.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String phoneNumber,
        String userType,
        String name,
        String surname,
        String email,
        Long associationId,
        String associationName,
        Long rankId,
        String rankName,
        LocalDateTime createdAt
) {
}
