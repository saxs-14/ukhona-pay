package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankWithdrawalResponse(
        String reference,
        BigDecimal amount,
        String bankName,
        String maskedAccountNumber,
        String status,
        BigDecimal newWalletBalance,
        LocalDateTime createdAt
) {
}
