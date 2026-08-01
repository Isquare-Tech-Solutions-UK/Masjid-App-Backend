package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
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
        RequestOptions requestOptions = RequestOptions.builder().setApiKey(secretKey).build();

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
}