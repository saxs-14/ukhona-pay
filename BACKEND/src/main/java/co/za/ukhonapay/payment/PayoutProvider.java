package co.za.ukhonapay.payment;

import co.za.ukhonapay.model.BankAccount;

import java.math.BigDecimal;

public interface PayoutProvider {
    ProviderPayoutResponse requestPayout(
            String merchantReference,
            BigDecimal amount,
            BankAccount bankAccount,
            String encryptionKey);
}
