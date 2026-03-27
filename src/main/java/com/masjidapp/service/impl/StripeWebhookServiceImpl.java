package com.masjidapp.service.impl;

import com.masjidapp.config.StripeConfig;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.DonationService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
@AllArgsConstructor
public class StripeWebhookServiceImpl {

    private final DonationService donationService;
    private final CampaignDonationQueryService campaignDonationQueryService;
    private final StripeConfig stripeConfig;
    private final SettingsRepository settingsRepository;

    private static final String DONATION_ID = "donationId";

    public void handleSessionCompleted(StripeObject stripeObject) {
        Session session = (Session) stripeObject;
        if (!session.getStatus().equalsIgnoreCase("complete") ||
                !session.getPaymentStatus().equalsIgnoreCase("paid")) {
            log.warn("checkout.session.completed received but payment not confirmed — status={}, paymentStatus={}",
                    session.getStatus(), session.getPaymentStatus());
            return;
        }

        String donationId = session.getMetadata().get(DONATION_ID);
        String campaignId = session.getMetadata().get("campaignId");
        BigDecimal donationAmount = new BigDecimal(session.getMetadata().get("donationAmount"));

        log.info("Payment confirmed — donationId={} campaignId={}", donationId, campaignId);

        // Mark donation complete and update campaign raised amount
        donationService.updateCampaignDonationStatus(donationId, campaignId, donationAmount);

        // Fetch actual Stripe fee from balance transaction
        fetchAndStoreActualFee(session, donationId);
    }

    private void fetchAndStoreActualFee(Session session, String donationId) {
        try {
            Stripe.apiKey = stripeConfig.getSecretKey();

            String stripeAccountId = settingsRepository.findAll().stream()
                    .findFirst()
                    .map(s -> s.getStripeAccountId())
                    .orElse(null);

            if (stripeAccountId == null || session.getPaymentIntent() == null) {
                log.warn("Cannot fetch actual fee — stripeAccountId or paymentIntent missing for donation {}", donationId);
                return;
            }

            RequestOptions requestOptions = RequestOptions.builder()
                    .setStripeAccount(stripeAccountId)
                    .build();

            // PaymentIntent → latest charge → balance transaction
            PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent(), requestOptions);
            Charge charge = Charge.retrieve(paymentIntent.getLatestCharge(), requestOptions);
            BalanceTransaction bt = BalanceTransaction.retrieve(charge.getBalanceTransaction(), requestOptions);

            // fee is in pence — convert to pounds
            BigDecimal actualFee = BigDecimal.valueOf(bt.getFee())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            String paymentMethod = charge.getPaymentMethodDetails() != null
                    ? charge.getPaymentMethodDetails().getType()
                    : null;

            log.info("Actual Stripe fee for donation {}: £{} (payment method: {})", donationId, actualFee, paymentMethod);

            donationService.updateActualStripeFee(donationId, actualFee, paymentMethod);

        } catch (StripeException e) {
            // Non-critical — donation is already marked complete, fee update is best-effort
            log.error("Failed to fetch actual Stripe fee for donation {}: {}", donationId, e.getMessage());
        }
    }

    public void handleSessionExpired(StripeObject stripeObject) {
        Session session = (Session) stripeObject;
        if (session.getStatus().equalsIgnoreCase("expired") &&
                session.getPaymentStatus().equalsIgnoreCase("unpaid")) {
            log.info("Session expired — donationId={}", session.getMetadata().get(DONATION_ID));
            donationService.updateDonationStatus(session.getMetadata().get(DONATION_ID));
        }
    }

}
