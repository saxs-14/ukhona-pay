package co.za.ukhonapay.payment;

public record ProviderPayoutStatus(
        String providerReference,
        int status,
        int subStatus,
        String errorMessage
) {}
