package co.za.ukhonapay.payment;

import java.math.BigDecimal;

public record ProviderPaymentTransaction(
        String providerReference,
        String merchantReference,
        BigDecimal amount,
        String currency,
        String status,
        String reason) {}
