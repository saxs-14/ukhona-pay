package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String reference,
        Long transactionId,
        Long vendorId,
        String vendorName,
        BigDecimal amount,
        BigDecimal cashbackEarned,
        BigDecimal newWalletBalance,
        BigDecimal newCashbackBalance,
        LocalDateTime timestamp
) {
}
