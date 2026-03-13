package com.masjidapp.service.impl;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.Donation;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.CampaignRepository;
import com.masjidapp.repository.DonationRepository;
import com.masjidapp.service.DonationService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final CampaignRepository campaignRepository;
    private final StripeServiceImpl stripeService;

    @Override
    @Transactional
    public Map<String, String> donateToCampaign(String campaignId, DonationCreateRequest donationCreateRequest) {
        try {
            Campaign campaign = campaignRepository.findById(UUID.fromString(donationCreateRequest.getCampaignId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + donationCreateRequest.getCampaignId()));
            Donation donation = Donation.builder().donorName(donationCreateRequest.getDonorName())
                    .donorEmail(donationCreateRequest.getDonorEmail())
                    .campaign(campaign)
                    .amount(donationCreateRequest.getAmount())
                    .anonymous(donationCreateRequest.getIsAnonymous())
                    .coverFee(donationCreateRequest.isCoverFee())
                    .createdAt(Instant.now())
                    .build();
            Donation saved = donationRepository.save(donation);
            String checkoutSessionId = stripeService.createCheckoutSession(saved.getId(), campaignId, campaign.getTitle(),
                    donationCreateRequest);
            saved.setStripeCheckoutSessionId(checkoutSessionId);
            return Map.of("sessionId", checkoutSessionId);
        } catch (StripeException | RuntimeException e ) {
            throw new MARequestException("Error in create donation", e);
        }
    }

    @Override
    public DonationDto getDonationStatus(UUID donationId) {
        return donationRepository.findById(donationId).map(DonationDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found with id: " + donationId));
    }

}
