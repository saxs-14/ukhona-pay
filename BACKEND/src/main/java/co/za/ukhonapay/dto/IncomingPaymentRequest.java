package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// A commuter paying a driver/vendor via their own banking app. Deliberately
// has no PIN/sender - the payer never holds a UKHONA PAY account. This stands
// in for what a real bank's payment-confirmation webhook would call.
//
// This endpoint is unauthenticated by necessity (see PaymentController), which
// means the amount cap below is the only thing standing between a fat-fingered
// or malicious request and an arbitrarily large wallet credit. R10,000 comfortably
// covers a realistic taxi fare or spaza purchase; a real bank integration would
// replace this whole endpoint with a signature-verified webhook and make this
// cap unnecessary.
public record IncomingPaymentRequest(
        @NotBlank String vendorQrCode,
        @NotNull
        @DecimalMin(value = "2.00", message = "amount must be at least R2.00 (covers the R1 platform fee)")
        @DecimalMax(value = "10000", message = "amount exceeds the per-payment limit")
        BigDecimal amount,
        String description
) {
}
