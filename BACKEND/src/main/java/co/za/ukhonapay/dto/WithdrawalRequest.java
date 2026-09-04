package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull Long atmLocationId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
