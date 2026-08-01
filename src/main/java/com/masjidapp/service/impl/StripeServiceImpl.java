package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class StripeServiceImpl {

    private final StripeConfig stripeConfig;

    /**
     * Creates a PaymentIntent directly on the charity's own Stripe account. The donor always
     * covers Stripe's processing fee (mandatory gross-up), so the charity receives the full
     * donation amount. Returns the data the mobile app needs to present the Stripe Payment Sheet.
     */
    public Map<String, String> createPaymentIntent(UUID donationId, String campaignId, String campaignTitle,
                                                    String secretKey, BigDecimal donationAmount) throws StripeException {

        // Gross-up so the charity nets exactly the donation amount:
        //   total = (donationAmount + fixedFee) / (1 - percentFee)
        //   fee   = total - donationAmount
        // Example: £100 → total = (100 + 0.20) / 0.985 = £101.73, fee = £1.73.
        BigDecimal one = BigDecimal.ONE;
        BigDecimal processingFee = donationAmount
                .add(stripeConfig.getFeeFixed())
                .divide(one.subtract(stripeConfig.getFeePercent()), 2, RoundingMode.HALF_UP)
                .subtract(donationAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCharged = donationAmount.add(processingFee);

        long amountInPence = totalCharged.multiply(new BigDecimal("100")).longValueExact();

        // Scope this API call to the charity's own secret key (thread-safe; no global Stripe.apiKey).
        // Idempotency key (unique per donation) makes PaymentIntent creation safe to retry without
        // creating duplicate intents — Stripe's recommended practice for all POST/creation calls.
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(secretKey)
                .setIdempotencyKey("pi_donation_" + donationId)
                .build();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInPence)
                .setCurrency("gbp")
                .setDescription(campaignTitle)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("donationId", donationId.toString())
                .putMetadata("campaignId", campaignId)
                .putMetadata("campaignTitle", campaignTitle)
                .putMetadata("donationAmount", donationAmount.toString())
                .putMetadata("processingFee", processingFee.toString())
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);
        log.info("PaymentIntent created: {} for donation {}", paymentIntent.getId(), donationId);

        Map<String, String> result = new HashMap<>();
        result.put("paymentIntentId", paymentIntent.getId());
        // The Stripe SDK on mobile consumes this as its clientSecret; exposed to the app as "paymentToken".
        result.put("clientSecret", paymentIntent.getClientSecret());
        result.put("currency", paymentIntent.getCurrency());
        result.put("processingFee", processingFee.toString());
        result.put("totalCharged", totalCharged.toString());
        return result;
    }

    /**
     * Cancels the PaymentIntent for an abandoned donation and returns its final status.
     * If the PaymentIntent has already succeeded or is processing (the money moved), it is NOT
     * canceled — the returned status lets the caller reconcile instead of wrongly failing a paid
     * donation. Already-canceled intents are treated as a no-op.
     */
    public String cancelPaymentIntent(String paymentIntentId, String secretKey) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder().setApiKey(secretKey).build();
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, requestOptions);
        String status = paymentIntent.getStatus();

        if ("succeeded".equals(status) || "processing".equals(status) || "canceled".equals(status)) {
            return status; // terminal or in-flight — cannot / should not cancel
        }

        PaymentIntentCancelParams params = PaymentIntentCancelParams.builder()
                .setCancellationReason(PaymentIntentCancelParams.CancellationReason.ABANDONED)
                .build();
        PaymentIntent canceled = paymentIntent.cancel(params, requestOptions);
        log.info("PaymentIntent canceled: {} (status={})", canceled.getId(), canceled.getStatus());
        return canceled.getStatus();
    }
}