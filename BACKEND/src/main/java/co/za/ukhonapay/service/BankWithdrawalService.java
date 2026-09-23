package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankWithdrawalRequest;
import co.za.ukhonapay.dto.BankWithdrawalResponse;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import co.za.ukhonapay.payment.PayoutProvider;
import co.za.ukhonapay.payment.ProviderPayoutResponse;
import co.za.ukhonapay.repository.BankAccountRepository;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BankWithdrawalService {

    private final BankWithdrawalRepository withdrawals;
    private final BankAccountRepository bankAccounts;
    private final BankWithdrawalReservationService reservationService;
    private final BankWithdrawalSettlementService settlementService;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final PasswordEncoder passwordEncoder;
    private final PayoutProvider payoutProvider;
    private final PayoutSecretCryptoService secretCrypto;
    private final String mode;

    public BankWithdrawalService(
            BankWithdrawalRepository withdrawals,
            BankAccountRepository bankAccounts,
            BankWithdrawalReservationService reservationService,
            BankWithdrawalSettlementService settlementService,
            UserRepository users,
            WalletRepository wallets,
            PasswordEncoder passwordEncoder,
            PayoutProvider payoutProvider,
            PayoutSecretCryptoService secretCrypto,
            @Value("${ukhonapay.mode:live}") String mode) {
        this.withdrawals = withdrawals;
        this.bankAccounts = bankAccounts;
        this.reservationService = reservationService;
        this.settlementService = settlementService;
        this.users = users;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
        this.payoutProvider = payoutProvider;
        this.secretCrypto = secretCrypto;
        this.mode = mode;
    }

    public BankWithdrawalResponse withdraw(Long userId, BankWithdrawalRequest req) {
        if (!"live".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("Bank withdrawal is only available through the real provider in live mode.");
        }

        var user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        BankAccount bankAccount = bankAccounts.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No bank account saved yet - add and verify one before withdrawing"));

        BankWithdrawal withdrawal = reservationService.reserve(userId, req, bankAccount);

        try {
            String encryptionKey = secretCrypto.decrypt(withdrawal.getProviderEncryptionKey());
            ProviderPayoutResponse providerResponse =
                    payoutProvider.requestPayout(withdrawal.getReference(), withdrawal.getAmount(), bankAccount, encryptionKey);
            applyProviderResponse(withdrawal.getReference(), providerResponse);
        } catch (Exception e) {
            markProviderUnknown(withdrawal.getReference(), e.getMessage());
        }

        return responseFor(withdrawal.getReference());
    }

    @Transactional
    public void applyProviderResponse(String reference, ProviderPayoutResponse response) {
        BankWithdrawal withdrawal = withdrawals.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));

        withdrawal.setProviderStatus(response.providerStatus());
        withdrawal.setProviderSubStatus(response.providerSubStatus());
        withdrawal.setProviderError(response.errorMessage());

        if (response.accepted()) {
            withdrawal.setProviderReference(response.providerReference());
            withdrawals.save(withdrawal);
            return;
        }

        withdrawal.setStatus(BankWithdrawalStatus.FAILED);
        withdrawals.save(withdrawal);
        settlementService.applyNotification(failureEventFor(withdrawal, response.errorMessage()));
    }

    @Transactional
    public void markProviderUnknown(String reference, String error) {
        BankWithdrawal withdrawal = withdrawals.findByReferenceForUpdate(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));
        withdrawal.setProviderError(truncate(error));
        withdrawals.save(withdrawal);
    }

    public List<BankWithdrawalResponse> historyForUser(Long userId) {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> {
                    BankAccount account = bankAccounts.findById(w.getBankAccountId()).orElse(null);
                    return toResponse(w, account, null);
                })
                .toList();
    }

    private BankWithdrawalResponse responseFor(String reference) {
        BankWithdrawal withdrawal = withdrawals.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));
        BankAccount account = bankAccounts.findById(withdrawal.getBankAccountId()).orElse(null);
        BigDecimal balance = wallets.findByUserId(withdrawal.getUserId())
                .map(w -> w.getBalance())
                .orElse(null);
        return toResponse(withdrawal, account, balance);
    }

    private BankWithdrawalResponse toResponse(BankWithdrawal w, BankAccount account, BigDecimal newBalance) {
        String bankName = account != null ? account.getBankName() : "Unknown";
        String masked = account != null ? mask(account.getAccountNumber()) : null;
        return new BankWithdrawalResponse(w.getReference(), w.getAmount(), bankName, masked,
                w.getStatus().name(), newBalance, w.getCreatedAt());
    }

    private String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return accountNumber;
        return "••••" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "Provider response was unavailable";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private co.za.ukhonapay.model.PayoutNotificationEvent failureEventFor(
            BankWithdrawal withdrawal, String reason) {
        var event = new co.za.ukhonapay.model.PayoutNotificationEvent();
        event.setProvider("OZOW");
        event.setPayoutReference(withdrawal.getProviderReference() == null
                ? "REJECTED-" + withdrawal.getReference()
                : withdrawal.getProviderReference());
        event.setMerchantReference(withdrawal.getReference());
        event.setStatus(4);
        event.setSubStatus(401);
        event.setHashVerified(true);
        event.setProcessingStatus("PROCESSED");
        event.setPayload(reason == null ? "" : reason);
        return event;
    }
}
