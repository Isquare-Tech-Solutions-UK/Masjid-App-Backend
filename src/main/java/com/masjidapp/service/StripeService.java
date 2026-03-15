package com.masjidapp.service;

public interface StripeService {

    /**
     * Creates a Stripe Express account (if not already connected) and returns
     * the hosted onboarding URL for the masjid admin to complete setup.
     */
    String createOnboardingLink(String returnUrl, String refreshUrl);

    /**
     * Fetches live account status from Stripe and syncs it to the DB.
     * Called from the webhook and on-demand from the status endpoint.
     */
    void syncAccountStatus();

    /**
     * Removes the Stripe account connection from the masjid settings.
     */
    void disconnectAccount();
}
