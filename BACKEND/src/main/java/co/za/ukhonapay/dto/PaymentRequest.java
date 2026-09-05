package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String vendorQrCode,
        @NotNull @DecimalMin(value = "2.00", message = "amount must be at least R2.00 (covers the R1 platform fee)") BigDecimal amount,
        @NotBlank String pin,
        String description
) {
}
