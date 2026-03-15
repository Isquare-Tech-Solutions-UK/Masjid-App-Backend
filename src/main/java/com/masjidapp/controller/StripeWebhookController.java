package com.masjidapp.controller;

import com.masjidapp.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Stripe webhook event receiver")
public class StripeWebhookController {

    private final StripeService stripeService;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping(value = "/stripe", consumes = "application/json")
    @Operation(summary = "Stripe webhook",
            description = "Receives and processes Stripe events (account.updated, etc.)")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // Verify signature if webhook secret is configured
        if (StringUtils.hasText(webhookSecret)) {
            try {
                Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } catch (SignatureVerificationException e) {
                log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
                return ResponseEntity.badRequest().body("Invalid signature");
            }
        }

        Event event;
        try {
            event = Event.GSON.fromJson(payload, Event.class);
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        log.info("Stripe webhook received: type={}", event.getType());

        switch (event.getType()) {
            case "account.updated" -> stripeService.syncAccountStatus();
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("OK");
    }
}
