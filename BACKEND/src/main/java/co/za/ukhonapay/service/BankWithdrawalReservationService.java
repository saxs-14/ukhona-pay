package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankWithdrawalRequest;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankWithdrawalReservationService {
    private final BankWithdrawalRepository withdrawals;
    private final UserRepository users;
    private final VendorRepository vendors;
    private final WalletRepository wallets;
    private final LedgerAccountService ledgerAccounts;
    private final LedgerService ledger;
    private final PayoutSecretCryptoService secretCrypto;

    public BankWithdrawalReservationService(
            BankWithdrawalRepository withdrawals,
            UserRepository users,
            VendorRepository vendors,
            WalletRepository wallets,
            LedgerAccountService ledgerAccounts,
            LedgerService ledger,
            PayoutSecretCryptoService secretCrypto) {
        this.withdrawals = withdrawals;
        this.users = users;
        this.vendors = vendors;
        this.wallets = wallets;
        this.ledgerAccounts = ledgerAccounts;
        this.ledger = ledger;
        this.secretCrypto = secretCrypto;
    }

    @Transactional
    public BankWithdrawal reserve(Long userId, BankWithdrawalRequest request, BankAccount bankAccount) {
        if (withdrawals.existsByUserIdAndStatus(userId, BankWithdrawalStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending bank withdrawal");
        }
        if (bankAccount.getBankGroupId() == null || bankAccount.getBankGroupId().isBlank()) {
            throw new IllegalStateException("Saved bank account is missing its provider bank identifier");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = wallets.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (request.amount().scale() > 2) {
            throw new IllegalArgumentException("Withdrawal amount may have at most two decimal places");
        }
        BigDecimal amount = request.amount().setScale(2);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Withdrawal amount exceeds available wallet balance");
        }

        String reference = generateReference();
        String encryptionKey = secretCrypto.generatePayoutEncryptionKey();

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallets.save(wallet);

        String walletAccount;
        Vendor vendor = vendors.findByUserId(userId).orElse(null);
        if (vendor != null) {
            walletAccount = ledgerAccounts.ensureVendorWalletAccount(vendor.getId(), userId);
        } else {
            walletAccount = ledgerAccounts.ensureUserWalletAccount(userId);
        }

        ledgerAccounts.ensureSystemAccounts();

        ledger.post(
                "WD-RESERVE-" + reference,
                "PAYOUT_RESERVATION",
                reference,
                "Reserve wallet funds for bank payout " + reference,
                java.util.List.of(
                        new LedgerService.Entry(walletAccount, "DEBIT", amount),
                        new LedgerService.Entry("PAYOUT_CLEARING_ZAR", "CREDIT", amount)
                )
        );

        BankWithdrawal withdrawal = BankWithdrawal.builder()
                .userId(userId)
                .bankAccountId(bankAccount.getId())
                .reference(reference)
                .amount(amount)
                .status(BankWithdrawalStatus.PENDING)
                .build();
        withdrawal.setProviderEncryptionKey(secretCrypto.encrypt(encryptionKey));
        withdrawal.setProviderStatus(0);
        withdrawal.setProviderSubStatus(0);
        withdrawal = withdrawals.save(withdrawal);

        return withdrawal;
    }

    private String generateReference() {
        String raw = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return "BWD-" + raw;
    }
}
