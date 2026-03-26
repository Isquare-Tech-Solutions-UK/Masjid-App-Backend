package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.dto.donation.DonationCreateRequest;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class StripeServiceImpl {

    private final StripeConfig stripeConfig;

    public Map<String, String> createCheckoutSession(UUID donationId, String campaignId, String campaignTitle,
                                                     String stripeAccountId,
                                                     DonationCreateRequest donationCreateRequest) throws StripeException {

        // Ensure the platform key is set (no CryptoUtil — env var is already plaintext)
        Stripe.apiKey = stripeConfig.getSecretKey();

        Map<String, String> checkoutSessionMap = new HashMap<>();
        BigDecimal donationAmount = donationCreateRequest.getAmount();
        boolean coverFee = donationCreateRequest.isCoverFee();

        // Gross-up formula: ensures masjid receives exactly the donation amount.
        // total = (donationAmount + fixedFee) / (1 - percentFee)
        // fee   = total - donationAmount
        // Example: £100 → total = (100 + 0.20) / 0.985 = £101.73, fee = £1.73
        // Stripe takes £101.73 × 1.5% + £0.20 = £1.73 → masjid gets exactly £100.
        BigDecimal one = BigDecimal.ONE;
        BigDecimal processingFee = donationAmount
                .add(stripeConfig.getFeeFixed())
                .divide(one.subtract(stripeConfig.getFeePercent()), 2, RoundingMode.HALF_UP)
                .subtract(donationAmount)
                .setScale(2, RoundingMode.HALF_UP);

        SessionCreateParams.PaymentIntentData.Builder piBuilder = SessionCreateParams.PaymentIntentData.builder()
                .putMetadata("campaignId", campaignId)
                .putMetadata("donationId", donationId.toString())
                .putMetadata("donorEmail", donationCreateRequest.getDonorEmail())
                .putMetadata("coverFee", String.valueOf(coverFee))
                .putMetadata("donationAmount", donationAmount.toString());

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setExpiresAt(Instant.now().plusSeconds(1800).getEpochSecond())
                .setSuccessUrl(donationCreateRequest.getSuccessUrl())
                .setCancelUrl(donationCreateRequest.getCancelUrl())
                .putMetadata("campaignId", campaignId)
                .putMetadata("donationId", donationId.toString())
                .putMetadata("donationAmount", donationAmount.toString())
                // Donation amount line item
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("gbp")
                                .setUnitAmount(donationAmount.multiply(new BigDecimal("100")).longValue())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(campaignTitle)
                                        .build())
                                .build())
                        .build());

        // Optionally add processing fee as a separate line item
        if (coverFee) {
            piBuilder.putMetadata("processingFee", processingFee.toString());
            checkoutSessionMap.put("processingFee", processingFee.toString());
            sessionBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("gbp")
                            .setUnitAmount(processingFee.multiply(new BigDecimal("100")).longValue())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Processing Fee")
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = sessionBuilder
                .setPaymentIntentData(piBuilder.build())
                .build();

        // Route payment directly to the masjid's connected Stripe account
        RequestOptions requestOptions = RequestOptions.builder()
                .setStripeAccount(stripeAccountId)
                .build();

        Session session = Session.create(params, requestOptions);
        log.info("Stripe session created: {} for account: {}", session.getId(), stripeAccountId);

        checkoutSessionMap.put("sessionId", session.getId());
        checkoutSessionMap.put("url", session.getUrl());
        checkoutSessionMap.put("paymentIntentId", session.getPaymentIntent());
        checkoutSessionMap.put("currency", session.getCurrency());

        return checkoutSessionMap;
    }
}
