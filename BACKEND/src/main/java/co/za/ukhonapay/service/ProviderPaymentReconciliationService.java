package co.za.ukhonapay.service;

import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.payment.PaymentProvider;
import co.za.ukhonapay.payment.ProviderPaymentRequestStatus;
import co.za.ukhonapay.payment.ProviderPaymentTransaction;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderPaymentReconciliationService {
    private final PaymentIntentRepository intents;
    private final PaymentProvider provider;
    private final ProviderPaymentSettlementService settlement;
    private final int lookbackHours;
    private final String configuredProvider;

    public ProviderPaymentReconciliationService(
            PaymentIntentRepository intents,
            PaymentProvider provider,
            ProviderPaymentSettlementService settlement,
            @Value("$"+"{ukhonapay.payments.ozow.payin-reconciliation-lookback-hours:48}") int lookbackHours,
            @Value("$"+"{ukhonapay.payments.provider:}") String configuredProvider) {
        this.intents = intents;
        this.provider = provider;
        this.settlement = settlement;
        this.lookbackHours = lookbackHours;
        this.configuredProvider = configuredProvider;
    }

    @Scheduled(fixedDelayString = "$"+"{ukhonapay.payments.ozow.payin-reconciliation-delay-ms:300000}")
    public void scheduledReconcile() {
        reconcilePending();
    }

    public int reconcilePending() {
        if (!"OZOW".equalsIgnoreCase(configuredProvider) || !"OZOW".equalsIgnoreCase(provider.name())) return 0;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(lookbackHours);
        List<PaymentIntent> pending = intents.findByStatusAndCreatedAtAfter("PENDING", cutoff);
        int processed = 0;

        for (PaymentIntent intent : pending) {
            try {
                String paymentReference = intent.getProviderPaymentReference();
                if (paymentReference == null || paymentReference.isBlank()) paymentReference = intent.getProviderReference();
                if (paymentReference == null || paymentReference.isBlank()) continue;

                List<ProviderPaymentTransaction> providerTransactions =
                        provider.getTransactions(paymentReference, intent.getCreatedAt().toLocalDate(), LocalDate.now());

                ProviderPaymentTransaction matching = providerTransactions.stream()
                        .filter(t -> intent.getInternalReference().equals(t.merchantReference()))
                        .filter(t -> intent.getAmount().compareTo(t.amount()) == 0)
                        .filter(t -> intent.getCurrency().equalsIgnoreCase(t.currency()))
                        .findFirst().orElse(null);

                if (matching != null) {
                    if ("Successful".equalsIgnoreCase(matching.status())) {
                        settlement.settleSuccessful(intent.getInternalReference(), matching.providerReference(), matching.amount(), matching.currency());
                        processed++;
                    } else if ("Error".equalsIgnoreCase(matching.status()) || "Refunded".equalsIgnoreCase(matching.status())) {
                        settlement.markFailed(intent.getInternalReference(),
                                matching.reason().isBlank() ? "Provider reported payment failure: " + matching.status() : matching.reason());
                        processed++;
                    }
                    continue;
                }

                ProviderPaymentRequestStatus paymentStatus = provider.getPaymentStatus(paymentReference);
                if ("Expired".equalsIgnoreCase(paymentStatus.status())) {
                    settlement.markFailed(intent.getInternalReference(),
                            paymentStatus.reason().isBlank() ? "Provider payment request expired" : paymentStatus.reason());
                    processed++;
                }
            } catch (RuntimeException ignored) {
                // Provider/network outages must never convert pending money into failure.
            }
        }
        return processed;
    }
}
