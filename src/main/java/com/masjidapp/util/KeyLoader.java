package com.masjidapp.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class KeyLoader {

    private static byte[] cachedKey;

    public static byte[] loadKey() {
        if (cachedKey != null) return cachedKey;

        try (InputStream is = KeyLoader.class
                .getClassLoader()
                .getResourceAsStream("secret.key");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String base64Key = reader.readLine();
            cachedKey = java.util.Base64.getDecoder().decode(base64Key);
            return cachedKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load key", e);
        }
    }
}