package com.masjidapp.service;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.request.CampaignCreateRequest;
import com.masjidapp.dto.request.CampaignUpdateRequest;
import com.masjidapp.dto.request.CampaignUpdateStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CampaignService {
    Page<CampaignDto> getAllCampaigns(Pageable pageable);
    CampaignDto getCampaignById(UUID id);
    CampaignDto createCampaign(CampaignCreateRequest request);
    CampaignDto updateCampaign(UUID id, CampaignUpdateRequest request);
    CampaignDto updateCampaignStatus(UUID id, CampaignUpdateStatusRequest request);
    void deleteDraftCampaign(UUID id);
}
