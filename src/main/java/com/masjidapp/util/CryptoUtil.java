package com.masjidapp.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {

    private static final String ALGO = "AES";

    private static SecretKeySpec getKey() {
        byte[] decodedKey = Base64.getDecoder().decode(KeyLoader.loadKey());
        return new SecretKeySpec(decodedKey, ALGO);
    }

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());

            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, getKey());

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(decoded));

        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}