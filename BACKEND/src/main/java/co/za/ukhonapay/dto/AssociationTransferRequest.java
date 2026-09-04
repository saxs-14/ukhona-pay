package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssociationTransferRequest(
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
        @NotBlank String pin,
        String description
) {
}
