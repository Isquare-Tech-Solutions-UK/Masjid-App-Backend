package com.masjidapp.service.impl;

import com.masjidapp.dto.request.StripeKeysUpdateRequest;
import com.masjidapp.dto.response.StripeSettingsResponse;
import com.masjidapp.entity.MasjidSettings;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.StripeKeyService;
import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeKeyServiceImpl implements StripeKeyService {

    private final SettingsRepository settingsRepository;

    private MasjidSettings getSettings() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Masjid settings not found"));
    }

    @Override
    @Transactional
    public StripeSettingsResponse saveKeys(StripeKeysUpdateRequest request) {
        String publishableKey = trimToNull(request.getPublishableKey());
        String secretKey = trimToNull(request.getSecretKey());
        String webhookSecret = trimToNull(request.getWebhookSecret());

        if (publishableKey == null || !publishableKey.startsWith("pk_")) {
            throw new MARequestException("Publishable key must start with 'pk_'");
        }
        if (secretKey == null || !(secretKey.startsWith("sk_") || secretKey.startsWith("rk_"))) {
            throw new MARequestException("Secret key must start with 'sk_' (or 'rk_' for a restricted key)");
        }
        if (webhookSecret != null && !webhookSecret.startsWith("whsec_")) {
            throw new MARequestException("Webhook signing secret must start with 'whsec_'");
        }

        String pubMode = modeOf(publishableKey);
        String secMode = modeOf(secretKey);
        if (pubMode != null && secMode != null && !pubMode.equals(secMode)) {
            throw new MARequestException(
                    "Publishable key (" + pubMode + ") and secret key (" + secMode + ") are from different modes");
        }

        // Prove the secret key actually works before persisting it.
        verifySecretKey(secretKey);

        MasjidSettings settings = getSettings();
        settings.setStripePublishableKey(publishableKey);
        settings.setStripeSecretKey(secretKey);
        if (webhookSecret != null) {
            settings.setStripeWebhookSecret(webhookSecret);
        }
        settings.setStripeKeyMode(secMode != null ? secMode : pubMode);
        settings.setStripeKeysUpdatedAt(Instant.now());
        settingsRepository.save(settings);

        log.info("Stripe keys updated (mode={}, webhookConfigured={})",
                settings.getStripeKeyMode(), settings.getStripeWebhookSecret() != null);
        return toResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public StripeSettingsResponse getStatus() {
        return toResponse(getSettings());
    }

    @Override
    @Transactional
    public void clearKeys() {
        MasjidSettings settings = getSettings();
        settings.setStripePublishableKey(null);
        settings.setStripeSecretKey(null);
        settings.setStripeWebhookSecret(null);
        settings.setStripeKeyMode(null);
        settings.setStripeKeysUpdatedAt(Instant.now());
        settingsRepository.save(settings);
        log.info("Stripe keys cleared — donations disabled until reconnected");
    }

    @Override
    @Transactional(readOnly = true)
    public String getPublishableKey() {
        String key = getSettings().getStripePublishableKey();
        if (key == null) {
            throw new MARequestException("Stripe is not configured for this masjid yet");
        }
        return key;
    }

    private void verifySecretKey(String secretKey) {
        try {
            RequestOptions options = RequestOptions.builder().setApiKey(secretKey).build();
            Balance.retrieve(options);
        } catch (StripeException e) {
            // Do not log the key or the raw exception detail (may echo the key).
            throw new MARequestException("Stripe rejected the secret key. Please check the key and try again.");
        }
    }

    private StripeSettingsResponse toResponse(MasjidSettings settings) {
        return StripeSettingsResponse.builder()
                .connected(settings.getStripeSecretKey() != null)
                .publishableKey(settings.getStripePublishableKey())
                .keyMode(settings.getStripeKeyMode())
                .webhookConfigured(settings.getStripeWebhookSecret() != null)
                .keysUpdatedAt(settings.getStripeKeysUpdatedAt())
                .build();
    }

    private static String modeOf(String key) {
        if (key.contains("_live_")) return "live";
        if (key.contains("_test_")) return "test";
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}