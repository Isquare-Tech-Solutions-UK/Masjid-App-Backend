package com.masjidapp.service.impl;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripeServiceImpl {

    @PostConstruct
    public void init() {
        Stripe.apiKey = "your_stripe_api_key";
    }

    public String createCheckoutSession(UUID donationId, String campaignId, String campaignTitle,
                                        DonationCreateRequest donationCreateRequest) throws StripeException {
        BigDecimal donationAmount = donationCreateRequest.getAmount();
        boolean coverFee = donationCreateRequest.isCoverFee();
        BigDecimal processingFee =
                donationAmount.multiply(new BigDecimal("0.029"))
                        .add(new BigDecimal("0.30"));

        SessionCreateParams.PaymentIntentData.Builder piBuilder = SessionCreateParams.PaymentIntentData.builder();
        piBuilder
                .putMetadata("campaignId", campaignId)
                .putMetadata("donationId", donationId.toString())
                .putMetadata("donorEmail", donationCreateRequest.getDonorEmail())
                .putMetadata("coverFee", String.valueOf(donationCreateRequest.isCoverFee()))
                .putMetadata("donationAmount", donationAmount.toString());

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(donationCreateRequest.getSuccessUrl())
                .setCancelUrl(donationCreateRequest.getCancelUrl());
        if (coverFee) {
            piBuilder.putMetadata("processingFee", processingFee.toString());
            sessionBuilder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("GBP")
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
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("GBP")
                                                        .setUnitAmount(donationAmount.multiply(new BigDecimal("100")).longValue())
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(campaignTitle)
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();
        Session session = Session.create(params);
        return session.getId();
    }

}
