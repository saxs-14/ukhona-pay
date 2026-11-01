package co.za.ukhonapay.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class OzowPayoutEncryptionServiceTest {
    @Test
    void encryptsAccountNumberWithoutReturningPlaintext() {
        OzowPayoutEncryptionService service = new OzowPayoutEncryptionService();
        String encrypted = service.encryptAccountNumber(
                "1234567890", "ORDER-001", new BigDecimal("100.00"), "unique-payout-key");

        assertNotEquals("1234567890", encrypted);
        assertFalse(encrypted.contains("1234567890"));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(encrypted));
    }
}
