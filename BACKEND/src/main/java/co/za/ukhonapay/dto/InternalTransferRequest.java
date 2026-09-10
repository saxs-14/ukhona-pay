package co.za.ukhonapay.dto;

import co.za.ukhonapay.model.enums.WalletPocket;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InternalTransferRequest(
        @NotNull WalletPocket from,
        @NotNull WalletPocket to,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount
) {
}
