package co.za.ukhonapay.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class OzowPayoutEncryptionService {

    public String encryptAccountNumber(String accountNumber, String merchantReference,
                                       java.math.BigDecimal amount, String encryptionKey) {
        try {
            long cents = amount.setScale(2, java.math.RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();

            String ivSource = (merchantReference + cents + encryptionKey).toLowerCase();
            String ivHex = sha512(ivSource).substring(0, 16);
            byte[] iv = ivHex.getBytes(StandardCharsets.UTF_8);

            String paddedKey = encryptionKey;
            while (paddedKey.length() < 32) paddedKey += encryptionKey;
            byte[] key = paddedKey.substring(0, 32).getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new javax.crypto.spec.IvParameterSpec(iv));
            return Base64.getEncoder().encodeToString(cipher.doFinal(
                    accountNumber.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt Ozow payout account number", e);
        }
    }

    private String sha512(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-512")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(128);
        for (byte b : digest) out.append(String.format("%02x", b));
        return out.toString();
    }
}
