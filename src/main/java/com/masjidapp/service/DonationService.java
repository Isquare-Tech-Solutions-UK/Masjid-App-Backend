package com.masjidapp.service;

import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface DonationService {

    Map<String, String> donateToCampaign(String campaignId, DonationCreateRequest donationCreateRequest);
    DonationDto getDonationStatus(UUID donationId);
    void updateCampaignDonationStatus(String donationId, String campaignId, BigDecimal amount);
    void updateDonationStatus(String donationId);
    void updateActualStripeFee(String donationId, BigDecimal actualFee, String paymentMethod);

}
