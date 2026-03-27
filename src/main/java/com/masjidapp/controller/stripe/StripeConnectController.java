package com.masjidapp.controller.stripe;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.StripeStatusResponse;
import com.masjidapp.service.StripeConnectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/admin/settings/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Connect", description = "Connect the masjid's Stripe account via OAuth to accept donations")
@SecurityRequirement(name = "bearerAuth")
public class StripeConnectController {

    private final StripeConnectService stripeConnectService;

    @Value("${app.stripe.oauth-redirect-uri}")
    private String oauthRedirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/connect")
    @Operation(
            summary = "Get Stripe OAuth URL",
            description = "Returns the Stripe OAuth authorization URL. Redirect the admin to this URL to begin connecting their Stripe account."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> connect() {
        String oauthUrl = stripeConnectService.getOAuthUrl(oauthRedirectUri);
        return ResponseEntity.ok(ApiResponse.success(Map.of("oauthUrl", oauthUrl)));
    }

    @GetMapping("/callback")
    @Operation(
            summary = "Stripe OAuth callback",
            description = "Handles the OAuth redirect from Stripe. Exchanges the authorization code for the connected account ID."
    )
    public RedirectView callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {
        if (error != null) {
            log.warn("Stripe OAuth denied: {} — {}", error, errorDescription);
            return new RedirectView(frontendUrl + "/settings?stripe=cancelled");
        }
        stripeConnectService.handleOAuthCallback(code, oauthRedirectUri);
        return new RedirectView(frontendUrl + "/settings?stripe=connected");
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get Stripe connection status",
            description = "Returns the current Stripe account status — whether connected, onboarding complete, and payouts enabled."
    )
    public ResponseEntity<ApiResponse<StripeStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(stripeConnectService.getStatus()));
    }

    @DeleteMapping("/disconnect")
    @Operation(
            summary = "Disconnect Stripe account",
            description = "Removes the Stripe account link from the masjid. Donations will be disabled until reconnected."
    )
    public ResponseEntity<ApiResponse<Void>> disconnect() {
        stripeConnectService.disconnect();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
