package co.za.ukhonapay.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.PayoutNotificationEvent;
import co.za.ukhonapay.repository.PayoutNotificationEventRepository;
import co.za.ukhonapay.security.OzowPayoutHashVerifier;
import co.za.ukhonapay.service.BankWithdrawalSettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments/webhooks")
public class OzowPayoutWebhookController {
    private static final String PROVIDER = "OZOW";

    private final ObjectMapper objectMapper;
    private final OzowPayoutHashVerifier verifier;
    private final PayoutNotificationEventRepository repository;
    private final BankWithdrawalSettlementService settlementService;

    public OzowPayoutWebhookController(
            ObjectMapper objectMapper,
            OzowPayoutHashVerifier verifier,
            PayoutNotificationEventRepository repository,
            BankWithdrawalSettlementService settlementService) {
        this.objectMapper = objectMapper;
        this.verifier = verifier;
        this.repository = repository;
        this.settlementService = settlementService;
    }

    @PostMapping("/ozow-payout")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody String rawBody) {
        String eventId = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String payoutId = text(root, "payoutId");
            String siteCode = text(root, "siteCode");
            String merchantReference = text(root, "merchantReference");
            String customerReference = text(root, "customerMerchantReference");
            String hashCheck = text(root, "hashCheck");
            JsonNode statusNode = root.path("payoutStatus");
            if (statusNode.isMissingNode()) return ResponseEntity.badRequest().build();

            int status = statusNode.path("status").asInt(-1);
            int subStatus = statusNode.path("subStatus").asInt(-1);
            if (payoutId == null || merchantReference == null || siteCode == null
                    || status < 0 || subStatus < 0) {
                return ResponseEntity.badRequest().build();
            }

            if (!verifier.verify(payoutId, siteCode, merchantReference, customerReference,
                    status, subStatus, hashCheck)) {
                return ResponseEntity.status(401).build();
            }

            eventId = payoutId + ":" + status + ":" + subStatus;
            int inserted = repository.claimIfNew(
                    PROVIDER, payoutId, merchantReference, status, subStatus, rawBody);
            if (inserted == 0) {
                return ResponseEntity.ok().build();
            }

            int claimed = repository.claimForProcessing(
                    PROVIDER, payoutId, status, subStatus, rawBody);
            if (claimed == 0) {
                return ResponseEntity.ok().build();
            }

            PayoutNotificationEvent event = repository
                    .findByProviderAndPayoutReferenceAndStatusAndSubStatus(
                            PROVIDER, payoutId, status, subStatus)
                    .orElseThrow(() -> new IllegalStateException("Payout event was not persisted"));

            settlementService.applyNotification(event);
            event.setProcessingStatus("PROCESSED");
            event.setProcessedAt(LocalDateTime.now());
            event.setErrorMessage(null);
            repository.save(event);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            if (eventId != null) {
                try {
                    String[] parts = eventId.split(":", 3);
                    if (parts.length == 3) {
                        repository.findByProviderAndPayoutReferenceAndStatusAndSubStatus(
                                        PROVIDER, parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                                .ifPresent(event -> {
                                    event.setProcessingStatus("FAILED");
                                    event.setErrorMessage(truncate(e.getMessage()));
                                    event.setProcessedAt(LocalDateTime.now());
                                    repository.save(event);
                                });
                    }
                } catch (Exception ignored) {
                    // Preserve the original webhook failure response.
                }
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "Payout notification processing failed";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
