package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IncomingPaymentResponse(
        String reference,
        Long vendorId,
        String vendorName,
        BigDecimal amount,
        BigDecimal newVendorBalance,
        LocalDateTime timestamp
) {
}
