package co.za.ukhonapay.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.model.BankWithdrawal;
import co.za.ukhonapay.repository.BankAccountRepository;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.security.OzowPayoutVerificationHashVerifier;
import co.za.ukhonapay.service.PayoutSecretCryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/webhooks")
public class OzowPayoutVerificationWebhookController {
    private final ObjectMapper objectMapper;
    private final BankWithdrawalRepository withdrawals;
    private final BankAccountRepository bankAccounts;
    private final OzowPayoutVerificationHashVerifier verifier;
    private final PayoutSecretCryptoService secretCrypto;
    private final String accessToken;
    private final String siteCode;
    private final String notifyUrl;
    private final boolean rtc;

    public OzowPayoutVerificationWebhookController(
            ObjectMapper objectMapper,
            BankWithdrawalRepository withdrawals,
            BankAccountRepository bankAccounts,
            OzowPayoutVerificationHashVerifier verifier,
            PayoutSecretCryptoService secretCrypto,
            @Value("${ukhonapay.payments.ozow.payout-verification-access-token:}") String accessToken,
            @Value("${ukhonapay.payments.ozow.site-code:}") String siteCode,
            @Value("${ukhonapay.payments.ozow.payout-notify-url:}") String notifyUrl,
            @Value("${ukhonapay.payments.ozow.payout-is-rtc:false}") boolean rtc) {
        this.objectMapper = objectMapper;
        this.withdrawals = withdrawals;
        this.bankAccounts = bankAccounts;
        this.verifier = verifier;
        this.secretCrypto = secretCrypto;
        this.accessToken = accessToken;
        this.siteCode = siteCode;
        this.notifyUrl = notifyUrl;
        this.rtc = rtc;
    }

    @PostMapping("/ozow-payout-verification")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestHeader(value = "AccessToken", required = false) String providedToken,
            @RequestBody String rawBody) {
        try {
            if (accessToken.isBlank() || providedToken == null
                    || !MessageDigest.isEqual(
                    accessToken.getBytes(StandardCharsets.UTF_8),
                    providedToken.getBytes(StandardCharsets.UTF_8))) {
                return ResponseEntity.status(401).body(Map.of(
                        "isVerified", false,
                        "reason", "Unauthorized"
                ));
            }

            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode banking = root.path("bankingDetails");
            String payoutId = text(root, "payoutId");
            String incomingSiteCode = text(root, "siteCode");
            BigDecimal amount = root.path("amount").decimalValue();
            String merchantReference = text(root, "merchantReference");
            String customerBankReference = text(root, "customerBankReference");
            boolean incomingRtc = root.path("isRtc").asBoolean(false);
            String incomingNotifyUrl = text(root, "notifyUrl");
            String bankGroupId = text(banking, "bankGroupId");
            String encryptedAccount = text(banking, "accountNumber");
            String branchCode = text(banking, "branchCode");
            String hashCheck = text(root, "hashCheck");

            if (payoutId == null || merchantReference == null || incomingSiteCode == null
                    || amount.signum() <= 0 || bankGroupId == null || encryptedAccount == null
                    || branchCode == null) {
                return verified(false, payoutId, "Invalid verification payload");
            }

            if (!siteCode.equals(incomingSiteCode) || !rtcEquals(incomingRtc)
                    || !notifyUrl.equals(incomingNotifyUrl)) {
                return verified(false, payoutId, "Payout configuration mismatch");
            }

            BankWithdrawal withdrawal = withdrawals.findByReference(merchantReference).orElse(null);
            if (withdrawal == null || !payoutId.equals(withdrawal.getProviderReference())) {
                return verified(false, payoutId, "Payout was not initiated by this system");
            }
            if (withdrawal.getAmount().compareTo(amount.setScale(2)) != 0) {
                return verified(false, payoutId, "Payout amount mismatch");
            }

            BankAccount account = bankAccounts.findById(withdrawal.getBankAccountId()).orElse(null);
            if (account == null || !bankGroupId.equals(account.getBankGroupId())
                    || !branchCode.equals(account.getBranchCode())) {
                return verified(false, payoutId, "Banking details do not match the initiated payout");
            }

            String expectedCustomerReference = withdrawal.getReference();
            if (!expectedCustomerReference.equals(customerBankReference)) {
                return verified(false, payoutId, "Customer bank reference mismatch");
            }

            if (!verifier.verify(payoutId, incomingSiteCode, amount, merchantReference,
                    customerBankReference, incomingRtc, incomingNotifyUrl, bankGroupId,
                    encryptedAccount, branchCode, hashCheck)) {
                return verified(false, payoutId, "Hash verification failed");
            }

            String encryptionKey = secretCrypto.decrypt(withdrawal.getProviderEncryptionKey());
            return ResponseEntity.ok(Map.of(
                    "payoutId", payoutId,
                    "isVerified", true,
                    "accountNumberDecryptionKey", encryptionKey,
                    "reason", ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "payoutId", "",
                    "isVerified", false,
                    "reason", "Verification could not be completed"
            ));
        }
    }

    private boolean rtcEquals(boolean incoming) {
        return incoming == rtc;
    }

    private ResponseEntity<Map<String, Object>> verified(boolean ok, String payoutId, String reason) {
        return ResponseEntity.ok(Map.of(
                "payoutId", payoutId == null ? "" : payoutId,
                "isVerified", ok,
                "reason", reason
        ));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
