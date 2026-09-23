package co.za.ukhonapay.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.PaymentWebhookEvent;
import co.za.ukhonapay.payment.PaymentProvider;
import co.za.ukhonapay.payment.ProviderPaymentTransaction;
import co.za.ukhonapay.payment.ProviderRefund;
import co.za.ukhonapay.repository.PaymentWebhookEventRepository;
import co.za.ukhonapay.security.SvixWebhookVerifier;
import co.za.ukhonapay.service.ProviderPaymentRefundService;
import co.za.ukhonapay.service.ProviderPaymentSettlementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments/webhooks/ozow")
public class OzowWebhookController {
    private final ObjectMapper mapper;
    private final PaymentWebhookEventRepository events;
    private final ProviderPaymentSettlementService settlement;
    private final ProviderPaymentRefundService refundSettlement;
    private final PaymentProvider provider;
    private final String secret;

    public OzowWebhookController(
            ObjectMapper mapper,
            PaymentWebhookEventRepository events,
            ProviderPaymentSettlementService settlement,
            ProviderPaymentRefundService refundSettlement,
            PaymentProvider provider,
            @Value("${ukhonapay.payments.webhook-secret:}") String secret) {
        this.mapper = mapper;
        this.events = events;
        this.settlement = settlement;
        this.refundSettlement = refundSettlement;
        this.provider = provider;
        this.secret = secret;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "svix-id", required = false) String id,
            @RequestHeader(value = "svix-timestamp", required = false) String timestamp,
            @RequestHeader(value = "svix-signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (id == null || id.isBlank()) return ResponseEntity.badRequest().build();
        if (!SvixWebhookVerifier.verify(secret, id, timestamp, signature, rawBody)) {
            return ResponseEntity.status(401).build();
        }

        final JsonNode root;
        try {
            root = mapper.readTree(rawBody);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }

        String type = root.path("type").asText("");
        if (!"transaction.complete".equals(type) && !"refund.complete".equals(type)) {
            return ResponseEntity.ok().build();
        }

        try {
            events.claimIfNew("OZOW", id, type, true, rawBody);
            int claimed = events.claimForProcessing("OZOW", id, rawBody);
            if (claimed == 0) return ResponseEntity.ok().build();

            PaymentWebhookEvent event = events.findByProviderAndProviderEventId("OZOW", id)
                    .orElseThrow(() -> new IllegalStateException("Webhook event was not persisted"));
            JsonNode data = root.path("data");

            if ("transaction.complete".equals(type)) {
                processTransaction(data);
            } else {
                processRefund(data);
            }

            event.setProcessingStatus("PROCESSED");
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            events.save(event);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            events.findByProviderAndProviderEventId("OZOW", id).ifPresent(event -> {
                event.setProcessingStatus("FAILED");
                event.setErrorMessage(truncate(ex.getMessage()));
                event.setProcessedAt(LocalDateTime.now());
                events.save(event);
            });
            return ResponseEntity.status(500).build();
        }
    }

    private void processTransaction(JsonNode data) {
        String providerReference = firstNonBlank(
                data.path("TransactionId").asText(""),
                data.path("id").asText(""));
        if (providerReference.isBlank()) {
            throw new IllegalArgumentException("Transaction webhook has no provider reference");
        }

        ProviderPaymentTransaction transaction;
        if (data.hasNonNull("TransactionReference") && data.hasNonNull("Amount")) {
            String reference = data.path("TransactionReference").asText("");
            BigDecimal amount = new BigDecimal(data.path("Amount").asText("0"));
            String currency = firstNonBlank(data.path("CurrencyCode").asText(""), "ZAR");
            transaction = new ProviderPaymentTransaction(
                    providerReference,
                    reference,
                    amount,
                    currency,
                    firstNonBlank(data.path("Status").asText(""), "Error"),
                    firstNonBlank(data.path("StatusMessage").asText(""), data.path("reason").asText("")));
        } else {
            transaction = provider.getTransaction(providerReference);
        }

        if (transaction.merchantReference().isBlank()) {
            throw new IllegalArgumentException("Transaction webhook could not resolve merchant reference");
        }

        if ("Successful".equalsIgnoreCase(transaction.status())) {
            settlement.settleSuccessful(
                    transaction.merchantReference(),
                    transaction.providerReference(),
                    transaction.amount(),
                    transaction.currency());
        } else if ("Error".equalsIgnoreCase(transaction.status())) {
            settlement.markFailed(
                    transaction.merchantReference(),
                    transaction.reason().isBlank()
                            ? "Provider reported payment failure"
                            : transaction.reason());
        }
    }

    private void processRefund(JsonNode data) {
        String refundReference = data.path("id").asText("");
        if (refundReference.isBlank()) {
            throw new IllegalArgumentException("Refund webhook has no refund reference");
        }

        ProviderRefund refund = provider.getRefund(refundReference);
        if ("Complete".equalsIgnoreCase(refund.status())) {
            refundSettlement.processCompletedRefund(
                    refund.refundReference(),
                    refund.transactionReference(),
                    refund.amount(),
                    refund.currency(),
                    refund.reason());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) return "Webhook processing failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
