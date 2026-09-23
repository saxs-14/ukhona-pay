package co.za.ukhonapay.service;

import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.model.PayoutNotificationEvent;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.BankWithdrawalStatus;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankWithdrawalSettlementService {
    private final BankWithdrawalRepository withdrawals;
    private final WalletRepository wallets;
    private final VendorRepository vendors;
    private final LedgerAccountService ledgerAccounts;
    private final LedgerService ledger;

    public BankWithdrawalSettlementService(
            BankWithdrawalRepository withdrawals,
            WalletRepository wallets,
            VendorRepository vendors,
            LedgerAccountService ledgerAccounts,
            LedgerService ledger) {
        this.withdrawals = withdrawals;
        this.wallets = wallets;
        this.vendors = vendors;
        this.ledgerAccounts = ledgerAccounts;
        this.ledger = ledger;
    }

    @Transactional
    public void applyNotification(PayoutNotificationEvent event) {
        BankWithdrawal withdrawal = withdrawals.findByReferenceForUpdate(event.getMerchantReference())
                .orElseThrow(() -> new IllegalStateException(
                        "No bank withdrawal exists for payout merchant reference " + event.getMerchantReference()));

        if (withdrawal.getProviderReference() == null
                || !withdrawal.getProviderReference().equals(event.getPayoutReference())) {
            throw new IllegalStateException("Payout provider reference does not match the withdrawal");
        }

        withdrawal.setProviderStatus(event.getStatus());
        withdrawal.setProviderSubStatus(event.getSubStatus());

        if (event.getStatus() == 5) {
            complete(withdrawal);
        } else if (event.getStatus() == 4 || event.getStatus() == 90 || event.getStatus() == 99) {
            failOrReturn(withdrawal, "Ozow payout failed or was returned");
        }

        withdrawals.save(withdrawal);
    }

    private void complete(BankWithdrawal withdrawal) {
        if (withdrawal.getStatus() == BankWithdrawalStatus.COMPLETED) return;
        if (withdrawal.getStatus() == BankWithdrawalStatus.FAILED) {
            throw new IllegalStateException("A failed withdrawal cannot later be completed");
        }

        ledgerAccounts.ensureSystemAccounts();
        ledger.post(
                "WD-SETTLE-" + withdrawal.getReference(),
                "PAYOUT_SETTLEMENT",
                withdrawal.getReference(),
                "Settle completed Ozow payout " + withdrawal.getReference(),
                java.util.List.of(
                        new LedgerService.Entry("PAYOUT_CLEARING_ZAR", "DEBIT", withdrawal.getAmount()),
                        new LedgerService.Entry("OZOW_PAYOUT_FLOAT_ZAR", "CREDIT", withdrawal.getAmount())
                )
        );

        withdrawal.setStatus(BankWithdrawalStatus.COMPLETED);
    }

    private void failOrReturn(BankWithdrawal withdrawal, String reason) {
        if (withdrawal.getStatus() == BankWithdrawalStatus.FAILED) return;

        BigDecimal amount = withdrawal.getAmount();
        if (withdrawal.getStatus() == BankWithdrawalStatus.COMPLETED) {
            refundAfterCompleted(withdrawal);
        } else {
            refundPending(withdrawal);
        }
        withdrawal.setStatus(BankWithdrawalStatus.FAILED);
        withdrawal.setProviderError(reason);
    }

    private void refundPending(BankWithdrawal withdrawal) {
        Wallet wallet = wallets.findWithLockByUserId(withdrawal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for payout refund"));
        wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
        wallets.save(wallet);

        String walletAccount = walletLedgerAccount(withdrawal.getUserId());
        ledger.post(
                "WD-RELEASE-" + withdrawal.getReference(),
                "PAYOUT_RELEASE",
                withdrawal.getReference(),
                "Release failed Ozow payout reservation " + withdrawal.getReference(),
                java.util.List.of(
                        new LedgerService.Entry("PAYOUT_CLEARING_ZAR", "DEBIT", withdrawal.getAmount()),
                        new LedgerService.Entry(walletAccount, "CREDIT", withdrawal.getAmount())
                )
        );
    }

    private void refundAfterCompleted(BankWithdrawal withdrawal) {
        Wallet wallet = wallets.findWithLockByUserId(withdrawal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found for returned payout"));
        wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
        wallets.save(wallet);

        String walletAccount = walletLedgerAccount(withdrawal.getUserId());
        ledger.post(
                "WD-RETURN-" + withdrawal.getReference(),
                "PAYOUT_RETURN",
                withdrawal.getReference(),
                "Return completed Ozow payout " + withdrawal.getReference(),
                java.util.List.of(
                        new LedgerService.Entry("OZOW_PAYOUT_FLOAT_ZAR", "DEBIT", withdrawal.getAmount()),
                        new LedgerService.Entry(walletAccount, "CREDIT", withdrawal.getAmount())
                )
        );
    }

    private String walletLedgerAccount(Long userId) {
        Vendor vendor = vendors.findByUserId(userId).orElse(null);
        if (vendor != null) {
            return ledgerAccounts.ensureVendorWalletAccount(vendor.getId(), userId);
        }
        return ledgerAccounts.ensureUserWalletAccount(userId);
    }
}
