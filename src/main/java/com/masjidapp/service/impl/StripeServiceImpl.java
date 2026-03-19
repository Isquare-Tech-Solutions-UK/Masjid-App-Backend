package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.util.CryptoUtil;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        Stripe.apiKey = CryptoUtil.decrypt(stripeConfig.getApiKey());
    }

    public Map<String,String> createCheckoutSession(UUID donationId, String campaignId, String campaignTitle,
                                        DonationCreateRequest donationCreateRequest) throws StripeException {
        Map<String, String> checkoutSessionMap = new HashMap<>();
        BigDecimal donationAmount = donationCreateRequest.getAmount();
        boolean coverFee = donationCreateRequest.isCoverFee();
        BigDecimal processingFee =
                donationAmount.multiply(new BigDecimal("0.029"))
                        .add(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

        SessionCreateParams.PaymentIntentData.Builder piBuilder = SessionCreateParams.PaymentIntentData.builder();
        piBuilder
                .putMetadata("campaignId", campaignId)
                .putMetadata("donationId", donationId.toString())
                .putMetadata("donorEmail", donationCreateRequest.getDonorEmail())
                .putMetadata("coverFee", String.valueOf(donationCreateRequest.isCoverFee()))
                .putMetadata("donationAmount", donationAmount.toString());

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setExpiresAt(Instant.now().plusSeconds(1800).getEpochSecond())
                .setSuccessUrl(donationCreateRequest.getSuccessUrl())
                .setCancelUrl(donationCreateRequest.getCancelUrl());
        if (coverFee) {
            piBuilder.putMetadata("processingFee", processingFee.toString());
            checkoutSessionMap.put("processingFee", processingFee.toString());
            sessionBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("gbp")
                                            .setUnitAmount(processingFee.multiply(new BigDecimal("100")).longValue())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Processing Fee")
                                                            .build())
                                            .build())
                            .build());
        }
        SessionCreateParams params =
                sessionBuilder
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(donationCreateRequest.getSuccessUrl())
                        .setCancelUrl(donationCreateRequest.getCancelUrl())
                        .setPaymentIntentData(piBuilder.build())
                        .putMetadata("campaignId", campaignId)
                        .putMetadata("donationId", donationId.toString())
                        .putMetadata("donationAmount", donationAmount.toString())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("gbp")
                                                        .setUnitAmount(donationAmount.multiply(new BigDecimal("100")).longValue())
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(campaignTitle)
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();
        Session session = Session.create(params);
        checkoutSessionMap.put("sessionId", session.getId());
        checkoutSessionMap.put("url", session.getUrl());
        checkoutSessionMap.put("paymentIntentId", session.getPaymentIntent());
        checkoutSessionMap.put("currency", session.getCurrency());

        return checkoutSessionMap;
    }

}
