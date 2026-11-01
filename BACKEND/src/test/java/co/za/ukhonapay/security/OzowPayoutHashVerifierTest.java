package co.za.ukhonapay.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class OzowPayoutHashVerifierTest {
    private OzowPayoutHashVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new OzowPayoutHashVerifier("test-api-key", "SITE123");
    }

    @Test
    void acceptsValidNotificationHash() {
        String hash = sha512("payout-1site123merchant-1customer-11100test-api-key");
        assertTrue(verifier.verify("payout-1", "SITE123", "merchant-1", "customer-1", 1, 100, hash));
    }

    @Test
    void rejectsWrongSiteCode() {
        String hash = sha512("payout-1othermerchant-1customer-11100test-api-key");
        assertFalse(verifier.verify("payout-1", "OTHER", "merchant-1", "customer-1", 1, 100, hash));
    }

    @Test
    void rejectsTamperedHash() {
        String hash = sha512("payout-1site123merchant-1customer-1100test-api-key");
        assertFalse(verifier.verify("payout-1", "SITE123", "merchant-1", "customer-1", 2, 100, hash));
    }

    private static String sha512(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
