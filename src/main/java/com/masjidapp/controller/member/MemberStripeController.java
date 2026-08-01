package com.masjidapp.controller.member;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.service.StripeKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/member/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Stripe", description = "Stripe configuration for the mobile app")
@SecurityRequirement(name = "apiKeyAuth")
public class MemberStripeController {

    private final StripeKeyService stripeKeyService;

    @GetMapping("/publishable-key")
    @Operation(
            summary = "Get Stripe publishable key",
            description = "Returns the charity's Stripe publishable key so the mobile app can initialise "
                    + "the Stripe SDK and present the Payment Sheet."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublishableKey() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("publishableKey", stripeKeyService.getPublishableKey())));
    }
}