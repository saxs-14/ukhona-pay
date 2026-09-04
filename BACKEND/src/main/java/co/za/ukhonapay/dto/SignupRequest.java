package co.za.ukhonapay.dto;

import co.za.ukhonapay.model.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Pattern(regexp = "^0[0-9]{9}$", message = "must be a 10-digit SA phone number starting with 0") String phoneNumber,
        @NotBlank @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits") @Pattern(regexp = "^[0-9]{4}$") String pin,
        @NotNull UserType userType,
        @NotBlank String name,
        @NotBlank String surname,
        @NotBlank @Pattern(regexp = "^[0-9]{13}$", message = "must be a 13-digit SA ID number") String idNumber,
        String email,
        // Driver-only.
        String vehicleRegistration,
        // Driver + Association Administrator.
        Long associationId,
        // Vendor + Association Administrator.
        Long rankId
) {
}
