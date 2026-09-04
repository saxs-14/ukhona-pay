package co.za.ukhonapay.dto;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String userType,
        String phoneNumber
) {
}
