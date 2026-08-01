package com.masjidapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Admin-facing view of the charity's Stripe connection. Deliberately excludes the
 * secret key and webhook signing secret — those are write-only and never returned.
 */
@Data
@Builder
@Schema(description = "Status of the charity's direct Stripe connection")
public class StripeSettingsResponse {

    @Schema(description = "Whether a secret key is configured and donations can be taken")
    private boolean connected;

    @Schema(description = "The publishable key (safe to expose)")
    private String publishableKey;

    @Schema(description = "'test' or 'live', derived from the key prefix")
    private String keyMode;

    @Schema(description = "Whether a webhook signing secret is configured")
    private boolean webhookConfigured;

    @Schema(description = "When the keys were last updated")
    private Instant keysUpdatedAt;
}