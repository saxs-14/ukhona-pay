package co.za.ukhonapay.service;

import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderPaymentSettlementService {
    private final PaymentIntentRepository intents;
    private final VendorRepository vendors;
    private final WalletRepository wallets;
    private final TransactionRepository transactions;
    private final LedgerService ledger;
    private final LedgerAccountService ledgerAccounts;

    public ProviderPaymentSettlementService(
            PaymentIntentRepository intents,
            VendorRepository vendors,
            WalletRepository wallets,
            TransactionRepository transactions,
            LedgerService ledger,
            LedgerAccountService ledgerAccounts) {
        this.intents = intents;
        this.vendors = vendors;
        this.wallets = wallets;
        this.transactions = transactions;
        this.ledger = ledger;
        this.ledgerAccounts = ledgerAccounts;
    }

    @Transactional
    public void settleSuccessful(
            String internalReference,
            String providerReference,
            BigDecimal amount,
            String currency) {

        PaymentIntent intent = intents.findByInternalReferenceForUpdate(internalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));

        if (!"ZAR".equals(currency) || amount == null || intent.getAmount().compareTo(amount) != 0) {
            throw new IllegalArgumentException(
                    "Provider payment amount or currency does not match the payment intent");
        }

        if (intent.getProviderReference() != null
                && !intent.getProviderReference().equals(providerReference)) {
            throw new IllegalArgumentException("Provider reference does not match the payment intent");
        }

        if ("COMPLETED".equals(intent.getStatus())) {
            return;
        }
        if (!"PENDING".equals(intent.getStatus())) {
            throw new IllegalStateException(
                    "Payment intent is not payable in status " + intent.getStatus());
        }

        Vendor vendor = vendors.findById(intent.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        Wallet vendorWallet = wallets.findWithLockByUserId(vendor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor wallet not found"));

        BigDecimal fee = WalletService.PLATFORM_FEE;
        BigDecimal net = amount.subtract(fee);
        if (net.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount is too small for the configured platform fee");
        }

        String vendorAccountCode =
                ledgerAccounts.ensureVendorWalletAccount(vendor.getId(), vendor.getUserId());

        /*
         * No human ADMIN wallet is involved in settlement.
         * Platform revenue belongs to the dedicated system ledger account.
         * This prevents customer money or fee revenue from being mixed with
         * an administrator's personal wallet.
         */
        ledger.post(
                "LED-" + internalReference,
                "PROVIDER_PAYMENT",
                providerReference,
                "Provider settlement " + internalReference,
                List.of(
                        new LedgerService.Entry("PAYMENT_CLEARING_ZAR", "DEBIT", amount),
                        new LedgerService.Entry(vendorAccountCode, "CREDIT", net),
                        new LedgerService.Entry("PLATFORM_FEE_REVENUE_ZAR", "CREDIT", fee)
                ));

        WalletService.creditWithAutoAllocation(vendorWallet, net);
        wallets.save(vendorWallet);

        Transaction tx = Transaction.builder()
                .reference("TXN-" + java.util.UUID.randomUUID()
                        .toString().replace("-", "").substring(0, 16).toUpperCase())
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .platformFee(fee)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description("Provider payment " + internalReference)
                .build();

        transactions.save(tx);

        intent.setProviderReference(providerReference);
        intent.setStatus("COMPLETED");
        intent.setCompletedAt(LocalDateTime.now());
        intents.save(intent);
    }

    @Transactional
    public void markFailed(String internalReference, String reason) {
        PaymentIntent intent = intents.findByInternalReferenceForUpdate(internalReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found"));

        if ("COMPLETED".equals(intent.getStatus())) {
            return;
        }

        intent.setStatus("FAILED");
        intent.setFailureReason(reason);
        intents.save(intent);
    }
}
