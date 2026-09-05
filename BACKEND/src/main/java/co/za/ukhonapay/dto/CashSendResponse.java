package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CashSendResponse(
        String reference,
        String voucherNumber,
        String cashSendPin,
        BigDecimal amount,
        String recipientPhone,
        String maskedPhone,
        String status,
        List<String> validOutlets,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
