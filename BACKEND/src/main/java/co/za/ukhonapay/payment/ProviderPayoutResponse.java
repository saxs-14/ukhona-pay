package co.za.ukhonapay.payment;

public record ProviderPayoutResponse(
        boolean accepted,
        String providerReference,
        int providerStatus,
        int providerSubStatus,
        String errorMessage
) {}
