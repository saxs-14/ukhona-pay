package co.za.ukhonapay.service;

import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.payment.PayoutProvider;
import co.za.ukhonapay.payment.ProviderPayoutStatus;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankWithdrawalReconciliationService {
    private final BankWithdrawalRepository withdrawals;
    private final PayoutProvider payoutProvider;
    private final BankWithdrawalSettlementService settlementService;
    private final int lookbackHours;

    public BankWithdrawalReconciliationService(
            BankWithdrawalRepository withdrawals,
            PayoutProvider payoutProvider,
            BankWithdrawalSettlementService settlementService,
            @Value("${ukhonapay.payments.ozow.reconciliation-lookback-hours:48}") int lookbackHours) {
        this.withdrawals = withdrawals;
        this.payoutProvider = payoutProvider;
        this.settlementService = settlementService;
        this.lookbackHours = lookbackHours;
    }

    public int reconcilePending() {
        if (!payoutProvider.isConfigured()) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(lookbackHours);
        int processed = 0;
        List<BankWithdrawal> pending = withdrawals.findByStatusAndCreatedAtAfter(
                co.za.ukhonapay.model.enums.BankWithdrawalStatus.PENDING, cutoff);

        for (BankWithdrawal withdrawal : pending) {
            String providerReference = withdrawal.getProviderReference();
            if (providerReference == null || providerReference.isBlank()) continue;
            ProviderPayoutStatus status;
            try {
                status = payoutProvider.getPayoutStatus(providerReference);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (status.status() == 5 || status.status() == 4
                    || status.status() == 90 || status.status() == 99) {
                var event = new co.za.ukhonapay.model.PayoutNotificationEvent();
                event.setProvider("OZOW");
                event.setPayoutReference(providerReference);
                event.setMerchantReference(withdrawal.getReference());
                event.setStatus(status.status());
                event.setSubStatus(status.subStatus());
                event.setHashVerified(true);
                event.setProcessingStatus("PROCESSED");
                event.setPayload("reconciliation");
                settlementService.applyNotification(event);
                processed++;
            }
        }
        return processed;
    }
}
