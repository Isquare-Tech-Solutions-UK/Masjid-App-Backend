package com.masjidapp.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;

public class KeyGeneratorUtil {

    public static void base64Generator() {
        try (InputStream is = KeyGeneratorUtil.class
                .getClassLoader()
                .getResourceAsStream("raw.key");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String rawKey = reader.readLine();
            String base64Key = Base64.getEncoder().encodeToString(rawKey.getBytes());

            System.out.println("Base64 Key  : " + base64Key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load key", e);
        }
    }

}
