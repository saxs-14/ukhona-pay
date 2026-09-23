package co.za.ukhonapay.security;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class OzowPayoutRequestSigner {

    public String sign(String siteCode,
                        BigDecimal amount,
                        String merchantReference,
                        String customerBankReference,
                        boolean isRtc,
                        String notifyUrl,
                        String bankGroupId,
                        String encryptedAccountNumber,
                        String branchCode,
                        String apiKey) {
        long cents = amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        String input = value(siteCode)
                + cents
                + value(merchantReference)
                + value(customerBankReference)
                + Boolean.toString(isRtc)
                + value(notifyUrl)
                + value(bankGroupId)
                + value(encryptedAccountNumber)
                + value(branchCode)
                + value(apiKey);

        return sha512(input.toLowerCase());
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
        } catch (Exception e) {
            throw new IllegalStateException("SHA-512 is unavailable", e);
        }
    }
}
