package com.masjidapp.service.impl;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.entity.CampaignStatus;
import com.masjidapp.entity.Donation;
import com.masjidapp.entity.DonationStatus;
import com.masjidapp.repository.CampaignRepository;
import com.masjidapp.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                List.of(CampaignStatus.active, CampaignStatus.paused)).map(CampaignDto::toDto);
    }

    public Page<DonationDto> getCampaignDonationSummary(UUID uuid, Pageable pageable) {
        Page<Donation> donations = donationRepository.findByCampaignId(uuid, newestFirst(pageable));
        return donations.map(DonationDto::toDto);
    }

    public Page<DonationDto> getCampaignActiveDonations(UUID id, Pageable pageable) {
        // Member-facing list — anonymise donors who chose to donate anonymously.
        return donationRepository.findByCampaignIdAndStatus(id, DonationStatus.completed, newestFirst(pageable))
                .map(DonationDto::toPublicDto);
    }

    /** Force donation history to be sorted by creation date, newest first. */
    private Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

}
