package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BankWithdrawalRequest(
        @NotNull @DecimalMin(value = "1.00", message = "minimum bank withdrawal is R1.00") BigDecimal amount,
        @NotBlank String pin
) {
}
