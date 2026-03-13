package com.masjidapp.service;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;

import java.util.Map;
import java.util.UUID;

public interface DonationService {

    Map<String, String> donateToCampaign(String campaignId, DonationCreateRequest donationCreateRequest);
    DonationDto getDonationStatus(UUID donationId);
}
