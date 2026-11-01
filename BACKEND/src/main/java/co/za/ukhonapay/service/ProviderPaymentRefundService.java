package co.za.ukhonapay.service;

import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.model.PaymentRefund;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import co.za.ukhonapay.repository.PaymentRefundRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderPaymentRefundService {
    private final PaymentRefundRepository refunds;
    private final PaymentIntentRepository intents;
    private final VendorRepository vendors;
    private final WalletRepository wallets;
    private final LedgerService ledger;
    private final LedgerAccountService ledgerAccounts;

    public ProviderPaymentRefundService(
            PaymentRefundRepository refunds,
            PaymentIntentRepository intents,
            VendorRepository vendors,
            WalletRepository wallets,
            LedgerService ledger,
            LedgerAccountService ledgerAccounts) {
        this.refunds = refunds;
        this.intents = intents;
        this.vendors = vendors;
        this.wallets = wallets;
        this.ledger = ledger;
        this.ledgerAccounts = ledgerAccounts;
    }

    @Transactional
    public void processCompletedRefund(
            String providerRefundReference,
            String providerTransactionReference,
            BigDecimal amount,
            String currency,
            String reason) {

        if (providerRefundReference == null || providerRefundReference.isBlank()) {
            throw new IllegalArgumentException("Provider refund reference is required");
        }
        if (providerTransactionReference == null || providerTransactionReference.isBlank()) {
            throw new IllegalArgumentException("Provider transaction reference is required");
        }
        if (amount == null || amount.signum() <= 0 || !"ZAR".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Refund must be a positive ZAR amount");
        }

        PaymentRefund refund = refunds.findByProviderRefundReferenceForUpdate(providerRefundReference).orElse(null);
        if (refund != null && "COMPLETED".equals(refund.getStatus())) {
            return;
        }

        PaymentIntent intent = intents.findByProviderReferenceForUpdate(providerTransactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent for refunded transaction not found"));

        if (!"COMPLETED".equals(intent.getStatus())) {
            throw new IllegalStateException("Only completed payment intents can be refunded");
        }

        BigDecimal alreadyRefunded = intent.getRefundedAmount() == null ? BigDecimal.ZERO : intent.getRefundedAmount();
        BigDecimal newRefunded = alreadyRefunded.add(amount);
        if (newRefunded.compareTo(intent.getAmount()) > 0) {
            throw new IllegalArgumentException("Provider refund exceeds the original payment amount");
        }

        Vendor vendor = vendors.findById(intent.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        Wallet wallet = wallets.findWithLockByUserId(vendor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor wallet not found"));

        BigDecimal originalFee = WalletService.PLATFORM_FEE.min(intent.getAmount());
        BigDecimal feeReversal = originalFee.multiply(
                amount.divide(intent.getAmount(), 8, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        if (newRefunded.compareTo(intent.getAmount()) == 0) {
            feeReversal = originalFee.subtract(
                    originalFee.multiply(alreadyRefunded.divide(intent.getAmount(), 8, RoundingMode.HALF_UP))
                            .setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal netReversal = amount.subtract(feeReversal);
        if (netReversal.signum() <= 0) {
            throw new IllegalArgumentException("Refund is too small after platform fee allocation");
        }

        String vendorAccount = ledgerAccounts.ensureVendorWalletAccount(vendor.getId(), vendor.getUserId());

        ledger.post(
                "REFUND-" + providerRefundReference,
                "PROVIDER_REFUND",
                providerRefundReference,
                "Provider refund " + providerRefundReference,
                List.of(
                        new LedgerService.Entry(vendorAccount, "DEBIT", netReversal),
                        new LedgerService.Entry("PLATFORM_FEE_REVENUE_ZAR", "DEBIT", feeReversal),
                        new LedgerService.Entry("PAYMENT_CLEARING_ZAR", "CREDIT", amount)
                ));

        WalletService.reverseAutoAllocation(wallet, netReversal);
        wallets.save(wallet);

        if (refund == null) {
            refund = new PaymentRefund();
            refund.setProviderRefundReference(providerRefundReference);
            refund.setProviderTransactionReference(providerTransactionReference);
            refund.setPaymentIntentId(intent.getId());
            refund.setAmount(amount);
            refund.setCurrency("ZAR");
        }
        refund.setStatus("COMPLETED");
        refund.setReason(reason);
        refund.setCompletedAt(LocalDateTime.now());
        refunds.save(refund);

        intent.setRefundedAmount(newRefunded);
        intents.save(intent);
    }
}
