package com.masjidapp.controller.campaign;

import java.util.UUID;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.dto.request.CampaignCreateRequest;
import com.masjidapp.dto.request.CampaignUpdateRequest;
import com.masjidapp.dto.request.CampaignUpdateStatusRequest;
import com.masjidapp.service.CampaignService;
import com.masjidapp.service.impl.CampaignDonationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns (Admin)", description = "Admin APIs for managing donation campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignDonationQueryService campaignDonationQueryService;

    @GetMapping
    @Operation(
            summary = "Get all campaigns",
            description = "Returns a paginated list of all campaigns for administration."
    )
    public ResponseEntity<Page<CampaignDto>> getAllCampaigns(
            @PageableDefault(page = 0, size = 5) Pageable pageable) {
        return ResponseEntity.ok(campaignService.getAllCampaigns(pageable));
    }

    @GetMapping("{id}")
    @Operation(
            summary = "Get campaign by ID",
            description = "Returns details of a single campaign identified by its ID."
    )
    public ResponseEntity<CampaignDto> getCampaign(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new campaign",
            description = "Creates a new campaign with the provided details."
    )
    public ResponseEntity<CampaignDto> createCampaign(
            @Valid @RequestBody CampaignCreateRequest campaignCreateRequest) {
        return ResponseEntity.ok(campaignService.createCampaign(campaignCreateRequest));
    }

    @PutMapping("{id}")
    @Operation(
            summary = "Update a campaign",
            description = "Updates an existing campaign with the provided details."
    )
    public ResponseEntity<CampaignDto> updateCampaign(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CampaignUpdateRequest campaignUpdateRequest) {
        return ResponseEntity.ok(campaignService.updateCampaign(id, campaignUpdateRequest));
    }

    @PutMapping("{id}/update-status")
    @Operation(
            summary = "Update campaign status",
            description = "Updates the status of a campaign (e.g., active, paused, closed)."
    )
    public ResponseEntity<CampaignDto> updateCampaignStatus(
            @PathVariable("id") UUID id,
            @RequestBody CampaignUpdateStatusRequest campaignUpdateStatusRequest) {
        return ResponseEntity.ok(campaignService.updateCampaignStatus(id, campaignUpdateStatusRequest));
    }

    @GetMapping("{id}/donations")
    @Operation(
            summary = "Get campaign donation summary",
            description = "Returns a paginated summary of donations for the specified campaign."
    )
    public ResponseEntity<Page<DonationDto>> getCampaignDonationSummary(
            @PathVariable("id") UUID id,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(campaignDonationQueryService.getCampaignDonationSummary(id, pageable));
    }

}
