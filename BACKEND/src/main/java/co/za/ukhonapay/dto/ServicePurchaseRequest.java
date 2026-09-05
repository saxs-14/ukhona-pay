package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ServicePurchaseRequest(
        @NotBlank @Pattern(regexp = "AIRTIME|ELECTRICITY|PAYAT_BILL") String type,
        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @DecimalMax(value = "2000", message = "amount exceeds the per-purchase limit")
        BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits") String pin,

        // AIRTIME
        String network,
        String recipientPhone,

        // ELECTRICITY
        String meterNumber,
        String municipality,

        // PAYAT_BILL
        String billerId,
        String billerName,
        String billerCategory,
        String payAtReference,
        String accountName
) {
}
