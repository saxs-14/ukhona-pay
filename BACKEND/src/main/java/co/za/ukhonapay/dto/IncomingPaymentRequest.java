package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// A commuter paying a driver/vendor via their own banking app. Deliberately
// has no PIN/sender - the payer never holds a UKHONA PAY account. This stands
// in for what a real bank's payment-confirmation webhook would call.
public record IncomingPaymentRequest(
        @NotBlank String vendorQrCode,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
        String description
) {
}
