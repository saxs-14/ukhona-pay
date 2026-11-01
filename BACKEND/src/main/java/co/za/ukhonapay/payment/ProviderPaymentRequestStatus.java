package co.za.ukhonapay.payment;

public record ProviderPaymentRequestStatus(
        String providerReference,
        String status,
        String reason) {}
