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
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    @PostConstruct
    public void initializeFirebase() {
        if (!StringUtils.hasText(serviceAccountJson)) {
            log.warn("Firebase not initialized: FIREBASE_SERVICE_ACCOUNT_JSON is not set. " +
                    "Push notifications will be disabled.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized.");
            return;
        }

        try {
            ByteArrayInputStream serviceAccountStream = new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8));

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(serviceAccountStream);

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
}
