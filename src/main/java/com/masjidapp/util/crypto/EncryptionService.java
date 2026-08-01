package com.masjidapp.util.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive values stored at rest (e.g. the charity's
 * Stripe secret key and webhook signing secret).
 *
 * <p>The master key is supplied as a base64-encoded 256-bit key via the
 * {@code app.encryption.master-key} property (env var {@code ENCRYPTION_MASTER_KEY}).
 * It must NEVER be committed to the repo; in production it should come from a
 * secrets manager. Each encrypted value uses a fresh random 12-byte IV, which is
 * prepended to the ciphertext so decryption is self-contained.
 *
 * <p>A static reference is exposed so JPA {@link jakarta.persistence.AttributeConverter}
 * instances (which Hibernate may create outside the Spring context) can reach the
 * Spring-managed key. The reference is set at startup, before any entity is read or
 * written.
 */
@Component
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;      // 96-bit IV recommended for GCM
    private static final int TAG_LENGTH_BITS = 128;

    private static EncryptionService instance;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.encryption.master-key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "app.encryption.master-key (ENCRYPTION_MASTER_KEY) is not set — cannot encrypt secrets at rest");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.master-key must be a base64-encoded 128/192/256-bit AES key; got "
                            + (keyBytes.length * 8) + " bits");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    public static EncryptionService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EncryptionService has not been initialised yet");
        }
        return instance;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV so the value is self-describing: [12-byte IV][ciphertext+tag]
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}