package com.masjidapp.controller.stripe;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.service.impl.StripeWebhookServiceImpl;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
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

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/stripe")
@AllArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookServiceImpl stripeWebhookServiceImpl;
    private final StripeConfig stripeConfig;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Map<String, Boolean>> stripeNotification(@RequestBody String request,
                                                                  @RequestHeader("Stripe-Signature") String stripeSignature) throws EventDataObjectDeserializationException {
        Event event;
        try {
            log.info("Received Stripe webhook event body {}", StripeUtils.toSingleLineJson(request));
            log.info("Webhook received at {}", Instant.now());
            final String signingSecret = stripeConfig.getSigningSecret();
            event = Webhook.constructEvent(
                    request, stripeSignature, signingSecret
            );
            log.info("Stripe Event ID: {}", event.getId());
        } catch (SignatureVerificationException e) {
            throw new MARequestException("Stripe signature verification failure" + e);
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
            case "checkout.session.completed":
                stripeWebhookServiceImpl.handleSessionCompleted(stripeObject);
                break;
            case "checkout.session.expired":
                stripeWebhookServiceImpl.handleSessionExpired(stripeObject);
                break;
            default:
                log.info("Unhandled event type: [{}]", event.getType());
        }
    }

}
