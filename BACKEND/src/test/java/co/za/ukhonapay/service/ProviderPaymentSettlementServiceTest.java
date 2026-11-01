package co.za.ukhonapay.service;

import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import co.za.ukhonapay.repository.TransactionRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderPaymentSettlementServiceTest {

    @Mock PaymentIntentRepository intents;
    @Mock VendorRepository vendors;
    @Mock WalletRepository wallets;
    @Mock TransactionRepository transactions;
    @Mock LedgerService ledger;
    @Mock LedgerAccountService ledgerAccounts;

    ProviderPaymentSettlementService service;

    @BeforeEach
    void setUp() {
        service = new ProviderPaymentSettlementService(
                intents, vendors, wallets, transactions, ledger, ledgerAccounts);
    }

    private PaymentIntent pendingIntent(BigDecimal amount) {
        PaymentIntent intent = new PaymentIntent();
        intent.setInternalReference("REF-1");
        intent.setVendorId(9L);
        intent.setAmount(amount);
        intent.setStatus("PENDING");
        return intent;
    }

    @Test
    void settleSuccessful_rejectsAmountMismatch() {
        when(intents.findByInternalReferenceForUpdate("REF-1"))
                .thenReturn(Optional.of(pendingIntent(new BigDecimal("100.00"))));

        assertThrows(IllegalArgumentException.class, () ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("50.00"), "ZAR"));

        verifyNoInteractions(ledger, wallets, transactions);
    }

    @Test
    void settleSuccessful_rejectsWrongCurrency() {
        when(intents.findByInternalReferenceForUpdate("REF-1"))
                .thenReturn(Optional.of(pendingIntent(new BigDecimal("100.00"))));

        assertThrows(IllegalArgumentException.class, () ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("100.00"), "USD"));

        verifyNoInteractions(ledger, wallets, transactions);
    }

    @Test
    void settleSuccessful_isIdempotentWhenAlreadyCompleted() {
        PaymentIntent intent = pendingIntent(new BigDecimal("100.00"));
        intent.setStatus("COMPLETED");
        when(intents.findByInternalReferenceForUpdate("REF-1")).thenReturn(Optional.of(intent));

        assertDoesNotThrow(() ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("100.00"), "ZAR"));

        verifyNoInteractions(ledger, wallets, transactions);
    }

    @Test
    void settleSuccessful_rejectsWhenNotPending() {
        PaymentIntent intent = pendingIntent(new BigDecimal("100.00"));
        intent.setStatus("FAILED");
        when(intents.findByInternalReferenceForUpdate("REF-1")).thenReturn(Optional.of(intent));

        assertThrows(IllegalStateException.class, () ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("100.00"), "ZAR"));

        verifyNoInteractions(ledger, wallets, transactions);
    }

    @Test
    void settleSuccessful_rejectsWhenNetAmountTooSmallForPlatformFee() {
        // PLATFORM_FEE is a flat R1.00, so a R1.00 payment leaves nothing for the vendor.
        when(intents.findByInternalReferenceForUpdate("REF-1"))
                .thenReturn(Optional.of(pendingIntent(new BigDecimal("1.00"))));
        Vendor vendor = new Vendor();
        vendor.setId(9L);
        vendor.setUserId(5L);
        when(vendors.findById(9L)).thenReturn(Optional.of(vendor));
        when(wallets.findWithLockByUserId(5L)).thenReturn(Optional.of(
                Wallet.builder().userId(5L).balance(BigDecimal.ZERO)
                        .savingsBalance(BigDecimal.ZERO).maintenanceBalance(BigDecimal.ZERO)
                        .cashbackBalance(BigDecimal.ZERO).currency("ZAR").build()));

        assertThrows(IllegalArgumentException.class, () ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("1.00"), "ZAR"));

        verifyNoInteractions(ledger, transactions);
        verify(wallets, never()).save(any());
    }

    @Test
    void settleSuccessful_creditsVendorWalletAndCompletesIntent() {
        PaymentIntent intent = pendingIntent(new BigDecimal("101.00"));
        when(intents.findByInternalReferenceForUpdate("REF-1")).thenReturn(Optional.of(intent));

        Vendor vendor = new Vendor();
        vendor.setId(9L);
        vendor.setUserId(5L);
        when(vendors.findById(9L)).thenReturn(Optional.of(vendor));

        Wallet wallet = Wallet.builder().userId(5L).balance(BigDecimal.ZERO)
                .savingsBalance(BigDecimal.ZERO).maintenanceBalance(BigDecimal.ZERO)
                .cashbackBalance(BigDecimal.ZERO).currency("ZAR").build();
        when(wallets.findWithLockByUserId(5L)).thenReturn(Optional.of(wallet));
        when(ledgerAccounts.ensureVendorWalletAccount(9L, 5L)).thenReturn("VENDOR_WALLET_9");

        service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("101.00"), "ZAR");

        // amount 101.00 - PLATFORM_FEE 1.00 = net 100.00, split 90/5/5 across balance/savings/maintenance.
        assertEquals(new BigDecimal("90.00"), wallet.getBalance());
        assertEquals(new BigDecimal("5.00"), wallet.getSavingsBalance());
        assertEquals(new BigDecimal("5.00"), wallet.getMaintenanceBalance());

        verify(ledger).post(eq("LED-REF-1"), eq("PROVIDER_PAYMENT"), eq("PROV-1"), anyString(), any());
        verify(wallets).save(wallet);
        verify(transactions).save(any());
        verify(intents).save(intent);

        assertEquals("COMPLETED", intent.getStatus());
        assertEquals("PROV-1", intent.getProviderReference());
        assertNotNull(intent.getCompletedAt());
    }

    @Test
    void settleSuccessful_missingVendorThrows() {
        when(intents.findByInternalReferenceForUpdate("REF-1"))
                .thenReturn(Optional.of(pendingIntent(new BigDecimal("100.00"))));
        when(vendors.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.settleSuccessful("REF-1", "PROV-1", new BigDecimal("100.00"), "ZAR"));
    }

    @Test
    void markFailed_isIdempotentWhenAlreadyCompleted() {
        PaymentIntent intent = pendingIntent(new BigDecimal("100.00"));
        intent.setStatus("COMPLETED");
        when(intents.findByInternalReferenceForUpdate("REF-1")).thenReturn(Optional.of(intent));

        service.markFailed("REF-1", "provider declined");

        assertEquals("COMPLETED", intent.getStatus());
        verify(intents, never()).save(any());
    }

    @Test
    void markFailed_setsFailedStatusAndReason() {
        PaymentIntent intent = pendingIntent(new BigDecimal("100.00"));
        when(intents.findByInternalReferenceForUpdate("REF-1")).thenReturn(Optional.of(intent));

        service.markFailed("REF-1", "provider declined");

        assertEquals("FAILED", intent.getStatus());
        assertEquals("provider declined", intent.getFailureReason());
        verify(intents).save(intent);
    }
}
