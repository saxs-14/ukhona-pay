package co.za.ukhonapay.payment;
import java.math.BigDecimal;
public interface PaymentProvider{String name(); ProviderPaymentResponse createPayment(String reference,BigDecimal amount,String currency,String returnUrl,String idempotencyKey);}