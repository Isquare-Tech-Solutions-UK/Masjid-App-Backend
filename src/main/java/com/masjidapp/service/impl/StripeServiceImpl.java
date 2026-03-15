package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.entity.MasjidSettings;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements StripeService {

    private final SettingsRepository settingsRepository;
    private final StripeConfig stripeConfig;

    @Override
    @Transactional
    public String createOnboardingLink(String returnUrl, String refreshUrl) {
        if (!stripeConfig.isConfigured()) {
            throw new IllegalStateException("Stripe is not configured. Add STRIPE_SECRET_KEY to environment.");
        }

        MasjidSettings settings = getSettings();
        String accountId = settings.getStripeAccountId();

        try {
            if (!StringUtils.hasText(accountId)) {
                // Create a new Express account
                AccountCreateParams accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("GB")
                        .setCapabilities(AccountCreateParams.Capabilities.builder()
                                .setCardPayments(AccountCreateParams.Capabilities.CardPayments.builder()
                                        .setRequested(true).build())
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                        .setRequested(true).build())
                                .build())
                        .build();

                Account account = Account.create(accountParams);
                accountId = account.getId();

                settings.setStripeAccountId(accountId);
                settingsRepository.save(settings);
                log.info("Stripe Express account created: accountId={}", accountId);
            }

            // Create an Account Link for onboarding (or re-onboarding)
            AccountLink link = AccountLink.create(AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setReturnUrl(returnUrl)
                    .setRefreshUrl(refreshUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build());

            log.info("Stripe onboarding link created for accountId={}", accountId);
            return link.getUrl();

        } catch (StripeException e) {
            log.error("Stripe error creating onboarding link: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Stripe onboarding link: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void syncAccountStatus() {
        MasjidSettings settings = getSettings();

        if (!StringUtils.hasText(settings.getStripeAccountId())) {
            log.debug("Stripe sync skipped: no account connected.");
            return;
        }

        try {
            Account account = Account.retrieve(settings.getStripeAccountId());

            settings.setStripeOnboardingComplete(Boolean.TRUE.equals(account.getDetailsSubmitted()));
            settings.setStripeAcceptingDonations(Boolean.TRUE.equals(account.getChargesEnabled()));
            settings.setStripePayoutsEnabled(Boolean.TRUE.equals(account.getPayoutsEnabled()));
            settingsRepository.save(settings);

            log.info("Stripe status synced: accountId={}, acceptingDonations={}, payoutsEnabled={}",
                    settings.getStripeAccountId(),
                    settings.isStripeAcceptingDonations(),
                    settings.isStripePayoutsEnabled());

        } catch (StripeException e) {
            log.error("Stripe error syncing account status: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void disconnectAccount() {
        MasjidSettings settings = getSettings();
        String accountId = settings.getStripeAccountId();

        settings.setStripeAccountId(null);
        settings.setStripeOnboardingComplete(false);
        settings.setStripeAcceptingDonations(false);
        settings.setStripePayoutsEnabled(false);
        settingsRepository.save(settings);

        log.info("Stripe account disconnected: accountId={}", accountId);
    }

    private MasjidSettings getSettings() {
        return settingsRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Masjid settings not found"));
    }
}
