package co.za.ukhonapay.payment;

import java.math.BigDecimal;

public record ProviderPayoutLookup(
        String providerReference,
        String merchantReference,
        BigDecimal amount,
        int status,
        int subStatus,
        String errorMessage) {}
