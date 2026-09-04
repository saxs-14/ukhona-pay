package co.za.ukhonapay.dto;

import co.za.ukhonapay.model.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Pattern(regexp = "^0[0-9]{9}$", message = "must be a 10-digit SA phone number starting with 0") String phoneNumber,
        @NotBlank @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits") @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits") String pin,
        @NotNull(message = "select whether you're a driver, vendor, or association administrator") UserType userType,
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "name must only contain letters, spaces, hyphens, or apostrophes") String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z '-]{2,60}$", message = "surname must only contain letters, spaces, hyphens, or apostrophes") String surname,
        // Format-checked here (fast fail); the real SA checksum/date validation
        // happens in AuthService, where a bad value gets a clearer message.
        @NotBlank @Pattern(regexp = "^[0-9]{13}$", message = "must be a 13-digit SA ID number") String idNumber,
        // Optional - real commuters aren't UKHONA PAY users, but a trader/admin
        // may still want an email on file. Blank/absent is fine; if given, it
        // must look like an email.
        @Email(message = "must be a valid email address") String email,
        // Required for TAXI_DRIVER only, enforced in AuthService since it's
        // conditional on role, not on every signup.
        String vehicleRegistration,
        // Driver + Association Administrator.
        Long associationId,
        // Vendor + Association Administrator.
        Long rankId
) {
}
