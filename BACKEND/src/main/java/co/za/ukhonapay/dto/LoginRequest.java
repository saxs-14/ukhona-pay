package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^0[0-9]{9}$") String phoneNumber,
        @NotBlank String pin
) {
}
