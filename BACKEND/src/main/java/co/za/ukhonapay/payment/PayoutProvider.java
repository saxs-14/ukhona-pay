package co.za.ukhonapay.payment;

import co.za.ukhonapay.model.BankAccount;

import java.math.BigDecimal;

public interface PayoutProvider {
    boolean isConfigured();

    ProviderPayoutStatus getPayoutStatus(String providerReference);

    ProviderPayoutResponse requestPayout(
            String merchantReference,
            BigDecimal amount,
            BankAccount bankAccount,
            String encryptionKey);
}
