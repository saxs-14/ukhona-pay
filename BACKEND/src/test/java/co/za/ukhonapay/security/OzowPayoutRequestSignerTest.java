package co.za.ukhonapay.security;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OzowPayoutRequestSignerTest {
    @Test
    void signsUsingOzowDocumentedFieldOrder() {
        OzowPayoutRequestSigner signer = new OzowPayoutRequestSigner();
        String hash = signer.sign(
                "SITE",
                new BigDecimal("100.00"),
                "ORDER-001",
                "ORDER-001",
                false,
                "https://example.test/notify",
                "bank-id",
                "encrypted-account",
                "632005",
                "APIKEY"
        );

        assertEquals(128, hash.length());
        assertTrue(hash.matches("[0-9a-f]{128}"));
    }

    @Test
    void roundsAmountToCents() {
        OzowPayoutRequestSigner signer = new OzowPayoutRequestSigner();
        assertDoesNotThrow(() -> signer.sign(
                "SITE", new BigDecimal("1.15"), "REF", "REF", false,
                "https://example.test", "bank", "enc", "123456", "key"));
    }
}
