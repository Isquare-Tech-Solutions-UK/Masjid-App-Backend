package com.masjidapp.service;

import com.masjidapp.dto.request.StripeKeysUpdateRequest;
import com.masjidapp.dto.response.StripeSettingsResponse;

public interface StripeKeyService {

    /** Validate and store the charity's Stripe keys (secret + webhook secret encrypted at rest). */
    StripeSettingsResponse saveKeys(StripeKeysUpdateRequest request);

    /** Admin-facing connection status — never includes the secret or webhook secret. */
    StripeSettingsResponse getStatus();

    /** Remove all stored Stripe keys. Donations are disabled until reconnected. */
    void clearKeys();

    /** The publishable key for the mobile app to initialise the Stripe SDK. */
    String getPublishableKey();
}