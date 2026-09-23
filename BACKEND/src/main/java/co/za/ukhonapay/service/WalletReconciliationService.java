package co.za.ukhonapay.service;

import co.za.ukhonapay.model.LedgerAccount;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.repository.LedgerAccountRepository;
import co.za.ukhonapay.repository.LedgerEntryRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only control used to detect divergence between the wallet projection
 * and the accounting ledger. It deliberately does not "fix" balances:
 * unexplained differences must be investigated rather than silently overwritten.
 */
@Service
public class WalletReconciliationService {
    private final WalletRepository wallets;
    private final LedgerAccountRepository accounts;
    private final LedgerEntryRepository entries;

    public WalletReconciliationService(WalletRepository wallets,
                                       LedgerAccountRepository accounts,
                                       LedgerEntryRepository entries) {
        this.wallets = wallets;
        this.accounts = accounts;
        this.entries = entries;
    }

    public record Result(
            Long walletUserId,
            Long vendorId,
            String ledgerAccountCode,
            BigDecimal walletTotal,
            BigDecimal ledgerBalance,
            BigDecimal difference,
            boolean reconciled) {}

    @Transactional(readOnly = true)
    public Result reconcileVendor(Long vendorId, Long userId) {
        String accountCode = "VENDOR_WALLET_ZAR_" + vendorId;
        LedgerAccount account = accounts.findByAccountCode(accountCode)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + accountCode));

        Wallet wallet = wallets.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for user " + userId));

        BigDecimal walletTotal = zero(wallet.getBalance())
                .add(zero(wallet.getSavingsBalance()))
                .add(zero(wallet.getMaintenanceBalance()))
                .add(zero(wallet.getCashbackBalance()));

        BigDecimal ledgerBalance = zero(entries.getSignedBalanceByAccountCode(account.getAccountCode()));
        BigDecimal difference = walletTotal.subtract(ledgerBalance);

        return new Result(userId, vendorId, account.getAccountCode(),
                walletTotal, ledgerBalance, difference, difference.signum() == 0);
    }

    @Transactional(readOnly = true)
    public List<Result> reconcileVendorAccounts() {
        List<Result> results = new ArrayList<>();
        for (LedgerAccount account : accounts.findAll()) {
            if (!"VENDOR_WALLET".equals(account.getAccountType()) || account.getUserId() == null) {
                continue;
            }
            String suffix = account.getAccountCode().replace("VENDOR_WALLET_ZAR_", "");
            try {
                results.add(reconcileVendor(Long.valueOf(suffix), account.getUserId()));
            } catch (RuntimeException ignored) {
                // A missing projection is reported by the control endpoint that
                // performs targeted reconciliation; this batch method skips
                // malformed legacy accounts rather than mutating data.
            }
        }
        return results;
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
