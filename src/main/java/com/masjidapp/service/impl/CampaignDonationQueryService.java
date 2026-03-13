package com.masjidapp.service.impl;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.entity.CampaignStatus;
import com.masjidapp.entity.Donation;
import com.masjidapp.repository.CampaignRepository;
import com.masjidapp.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignDonationQueryService {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;

    public Page<CampaignDto> getActivePausedCampaigns(Pageable pageable) {
        return campaignRepository.findByStatusInOrderByStartDateDescEndDateAsc(pageable,
                List.of(CampaignStatus.active, CampaignStatus.active)).map(CampaignDto::toDto);
    }

    public Page<DonationDto> getCampaignDonationSummary(UUID uuid, Pageable pageable) {
        Page<Donation> donations = donationRepository.findByCampaignId(uuid, pageable);
        return donations.map(DonationDto::toDto);
    }

}
