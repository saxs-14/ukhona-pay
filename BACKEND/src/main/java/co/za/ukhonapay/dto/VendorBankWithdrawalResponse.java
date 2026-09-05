package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendorBankWithdrawalResponse(
        String reference,
        BigDecimal amount,
        String bankName,
        String maskedAccount,
        String accountHolderName,
        String status,
        LocalDateTime timestamp
) {
}
