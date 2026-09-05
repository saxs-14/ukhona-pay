package co.za.ukhonapay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Deliberately excludes phoneNumber/idNumber (unique identity anchors, already
// validated at signup) and userType/pin (changing either has cascading effects
// - e.g. a vendor profile existing/not existing - out of scope for this admin
// surface; use a fresh signup for a genuine role change).
public record AdminUserUpdateRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "name must only contain letters, spaces, hyphens, or apostrophes") String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "surname must only contain letters, spaces, hyphens, or apostrophes") String surname,
        @Email(message = "must be a valid email address") String email,
        Long associationId,
        Long rankId
) {
}
