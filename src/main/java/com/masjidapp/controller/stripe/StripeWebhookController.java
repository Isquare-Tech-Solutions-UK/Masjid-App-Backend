package com.masjidapp.controller.stripe;

import com.masjidapp.exception.MARequestException;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.impl.StripeWebhookServiceImpl;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/stripe")
@AllArgsConstructor
@Slf4j
@Tag(name = "Stripe Webhooks", description = "Stripe event webhooks — internal use only")
@Hidden
public class StripeWebhookController {

    private final StripeWebhookServiceImpl stripeWebhookServiceImpl;
    private final SettingsRepository settingsRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Boolean>> stripeNotification(@RequestBody String request,
                                                                  @RequestHeader("Stripe-Signature") String stripeSignature) throws EventDataObjectDeserializationException {
        // The signing secret belongs to the charity's own Stripe account (stored encrypted).
        String signingSecret = settingsRepository.findAll().stream()
                .findFirst()
                .map(s -> s.getStripeWebhookSecret())
                .orElse(null);
        if (signingSecret == null) {
            log.warn("Stripe webhook received but no signing secret configured — rejecting");
            throw new MARequestException("Stripe webhook signing secret is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(request, stripeSignature, signingSecret);
            log.info("Stripe webhook verified: id={} type={}", event.getId(), event.getType());
        } catch (SignatureVerificationException e) {
            // Do not log the payload or signature — avoids leaking PII / aiding forgery.
            throw new MARequestException("Stripe signature verification failed");
        }
        processStripeEvent(event);
        return ResponseEntity.ok(Map.of("received", true));
    }

    private void processStripeEvent(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.warn("Stripe object deserialization failed, using unsafe fallback");
            stripeObject = dataObjectDeserializer.deserializeUnsafe();
        }
        handleStripeWebHookFlow(event, stripeObject);
    }

    private void handleStripeWebHookFlow(Event event, StripeObject stripeObject) {
        switch (event.getType()) {
            case "payment_intent.succeeded":
                stripeWebhookServiceImpl.handlePaymentIntentSucceeded(stripeObject);
                break;
            case "payment_intent.payment_failed":
                stripeWebhookServiceImpl.handlePaymentIntentFailed(stripeObject);
                break;
            case "payment_intent.canceled":
                stripeWebhookServiceImpl.handlePaymentIntentCanceled(stripeObject);
                break;
            default:
                log.info("Unhandled event type: [{}]", event.getType());
        }
    }

}