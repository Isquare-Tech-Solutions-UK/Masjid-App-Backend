package com.masjidapp.controller.admin;

import com.masjidapp.dto.request.UpdateMasjidSettingsRequest;
import com.masjidapp.dto.request.UpdatePaymentSettingsRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.MasjidSettingsResponse;
import com.masjidapp.dto.response.MessageResponse;
import com.masjidapp.service.SettingsService;
import com.masjidapp.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Settings", description = "Endpoints for managing masjid settings, services, facilities, and payment configuration")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AdminSettingsController {

    private final SettingsService settingsService;
    private final StripeService stripeService;

    @GetMapping
    @Operation(summary = "Get masjid settings",
            description = "Retrieve current masjid settings including address, contact, services, facilities, and payment info")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> getSettings() {
        log.info("GET /admin/settings");
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings()));
    }

    @PostMapping
    @Operation(summary = "Update masjid settings",
            description = "Update masjid details including name, about, address, contact, services, facilities, and capacity")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateMasjidSettingsRequest request) {
        log.info("POST /admin/settings - updating masjid settings");
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateSettings(request)));
    }

    @PostMapping("/payment")
    @Operation(summary = "Update payment settings",
            description = "Update bank and payment configuration")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> updatePaymentSettings(
            @Valid @RequestBody UpdatePaymentSettingsRequest request) {
        log.info("POST /admin/settings/payment - updating payment settings");
        return ResponseEntity.ok(ApiResponse.success(settingsService.updatePaymentSettings(request)));
    }

    // ─── Stripe Connect ────────────────────────────────────────────────────────

    @PostMapping("/stripe/connect")
    @Operation(summary = "Connect Stripe account",
            description = "Creates a Stripe Express account and returns the hosted onboarding URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> connectStripe(
            @RequestParam @NotBlank String returnUrl,
            @RequestParam @NotBlank String refreshUrl) {
        log.info("POST /admin/settings/stripe/connect");
        String onboardingUrl = stripeService.createOnboardingLink(returnUrl, refreshUrl);
        return ResponseEntity.ok(ApiResponse.success(Map.of("onboardingUrl", onboardingUrl)));
    }

    @GetMapping("/stripe/status")
    @Operation(summary = "Get Stripe account status",
            description = "Fetches live status from Stripe and returns the current connection state")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse.StripeResponse>> getStripeStatus() {
        log.info("GET /admin/settings/stripe/status");
        stripeService.syncAccountStatus();
        MasjidSettingsResponse settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings.getStripe()));
    }

    @DeleteMapping("/stripe/disconnect")
    @Operation(summary = "Disconnect Stripe account",
            description = "Removes the Stripe connection from masjid settings")
    public ResponseEntity<ApiResponse<MessageResponse>> disconnectStripe() {
        log.info("DELETE /admin/settings/stripe/disconnect");
        stripeService.disconnectAccount();
        return ResponseEntity.ok(ApiResponse.success(new MessageResponse("Stripe account disconnected successfully.")));
    }
}
