package co.za.ukhonapay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Self-service profile edit - deliberately excludes phoneNumber/idNumber
// (unique identity anchors) and userType/associationId/rankId (those affect
// trust/approval and stay admin-controlled, see AdminUserUpdateRequest).
public record UserSelfUpdateRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "name must only contain letters, spaces, hyphens, or apostrophes") String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "surname must only contain letters, spaces, hyphens, or apostrophes") String surname,
        @Email(message = "must be a valid email address") String email
) {
}
