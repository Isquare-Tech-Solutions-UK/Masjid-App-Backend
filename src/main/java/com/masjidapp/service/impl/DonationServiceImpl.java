package com.masjidapp.service.impl;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.Donation;
import com.masjidapp.entity.DonationStatus;
import com.masjidapp.entity.MasjidSettings;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.CampaignRepository;
import com.masjidapp.repository.DonationRepository;
import com.masjidapp.repository.SettingsRepository;
import com.masjidapp.service.DonationService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final SettingsRepository settingsRepository;
    private final StripeServiceImpl stripeService;

    @Override
    @Transactional
    public Map<String, String> donateToCampaign(String campaignId, DonationCreateRequest donationCreateRequest) {
        MasjidSettings settings = settingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new MARequestException("Masjid settings not configured"));
        String secretKey = settings.getStripeSecretKey();
        String publishableKey = settings.getStripePublishableKey();
        if (secretKey == null || publishableKey == null) {
            throw new MARequestException("Stripe is not configured. Please add your Stripe keys in Settings.");
        }

        try {
            Campaign campaign = campaignRepository.findById(UUID.fromString(campaignId))
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));
            Donation donation = Donation.builder()
                    .donorName(donationCreateRequest.getDonorName())
                    .donorEmail(donationCreateRequest.getDonorEmail())
                    .campaign(campaign)
                    .amount(donationCreateRequest.getAmount())
                    .processingFee(BigDecimal.ZERO)
                    .totalCharged(donationCreateRequest.getAmount())
                    .anonymous(donationCreateRequest.getIsAnonymous())
                    // Donor always covers the Stripe fee so the charity receives the full amount.
                    .coverFee(true)
                    .createdAt(Instant.now())
                    .build();

            Donation saved = donationRepository.save(donation);
            Map<String, String> paymentInfo = stripeService.createPaymentIntent(
                    saved.getId(), campaignId, campaign.getTitle(), secretKey, saved.getAmount());

            saved.setStripePaymentIntentId(paymentInfo.get("paymentIntentId"));
            saved.setCurrency(paymentInfo.get("currency"));
            saved.setProcessingFee(new BigDecimal(paymentInfo.get("processingFee")));
            saved.setTotalCharged(new BigDecimal(paymentInfo.get("totalCharged")));

            // Response for the mobile app's Stripe Payment Sheet. "paymentToken" is the
            // PaymentIntent client secret (renamed to avoid confusion with the Stripe secret key).
            Map<String, String> response = new HashMap<>();
            response.put("donationId", saved.getId().toString());
            response.put("paymentToken", paymentInfo.get("clientSecret"));
            response.put("publishableKey", publishableKey);
            response.put("amount", saved.getAmount().toString());
            response.put("processingFee", paymentInfo.get("processingFee"));
            response.put("totalCharged", paymentInfo.get("totalCharged"));
            response.put("currency", paymentInfo.get("currency"));
            return response;
        } catch (StripeException | RuntimeException e) {
            log.error("Donation creation failed: {}", e.getMessage(), e);
            throw new MARequestException("Error in create donation: " + e.getMessage(), e);
        }
    }

    @Override
    public DonationDto getDonationStatus(UUID donationId) {
        return donationRepository.findById(donationId).map(DonationDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));
    }

    @Transactional
    @Override
    public void updateCampaignDonationStatus(String donationId, String campaignId, BigDecimal amount) {
        log.info("Updating donation: {}", donationId);

        int updated = donationRepository.markCompletedIfNotAlready(UUID.fromString(donationId));
        if (updated == 0) {
            log.info("Donation already processed: {}", donationId);
            return;
        }
        campaignRepository.incrementCampaign(
                UUID.fromString(campaignId),
                amount
        );
    }

    @Override
    @Transactional
    public void updateActualStripeFee(String donationId, BigDecimal actualFee, String paymentMethod) {
        donationRepository.findById(UUID.fromString(donationId)).ifPresent(donation -> {
            donation.setProcessingFee(actualFee);
            donation.setTotalCharged(donation.getAmount().add(actualFee));
            if (paymentMethod != null) donation.setPaymentMethod(paymentMethod);
            donation.setUpdatedAt(Instant.now());
            log.info("Updated actual Stripe fee for donation {}: fee={}", donationId, actualFee);
        });
    }

    @Override
    @Transactional
    public void updateDonationStatus(String donationId) {
        Donation donation = Optional.of(donationId)
                .map(UUID::fromString)
                .flatMap(donationRepository::findById)
                .orElse(null);
        if (donation != null && donation.getStatus() != DonationStatus.completed) {
            donation.setStatus(DonationStatus.failed);
            donation.setCompletedAt(Instant.now());
            donation.setUpdatedAt(Instant.now());
        }
    }

}
