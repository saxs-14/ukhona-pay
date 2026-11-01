package co.za.ukhonapay.payment;
public record ProviderPaymentResponse(String providerReference,String redirectUrl,String status){}