package co.za.ukhonapay.dto;
import java.math.BigDecimal;
public record ProviderPaymentIntentResponse(String reference,String provider,String providerReference,String redirectUrl,String status,BigDecimal amount,String currency){}