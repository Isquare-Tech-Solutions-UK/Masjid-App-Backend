package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "The charity's own standalone Stripe API keys")
public class StripeKeysUpdateRequest {

    @NotBlank
    @Schema(description = "Stripe publishable key (public — sent to the mobile app)",
            example = "pk_live_51ABc...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String publishableKey;

    @NotBlank
    @Schema(description = "Stripe secret key (stored encrypted at rest; never returned)",
            example = "sk_live_51ABc...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secretKey;

    @Schema(description = "Webhook signing secret from the charity's Stripe dashboard "
            + "(stored encrypted). Required for donation confirmation to work.",
            example = "whsec_...")
    private String webhookSecret;
}
