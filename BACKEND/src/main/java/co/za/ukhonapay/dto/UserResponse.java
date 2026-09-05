package co.za.ukhonapay.dto;

public record UserResponse(
        Long id,
        String phoneNumber,
        String userType,
        String name,
        String surname,
        String email,
        Long associationId,
        String associationName,
        String rankName
) {
}
