package co.za.ukhonapay.payment;

import co.za.ukhonapay.model.BankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PayoutProvider {
    boolean isConfigured();

    ProviderPayoutStatus getPayoutStatus(String providerReference);

    ProviderPayoutLookup findPayoutByMerchantReference(String merchantReference, LocalDateTime from, LocalDateTime to);

    ProviderPayoutResponse requestPayout(
            String merchantReference,
            BigDecimal amount,
            BankAccount bankAccount,
            String encryptionKey);
}
