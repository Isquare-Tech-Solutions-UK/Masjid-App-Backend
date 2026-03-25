package com.masjidapp.service.impl;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.Donation;
import com.masjidapp.entity.DonationStatus;
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
        String stripeAccountId = settingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new MARequestException("Masjid settings not configured"))
                .getStripeAccountId();
        if (stripeAccountId == null) {
            throw new MARequestException("Stripe is not connected. Please connect your Stripe account in Settings.");
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
                    .coverFee(donationCreateRequest.isCoverFee())
                    .createdAt(Instant.now())
                    .build();

            Donation saved = donationRepository.save(donation);
            Map<String, String> sessionMap = stripeService.createCheckoutSession(
                    saved.getId(), campaignId, campaign.getTitle(), stripeAccountId, donationCreateRequest);
            saved.setStripeCheckoutSessionId(sessionMap.get("sessionId"));
            saved.setStripePaymentIntentId(sessionMap.get("paymentIntentId"));
            saved.setCurrency(sessionMap.get("currency"));

            if (sessionMap.containsKey("processingFee")) {
                saved.setProcessingFee(new BigDecimal(sessionMap.get("processingFee")));
                saved.setTotalCharged(saved.getAmount().add(saved.getProcessingFee()));
            }
            return sessionMap;
        } catch (StripeException | RuntimeException e) {
            throw new MARequestException("Error in create donation", e);
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
    public void updateDonationStatus(String donationId) {
        Donation donation = Optional.of(donationId)
                .map(UUID::fromString)
                .flatMap(donationRepository::findById)
                .orElse(null);
        if (donation != null) {
            donation.setStatus(DonationStatus.failed);
            donation.setCompletedAt(Instant.now());
            donation.setUpdatedAt(Instant.now());
        }
    }

}
