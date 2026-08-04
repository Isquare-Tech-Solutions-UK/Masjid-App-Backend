package com.masjidapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${app.firebase.service-account-file:firebase/firebase-service-account.json}")
    private String serviceAccountFile;

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized.");
            return;
        }

        try {
            GoogleCredentials credentials = loadCredentials();
            if (credentials == null) {
                log.warn("Firebase not initialized: no credentials found in FIREBASE_SERVICE_ACCOUNT_JSON " +
                        "or file '{}'. Push notifications will be disabled.", serviceAccountFile);
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully.");

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }

    /**
     * Prefers FIREBASE_SERVICE_ACCOUNT_JSON (used in deployed environments via
     * Infisical) so that path is unaffected. Falls back to a local credentials
     * file for environments where the JSON was dropped on disk instead — a
     * missing or unreadable file is treated the same as "not configured"
     * rather than failing startup, since deploy never ships this file.
     */
    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(serviceAccountJson)) {
            log.info("Loading Firebase credentials from FIREBASE_SERVICE_ACCOUNT_JSON.");
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
        }

        Path path = Paths.get(serviceAccountFile);
        if (!Files.isRegularFile(path)) {
            return null;
        }

        log.info("Loading Firebase credentials from file '{}'.", path.toAbsolutePath());
        try (InputStream in = Files.newInputStream(path)) {
            return GoogleCredentials.fromStream(in);
        }
    }
}
