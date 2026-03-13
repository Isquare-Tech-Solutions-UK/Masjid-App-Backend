package com.masjidapp.controller.stripe;

import com.masjidapp.exception.MARequestException;
import com.masjidapp.service.impl.StripeServiceImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
@AllArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeServiceImpl stripeServiceImpl;
    private final AwsSecretsService awsSecretsService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void stripeNotification(@RequestBody String request,
                                   @RequestHeader("Stripe-Signature") String stripeSignature) {
        Event event;
        try {
            log.info("Received Stripe webhook event body {} with signature: {}", request, stripeSignature);
            final String signingSecret = awsSecretsService.getSecrets().getStripeSigningSecret();
            event = Webhook.constructEvent(
                    request, stripeSignature, signingSecret
            );
        } catch (SignatureVerificationException e) {
            throw new MARequestException("Stripe signature verification failure" + e);
        }
        stripeServiceImpl.processStripeEvent(event);
    }
}
