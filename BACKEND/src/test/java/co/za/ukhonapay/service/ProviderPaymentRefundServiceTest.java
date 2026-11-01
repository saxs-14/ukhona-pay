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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderPaymentRefundServiceTest {

    @Mock PaymentRefundRepository refunds;
    @Mock PaymentIntentRepository intents;
    @Mock VendorRepository vendors;
    @Mock WalletRepository wallets;
    @Mock LedgerService ledger;
    @Mock LedgerAccountService ledgerAccounts;

    ProviderPaymentRefundService service;

    @BeforeEach
    void setUp() {
        service = new ProviderPaymentRefundService(refunds, intents, vendors, wallets, ledger, ledgerAccounts);
    }

    private PaymentIntent completedIntent(BigDecimal amount, BigDecimal refundedAmount) {
        PaymentIntent intent = new PaymentIntent();
        intent.setVendorId(9L);
        intent.setAmount(amount);
        intent.setStatus("COMPLETED");
        intent.setRefundedAmount(refundedAmount);
        return intent;
    }

    @Test
    void rejectsBlankProviderRefundReference() {
        assertThrows(IllegalArgumentException.class, () ->
                service.processCompletedRefund(" ", "PROV-TX-1", new BigDecimal("10.00"), "ZAR", "reason"));
        verifyNoInteractions(refunds, intents, ledger, wallets);
    }

    @Test
    void rejectsBlankProviderTransactionReference() {
        assertThrows(IllegalArgumentException.class, () ->
                service.processCompletedRefund("REFUND-1", " ", new BigDecimal("10.00"), "ZAR", "reason"));
        verifyNoInteractions(refunds, intents, ledger, wallets);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                service.processCompletedRefund("REFUND-1", "PROV-TX-1", BigDecimal.ZERO, "ZAR", "reason"));
        verifyNoInteractions(refunds, intents, ledger, wallets);
    }

    @Test
    void rejectsNonZarCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
                service.processCompletedRefund("REFUND-1", "PROV-TX-1", new BigDecimal("10.00"), "USD", "reason"));
        verifyNoInteractions(refunds, intents, ledger, wallets);
    }

    @Test
    void isIdempotentWhenRefundAlreadyCompleted() {
        PaymentRefund refund = new PaymentRefund();
        refund.setStatus("COMPLETED");
        when(refunds.findByProviderRefundReferenceForUpdate("REFUND-1")).thenReturn(Optional.of(refund));

        assertDoesNotThrow(() -> service.processCompletedRefund(
                "REFUND-1", "PROV-TX-1", new BigDecimal("10.00"), "ZAR", "reason"));

        verifyNoInteractions(intents, ledger, wallets);
    }

    @Test
    void throwsWhenPaymentIntentNotCompleted() {
        when(refunds.findByProviderRefundReferenceForUpdate("REFUND-1")).thenReturn(Optional.empty());
        PaymentIntent intent = completedIntent(new BigDecimal("100.00"), BigDecimal.ZERO);
        intent.setStatus("PENDING");
        when(intents.findByProviderReferenceForUpdate("PROV-TX-1")).thenReturn(Optional.of(intent));

        assertThrows(IllegalStateException.class, () -> service.processCompletedRefund(
                "REFUND-1", "PROV-TX-1", new BigDecimal("10.00"), "ZAR", "reason"));

        verifyNoInteractions(ledger, wallets);
    }

    @Test
    void throwsWhenPaymentIntentMissing() {
        when(refunds.findByProviderRefundReferenceForUpdate("REFUND-1")).thenReturn(Optional.empty());
        when(intents.findByProviderReferenceForUpdate("PROV-TX-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.processCompletedRefund(
                "REFUND-1", "PROV-TX-1", new BigDecimal("10.00"), "ZAR", "reason"));
    }

    @Test
    void rejectsRefundExceedingOriginalAmount() {
        when(refunds.findByProviderRefundReferenceForUpdate("REFUND-1")).thenReturn(Optional.empty());
        PaymentIntent intent = completedIntent(new BigDecimal("100.00"), new BigDecimal("60.00"));
        when(intents.findByProviderReferenceForUpdate("PROV-TX-1")).thenReturn(Optional.of(intent));

        assertThrows(IllegalArgumentException.class, () -> service.processCompletedRefund(
                "REFUND-1", "PROV-TX-1", new BigDecimal("50.00"), "ZAR", "reason"));

        verifyNoInteractions(ledger, wallets);
    }

    @Test
    void processesFullRefundAndReversesAutoAllocation() {
        when(refunds.findByProviderRefundReferenceForUpdate("REFUND-1")).thenReturn(Optional.empty());
        PaymentIntent intent = completedIntent(new BigDecimal("101.00"), BigDecimal.ZERO);
        when(intents.findByProviderReferenceForUpdate("PROV-TX-1")).thenReturn(Optional.of(intent));

        Vendor vendor = new Vendor();
        vendor.setId(9L);
        vendor.setUserId(5L);
        when(vendors.findById(9L)).thenReturn(Optional.of(vendor));

        // Wallet as it stands after the original settlement of a 101.00 payment:
        // net 100.00 split 90/5/5 across balance/savings/maintenance.
        Wallet wallet = Wallet.builder().userId(5L)
                .balance(new BigDecimal("90.00"))
                .savingsBalance(new BigDecimal("5.00"))
                .maintenanceBalance(new BigDecimal("5.00"))
                .cashbackBalance(BigDecimal.ZERO).currency("ZAR").build();
        when(wallets.findWithLockByUserId(5L)).thenReturn(Optional.of(wallet));
        when(ledgerAccounts.ensureVendorWalletAccount(9L, 5L)).thenReturn("VENDOR_WALLET_9");

        service.processCompletedRefund("REFUND-1", "PROV-TX-1", new BigDecimal("101.00"), "ZAR", "customer request");

        // Full refund of the original 101.00 payment reverses the full net 100.00 credit.
        assertEquals(BigDecimal.ZERO.setScale(2), wallet.getBalance());
        assertEquals(BigDecimal.ZERO.setScale(2), wallet.getSavingsBalance());
        assertEquals(BigDecimal.ZERO.setScale(2), wallet.getMaintenanceBalance());

        verify(ledger).post(eq("REFUND-REFUND-1"), eq("PROVIDER_REFUND"), eq("REFUND-1"), any(), any());
        verify(wallets).save(wallet);
        verify(refunds).save(any(PaymentRefund.class));

        assertEquals(new BigDecimal("101.00"), intent.getRefundedAmount());
        verify(intents).save(intent);
    }
}
