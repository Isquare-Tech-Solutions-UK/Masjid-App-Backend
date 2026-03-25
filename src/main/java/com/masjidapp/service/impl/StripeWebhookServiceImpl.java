package com.masjidapp.service.impl;

import com.masjidapp.exception.MARequestException;
import com.masjidapp.service.DonationService;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Slf4j
@AllArgsConstructor
public class StripeWebhookServiceImpl {

    private final DonationService donationService;
    private final CampaignDonationQueryService campaignDonationQueryService;
    private static final String DONATION_ID = "donationId";

    public void handleSessionCompleted(StripeObject stripeObject) {
        Session session = (Session) stripeObject;
        if (session.getStatus().equalsIgnoreCase("complete") &&
                session.getPaymentStatus().equalsIgnoreCase("paid")
        ) {
            log.info("donation-id :: {} campaign-id :: {}", session.getMetadata().get(DONATION_ID), session.getMetadata().get("campaignId"));
            donationService.updateCampaignDonationStatus(session.getMetadata().get(DONATION_ID),
                    session.getMetadata().get("campaignId"), new BigDecimal(session.getMetadata().get("donationAmount")));
        } else {
            log.warn("checkout.session.completed received but payment not confirmed — status={}, paymentStatus={}",
                    session.getStatus(), session.getPaymentStatus());
        }
    }

    public void handleSessionExpired(StripeObject stripeObject) {
        Session session = (Session) stripeObject;
        if (session.getStatus().equalsIgnoreCase("expired") &&
                session.getPaymentStatus().equalsIgnoreCase("unpaid")
        ) {
            log.info("donation-id :: {}", session.getMetadata().get(DONATION_ID));
            donationService.updateDonationStatus(session.getMetadata().get(DONATION_ID));
        }
    }

}
