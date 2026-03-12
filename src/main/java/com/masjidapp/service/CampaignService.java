package com.masjidapp.service;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.request.CampaignCreateRequest;

import java.util.List;
import java.util.UUID;

public interface CampaignService {
    List<CampaignDto> getAllCampaigns();
    CampaignDto getCampaignById(UUID id);
    CampaignDto createCampaign(CampaignCreateRequest request);
    CampaignDto updateCampaign(UUID id, CampaignDto campaignDto);
}
