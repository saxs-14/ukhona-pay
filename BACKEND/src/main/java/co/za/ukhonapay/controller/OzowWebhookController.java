package co.za.ukhonapay.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.PaymentWebhookEvent;
import co.za.ukhonapay.repository.PaymentWebhookEventRepository;
import co.za.ukhonapay.security.SvixWebhookVerifier;
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
    private final String secret;

    public OzowWebhookController(
            ObjectMapper mapper,
            PaymentWebhookEventRepository events,
            ProviderPaymentSettlementService settlement,
            @Value("${ukhonapay.payments.webhook-secret:}") String secret) {
        this.mapper = mapper;
        this.events = events;
        this.settlement = settlement;
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
        if (!"transaction.complete".equals(type)) return ResponseEntity.ok().build();

        try {
            events.claimIfNew("OZOW", id, type, true, rawBody);

            // The unique insert records the delivery; this conditional update
            // atomically assigns processing ownership to exactly one worker.
            int claimedForProcessing = events.claimForProcessing("OZOW", id, rawBody);
            if (claimedForProcessing == 0) {
                PaymentWebhookEvent existing =
                        events.findByProviderAndProviderEventId("OZOW", id).orElse(null);

                if (existing == null) return ResponseEntity.status(500).build();

                // Another worker is already processing this delivery, or it has
                // already reached a terminal state. Both cases are safe to
                // acknowledge because the event is idempotently persisted.
                return ResponseEntity.ok().build();
            }

            PaymentWebhookEvent event = events.findByProviderAndProviderEventId("OZOW", id)
                    .orElseThrow(() -> new IllegalStateException("Webhook event was not persisted"));

            JsonNode data = root.path("data");
            String reference = firstNonBlank(
                    data.path("TransactionReference").asText(""),
                    data.path("merchantReference").asText(""));
            String providerReference = firstNonBlank(
                    data.path("TransactionId").asText(""),
                    data.path("id").asText(""));
            String status = firstNonBlank(
                    data.path("Status").asText(""),
                    data.path("status").asText(""));

            if (reference.isBlank()) throw new IllegalArgumentException("Webhook has no payment intent reference");
            if (providerReference.isBlank()) throw new IllegalArgumentException("Webhook has no provider reference");

            if ("Successful".equalsIgnoreCase(status)) {
                BigDecimal amount = new BigDecimal(data.path("Amount").asText("0"));
                settlement.settleSuccessful(reference, providerReference, amount, "ZAR");
            } else if ("Error".equalsIgnoreCase(status)) {
                settlement.markFailed(reference, firstNonBlank(
                        data.path("Reason").asText(""),
                        data.path("reason").asText(""),
                        "Provider reported payment failure"));
            } else {
                event.setProcessingStatus("IGNORED");
                event.setProcessedAt(LocalDateTime.now());
                events.save(event);
                return ResponseEntity.ok().build();
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

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) return "Webhook processing failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
