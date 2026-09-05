package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long userId,
        BigDecimal balance,
        BigDecimal cashbackBalance,
        BigDecimal savingsBalance,
        BigDecimal maintenanceBalance,
        String currency
) {
}
