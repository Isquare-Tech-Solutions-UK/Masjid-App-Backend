package com.masjidapp.service.impl;

import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.DonationService;
import com.stripe.exception.StripeException;
import com.stripe.model.BalanceTransaction;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
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
    private final SettingsRepository settingsRepository;

    private static final String DONATION_ID = "donationId";

    public void handlePaymentIntentSucceeded(StripeObject stripeObject) {
        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        if (!"succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
            log.warn("payment_intent.succeeded received but status={} — ignoring", paymentIntent.getStatus());
            return;
        }

        String donationId = paymentIntent.getMetadata().get(DONATION_ID);
        String campaignId = paymentIntent.getMetadata().get("campaignId");
        String donationAmountRaw = paymentIntent.getMetadata().get("donationAmount");
        if (donationId == null || campaignId == null || donationAmountRaw == null) {
            log.warn("PaymentIntent {} is missing donation metadata — cannot process", paymentIntent.getId());
            return;
        }
        BigDecimal donationAmount = new BigDecimal(donationAmountRaw);

        log.info("Payment confirmed — donationId={} campaignId={}", donationId, campaignId);

        // Mark donation complete and update campaign raised amount (idempotent).
        donationService.updateCampaignDonationStatus(donationId, campaignId, donationAmount);

        // Reconcile the actual Stripe fee from the balance transaction.
        fetchAndStoreActualFee(paymentIntent, donationId);
    }

    public void handlePaymentIntentFailed(StripeObject stripeObject) {
        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        String donationId = paymentIntent.getMetadata().get(DONATION_ID);
        if (donationId != null) {
            log.info("Payment failed — donationId={}", donationId);
            donationService.updateDonationStatus(donationId);
        }
    }

    private void fetchAndStoreActualFee(PaymentIntent paymentIntent, String donationId) {
        try {
            String secretKey = settingsRepository.findAll().stream()
                    .findFirst()
                    .map(s -> s.getStripeSecretKey())
                    .orElse(null);

            if (secretKey == null || paymentIntent.getLatestCharge() == null) {
                log.warn("Cannot fetch actual fee — secret key or latest charge missing for donation {}", donationId);
                return;
            }

            RequestOptions requestOptions = RequestOptions.builder().setApiKey(secretKey).build();

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
            // Non-critical — donation is already marked complete, fee update is best-effort.
            log.error("Failed to fetch actual Stripe fee for donation {}: {}", donationId, e.getMessage());
        }
    }
}