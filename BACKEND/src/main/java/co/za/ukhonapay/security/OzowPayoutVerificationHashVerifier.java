package co.za.ukhonapay.security;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class OzowPayoutVerificationHashVerifier {
    private final String apiKey;

    public OzowPayoutVerificationHashVerifier(
            @org.springframework.beans.factory.annotation.Value("${ukhonapay.payments.ozow.payout-api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean verify(String payoutId, String siteCode, BigDecimal amount,
                          String merchantReference, String customerBankReference,
                          boolean isRtc, String notifyUrl, String bankGroupId,
                          String encryptedAccountNumber, String branchCode, String hashCheck) {
        if (apiKey.isBlank() || hashCheck == null || hashCheck.isBlank()) return false;
        long cents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        String input = value(payoutId) + value(siteCode) + cents + value(merchantReference)
                + value(customerBankReference) + Boolean.toString(isRtc) + value(notifyUrl)
                + value(bankGroupId) + value(encryptedAccountNumber) + value(branchCode) + apiKey;
        String expected = sha512(input.toLowerCase());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                hashCheck.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    private String value(String value) { return value == null ? "" : value; }

    private String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(128);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-512 is unavailable", e);
        }
    }
}
