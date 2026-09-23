package co.za.ukhonapay.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class OzowPayoutHashVerifier {
    private final String apiKey;
    private final String siteCode;

    public OzowPayoutHashVerifier(
            @Value("${ukhonapay.payments.ozow.payout-api-key:}") String apiKey,
            @Value("${ukhonapay.payments.ozow.site-code:}") String siteCode) {
        this.apiKey = apiKey;
        this.siteCode = siteCode;
    }

    public boolean verify(String payoutId,
                          String notificationSiteCode,
                          String merchantReference,
                          String customerMerchantReference,
                          int status,
                          int subStatus,
                          String hashCheck) {
        if (apiKey.isBlank() || siteCode.isBlank() || hashCheck == null || hashCheck.isBlank()) {
            return false;
        }
        if (!MessageDigest.isEqual(
                siteCode.getBytes(StandardCharsets.UTF_8),
                notificationSiteCode == null ? new byte[0] : notificationSiteCode.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        String input = value(payoutId)
                + value(notificationSiteCode)
                + value(merchantReference)
                + value(customerMerchantReference)
                + status
                + subStatus
                + apiKey;
        String expected = sha512(input.toLowerCase());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                hashCheck.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(128);
            for (byte b : digest) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is unavailable", e);
        }
    }
}