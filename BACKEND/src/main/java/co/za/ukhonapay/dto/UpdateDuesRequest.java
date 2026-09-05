package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateDuesRequest(
        @NotNull @DecimalMin(value = "0.00", message = "duesAmount cannot be negative") BigDecimal duesAmount
) {
}
