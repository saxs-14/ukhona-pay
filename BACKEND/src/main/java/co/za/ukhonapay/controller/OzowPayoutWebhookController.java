package co.za.ukhonapay.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.PayoutNotificationEvent;
import co.za.ukhonapay.repository.PayoutNotificationEventRepository;
import co.za.ukhonapay.security.OzowPayoutHashVerifier;
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

    public OzowPayoutWebhookController(ObjectMapper objectMapper,
                                       OzowPayoutHashVerifier verifier,
                                       PayoutNotificationEventRepository repository) {
        this.objectMapper = objectMapper;
        this.verifier = verifier;
        this.repository = repository;
    }

    @PostMapping("/ozow-payout")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody String rawBody) {
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
            if (payoutId == null || merchantReference == null || siteCode == null || status < 0 || subStatus < 0) {
                return ResponseEntity.badRequest().build();
            }

            if (!verifier.verify(payoutId, siteCode, merchantReference, customerReference, status, subStatus, hashCheck)) {
                return ResponseEntity.status(401).build();
            }

            if (repository.findByProviderAndPayoutReferenceAndStatusAndSubStatus(
                    PROVIDER, payoutId, status, subStatus).isPresent()) {
                return ResponseEntity.ok().build();
            }

            PayoutNotificationEvent event = new PayoutNotificationEvent();
            event.setProvider(PROVIDER);
            event.setPayoutReference(payoutId);
            event.setMerchantReference(merchantReference);
            event.setStatus(status);
            event.setSubStatus(subStatus);
            event.setHashVerified(true);
            event.setProcessingStatus("RECEIVED");
            event.setPayload(rawBody);
            repository.save(event);

            // Settlement is intentionally not performed here yet.
            // The next phase will atomically map this verified notification
            // to the bank withdrawal and post the corresponding ledger entry.
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}