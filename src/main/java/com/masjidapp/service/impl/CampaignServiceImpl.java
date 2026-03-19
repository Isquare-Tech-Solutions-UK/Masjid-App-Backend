package com.masjidapp.service.impl;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.request.CampaignCreateRequest;
import com.masjidapp.dto.request.CampaignUpdateRequest;
import com.masjidapp.dto.request.CampaignUpdateStatusRequest;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.CampaignStatus;
import com.masjidapp.exception.MARequestException;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.AdminUserRepository;
import com.masjidapp.repository.CampaignRepository;
import com.masjidapp.security.SecurityUtil;
import com.masjidapp.service.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final SecurityUtil securityUtil;
    private final AdminUserRepository adminUserRepository;

    @Override
    public Page<CampaignDto> getAllCampaigns(Pageable pageable) {

        log.info("Fetching all active campaigns");
        Page<Campaign> campaigns = campaignRepository.findByOrderByStartDateDescEndDateAsc(pageable);
        return campaigns.map(CampaignDto::toDto);
    }

    @Override
    public CampaignDto getCampaignById(UUID id) {

        log.info("Fetching campaign with id: {}", id);
        Campaign campaign = Optional.of(id).flatMap(campaignRepository::findById
        ).orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        return CampaignDto.toDto(campaign);
    }

    @Override
    public CampaignDto createCampaign(CampaignCreateRequest request) {

        log.info("Creating new campaign with title: {}", request.getTitle());
        try {
            Campaign.CampaignBuilder campaignBuilder = Campaign.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .goalAmount(request.getGoalAmount())
                    .raisedAmount(BigDecimal.ZERO)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .createdAt(Instant.now());

            if(CampaignStatus.active.name().equalsIgnoreCase(request.getStatus().name())) {
                campaignBuilder.status(CampaignStatus.active);
                campaignBuilder.publishedAt(Instant.now());
            } else {
                campaignBuilder.status(CampaignStatus.draft);
            }

            AdminUser adminUser = Optional.of(securityUtil.getUserName())
                    .flatMap(adminUserRepository::findByEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            campaignBuilder.createdBy(adminUser);

            Campaign saved = campaignRepository.save(campaignBuilder.build());
            log.info("Campaign created with id: {}", saved.getId());
            return CampaignDto.toDto(saved);
        } catch (Exception e) {
            throw new MARequestException("Failed to create campaign: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public CampaignDto updateCampaign(UUID id, CampaignUpdateRequest request) {

        log.info("Updating campaign with id: {}", id);
        Campaign campaign = Optional.of(id).flatMap(campaignRepository::findById
        ).orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if(campaign.getStatus() == CampaignStatus.cancelled || campaign.getStatus() == CampaignStatus.completed) {
            throw new MARequestException("Cannot update a campaign that is cancelled or completed");
        }

        if ((campaign.getStatus() == CampaignStatus.active || campaign.getStatus() == CampaignStatus.paused)
                && request.getStatus() == CampaignStatus.draft) {
            throw new MARequestException("Cannot change status from active/paused to draft");
        }

        if (campaign.getStatus() == CampaignStatus.active && campaign.getGoalAmount().compareTo(request.getGoalAmount()) != 0 &&
                (campaign.getRaisedAmount() != null && campaign.getRaisedAmount().compareTo(request.getGoalAmount()) >= 0)) {
            throw new MARequestException("The new goal amount cannot be less than or equal to the raised amount for an active campaign");
        }

        if (campaign.getStatus() == CampaignStatus.draft && request.getStatus() == CampaignStatus.active) {
            campaign.setPublishedAt(Instant.now());
        } else if (request.getStatus() == CampaignStatus.completed || request.getStatus() == CampaignStatus.cancelled) {
            campaign.setEndedAt(Instant.now());
        }

        campaign.setTitle(request.getTitle());
        campaign.setDescription(request.getDescription());
        campaign.setCategory(request.getCategory());
        campaign.setGoalAmount(request.getGoalAmount());
        campaign.setStatus(request.getStatus());
        campaign.setUpdatedAt(Instant.now());
        return CampaignDto.toDto(campaign);
    }

    @Override
    public CampaignDto updateCampaignStatus(UUID id, CampaignUpdateStatusRequest request) {

        log.info("Updating campaign status with id: {}", id);
        Campaign campaign = Optional.of(id).flatMap(campaignRepository::findById
        ).orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if(campaign.getStatus() == CampaignStatus.cancelled || campaign.getStatus() == CampaignStatus.completed) {
            throw new MARequestException("Cannot update a campaign that is cancelled or completed");
        }

        if ((campaign.getStatus() == CampaignStatus.active || campaign.getStatus() == CampaignStatus.paused)
                && request.getStatus() == CampaignStatus.draft) {
            throw new MARequestException("Cannot change status from active/paused to draft");
        }

        campaign.setStatus(request.getStatus());
        return CampaignDto.toDto(campaign);
    }

}
