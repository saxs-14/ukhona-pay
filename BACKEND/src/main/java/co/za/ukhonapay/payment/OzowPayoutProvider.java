package co.za.ukhonapay.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.security.OzowPayoutRequestSigner;
import co.za.ukhonapay.service.OzowPayoutEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OzowPayoutProvider implements PayoutProvider {
    private final String baseUrl;
    private final String apiKey;
    private final String siteCode;
    private final String notifyUrl;
    private final boolean rtc;
    private final OzowPayoutRequestSigner signer;
    private final OzowPayoutEncryptionService encryption;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OzowPayoutProvider(
            @Value("${ukhonapay.payments.ozow.payout-base-url:https://payoutsapi.ozow.com/v1}") String baseUrl,
            @Value("${ukhonapay.payments.ozow.payout-api-key:}") String apiKey,
            @Value("${ukhonapay.payments.ozow.site-code:}") String siteCode,
            @Value("${ukhonapay.payments.ozow.payout-notify-url:}") String notifyUrl,
            @Value("${ukhonapay.payments.ozow.payout-is-rtc:false}") boolean rtc,
            OzowPayoutRequestSigner signer,
            OzowPayoutEncryptionService encryption,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.siteCode = siteCode;
        this.notifyUrl = notifyUrl;
        this.rtc = rtc;
        this.signer = signer;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !siteCode.isBlank() && !notifyUrl.isBlank();
    }

    @Override
    public ProviderPayoutResponse requestPayout(String merchantReference, BigDecimal amount,
                                                 BankAccount bankAccount, String encryptionKey) {
        if (apiKey.isBlank() || siteCode.isBlank() || notifyUrl.isBlank()) {
            throw new IllegalStateException("Ozow payout configuration is incomplete");
        }
        if (bankAccount.getBankGroupId() == null || bankAccount.getBankGroupId().isBlank()) {
            throw new IllegalStateException("Saved bank account is missing its Ozow bankGroupId");
        }

        String encryptedAccount = encryption.encryptAccountNumber(
                bankAccount.getAccountNumber(), merchantReference, amount, encryptionKey);

        String customerBankReference = merchantReference;
        String hash = signer.sign(siteCode, amount, merchantReference, customerBankReference, rtc,
                notifyUrl, bankAccount.getBankGroupId(), encryptedAccount,
                bankAccount.getBranchCode(), apiKey);

        String json = """
                {
                  "siteCode":"%s",
                  "amount":%s,
                  "merchantReference":"%s",
                  "customerBankReference":"%s",
                  "isRtc":%s,
                  "notifyUrl":"%s",
                  "bankingDetails":{
                    "bankGroupId":"%s",
                    "accountNumber":"%s",
                    "branchCode":"%s"
                  },
                  "hashCheck":"%s"
                }
                """.formatted(
                esc(siteCode), amount.setScale(2), esc(merchantReference), esc(customerBankReference),
                rtc, esc(notifyUrl), esc(bankAccount.getBankGroupId()), esc(encryptedAccount),
                esc(bankAccount.getBranchCode()), hash);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/requestpayout"))
                    .timeout(Duration.ofSeconds(20))
                    .header("ApiKey", apiKey)
                    .header("SiteCode", siteCode)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Ozow payout API HTTP " + response.statusCode());
            }

            String payoutId = text(root, "payoutId");
            JsonNode status = root.path("payoutStatus");
            int providerStatus = status.path("status").asInt(0);
            int providerSubStatus = status.path("subStatus").asInt(0);
            String error = text(status, "errorMessage");

            boolean accepted = payoutId != null && !payoutId.isBlank() && (error == null || error.isBlank());
            return new ProviderPayoutResponse(accepted, payoutId, providerStatus, providerSubStatus, error);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ozow payout request was interrupted", e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Ozow payout request failed", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("\", "\\").replace(""", "\"");
    }
}
