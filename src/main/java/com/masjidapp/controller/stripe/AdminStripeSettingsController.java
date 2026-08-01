package com.masjidapp.controller.stripe;

import com.masjidapp.dto.request.StripeKeysUpdateRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.StripeSettingsResponse;
import com.masjidapp.service.StripeKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/settings/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Settings", description = "Configure the charity's own Stripe account keys to accept donations")
@SecurityRequirement(name = "bearerAuth")
public class AdminStripeSettingsController {

    private final StripeKeyService stripeKeyService;

    @PutMapping("/keys")
    @Operation(
            summary = "Save Stripe API keys",
            description = "Store the charity's own publishable + secret keys (and optional webhook signing secret). "
                    + "The secret key is validated against Stripe, then encrypted at rest. It is never returned."
    )
    public ResponseEntity<ApiResponse<StripeSettingsResponse>> saveKeys(
            @Valid @RequestBody StripeKeysUpdateRequest request) {
        log.info("PUT /admin/settings/stripe/keys - updating Stripe keys");
        return ResponseEntity.ok(ApiResponse.success(stripeKeyService.saveKeys(request)));
    }

    @GetMapping
    @Operation(
            summary = "Get Stripe connection status",
            description = "Returns whether Stripe is configured, the publishable key, and the key mode. "
                    + "Never returns the secret key or webhook signing secret."
    )
    public ResponseEntity<ApiResponse<StripeSettingsResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(stripeKeyService.getStatus()));
    }

    @DeleteMapping("/keys")
    @Operation(
            summary = "Remove Stripe keys",
            description = "Deletes the stored Stripe keys. Donations are disabled until keys are configured again."
    )
    public ResponseEntity<ApiResponse<Void>> clearKeys() {
        stripeKeyService.clearKeys();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}