package co.za.ukhonapay.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentProvider {
    String name();
    ProviderPaymentResponse createPayment(String reference, BigDecimal amount, String currency, String returnUrl, String idempotencyKey);
    List<ProviderPaymentTransaction> getTransactions(String paymentReference, LocalDate fromDate, LocalDate toDate);
    ProviderPaymentTransaction getTransaction(String transactionReference);
    ProviderRefund getRefund(String refundReference);
    ProviderPaymentRequestStatus getPaymentStatus(String paymentReference);
}
