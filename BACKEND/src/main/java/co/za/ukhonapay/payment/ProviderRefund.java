package co.za.ukhonapay.payment;

import java.math.BigDecimal;

public record ProviderRefund(
        String refundReference,
        String transactionReference,
        BigDecimal amount,
        String currency,
        String status,
        String reason) {
}
