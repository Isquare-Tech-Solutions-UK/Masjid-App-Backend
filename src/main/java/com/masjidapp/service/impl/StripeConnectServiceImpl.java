package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.dto.response.StripeStatusResponse;
import com.masjidapp.entity.MasjidSettings;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.StripeConnectService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.oauth.TokenResponse;
import com.stripe.net.OAuth;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeConnectServiceImpl implements StripeConnectService {

    private final StripeConfig stripeConfig;
    private final SettingsRepository settingsRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
    }

    private MasjidSettings getSettings() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Masjid settings not found"));
    }

    @Override
    public String getOAuthUrl(String redirectUri) {
        return UriComponentsBuilder
                .fromHttpUrl("https://connect.stripe.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", stripeConfig.getClientId())
                .queryParam("scope", "read_write")
                .queryParam("redirect_uri", redirectUri)
                .toUriString();
    }

    @Override
    @Transactional
    public void handleOAuthCallback(String code, String redirectUri) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("code", code);
            params.put("grant_type", "authorization_code");

            TokenResponse token = OAuth.token(params, null);
            String accountId = token.getStripeUserId();

            MasjidSettings settings = getSettings();
            settings.setStripeAccountId(accountId);
            settings.setStripeConnectedAt(Instant.now());
            settingsRepository.save(settings);

            log.info("Stripe OAuth connected: accountId={}", accountId);
        } catch (StripeException e) {
            throw new MARequestException("Failed to complete Stripe OAuth: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StripeStatusResponse getStatus() {
        MasjidSettings settings = getSettings();
        String accountId = settings.getStripeAccountId();

        if (accountId == null) {
            return StripeStatusResponse.builder()
                    .connected(false)
                    .onboardingComplete(false)
                    .acceptingDonations(false)
                    .payoutsEnabled(false)
                    .build();
        }

        try {
            Account account = Account.retrieve(accountId);
            boolean onboardingComplete = Boolean.TRUE.equals(account.getDetailsSubmitted());
            return StripeStatusResponse.builder()
                    .accountId(accountId)
                    .connected(true)
                    .onboardingComplete(onboardingComplete)
                    .acceptingDonations(Boolean.TRUE.equals(account.getChargesEnabled()))
                    .payoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()))
                    .build();
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe account status for {}: {}", accountId, e.getMessage());
            return StripeStatusResponse.builder()
                    .accountId(accountId)
                    .connected(true)
                    .onboardingComplete(false)
                    .acceptingDonations(false)
                    .payoutsEnabled(false)
                    .build();
        }
    }

    @Override
    @Transactional
    public void disconnect() {
        MasjidSettings settings = getSettings();
        log.info("Disconnecting Stripe account: {}", settings.getStripeAccountId());
        settings.setStripeAccountId(null);
        settings.setStripeConnectedAt(null);
        settingsRepository.save(settings);
    }
}
