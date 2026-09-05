package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicePurchaseResponse(
        String reference,
        String type,
        BigDecimal amount,
        String voucherToken,
        String tokenLabel,
        String dialInstruction,
        String extraLabel,
        String extraValue,
        String title,
        String subtitle,
        String network,
        BigDecimal newWalletBalance,
        LocalDateTime timestamp
) {
}
