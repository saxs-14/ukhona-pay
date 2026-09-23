package co.za.ukhonapay.service;

import co.za.ukhonapay.model.LedgerAccount;
import co.za.ukhonapay.repository.LedgerAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerAccountService {
    private final LedgerAccountRepository accounts;

    public LedgerAccountService(LedgerAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public String ensureUserWalletAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for a wallet ledger account");
        }
        String code = "USER_WALLET_ZAR_" + userId;
        accounts.ensureAccount(code, "USER_WALLET", userId, null);
        return code;
    }

    @Transactional
    public String ensureVendorWalletAccount(Long vendorId, Long userId) {
        if (vendorId == null || userId == null) {
            throw new IllegalArgumentException("Vendor and user IDs are required for a wallet ledger account");
        }
        String code = "VENDOR_WALLET_ZAR_" + vendorId;
        accounts.ensureAccount(code, "VENDOR_WALLET", userId, null);
        return code;
    }

    @Transactional
    public String ensureAssociationWalletAccount(Long associationId) {
        if (associationId == null) {
            throw new IllegalArgumentException("Association ID is required for a wallet ledger account");
        }
        String code = "ASSOCIATION_WALLET_ZAR_" + associationId;
        accounts.ensureAccount(code, "ASSOCIATION_WALLET", null, associationId);
        return code;
    }

    @Transactional
    public void ensureSystemAccounts() {
        accounts.ensureAccount("PLATFORM_FEE_REVENUE_ZAR", "PLATFORM_REVENUE", null, null);
        accounts.ensureAccount("PAYMENT_CLEARING_ZAR", "PAYMENT_CLEARING", null, null);
        accounts.ensureAccount("PAYOUT_CLEARING_ZAR", "PAYOUT_CLEARING", null, null);
        accounts.ensureAccount("OZOW_PAYOUT_FLOAT_ZAR", "EXTERNAL_FLOAT", null, null);
    }

    public LedgerAccount requireAccount(String code) {
        return accounts.findByAccountCode(code)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + code));
    }
}
