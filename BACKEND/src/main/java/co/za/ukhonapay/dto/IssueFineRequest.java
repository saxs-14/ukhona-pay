package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IssueFineRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @DecimalMax(value = "10000", message = "amount exceeds the per-fine limit")
        BigDecimal amount,
        @NotBlank(message = "a reason is required")
        @Size(max = 255)
        String reason
) {
}
