package com.masjidapp.controller.campaign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.dto.request.CampaignCreateRequest;
import com.masjidapp.dto.request.CampaignUpdateRequest;
import com.masjidapp.dto.request.CampaignUpdateStatusRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.service.CampaignService;
import com.masjidapp.service.impl.CampaignDonationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns (Admin)", description = "Admin APIs for managing donation campaigns")
@SecurityRequirement(name = "bearerAuth")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignDonationQueryService campaignDonationQueryService;

    @GetMapping
    @Operation(
            summary = "Get all campaigns",
            description = "Returns a paginated list of all campaigns for administration."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCampaigns(
            @ParameterObject @PageableDefault(page = 0, size = 5) Pageable pageable) {
        Page<CampaignDto> pageResult = campaignService.getAllCampaigns(pageable);
        Map<String, Object> data = new HashMap<>();
        data.put("content", pageResult.getContent());
        data.put("pagination", buildPagination(pageResult));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("{id}")
    @Operation(
            summary = "Get campaign by ID",
            description = "Returns details of a single campaign identified by its ID."
    )
    public ResponseEntity<ApiResponse<CampaignDto>> getCampaign(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getCampaignById(id)));
    }

    @PostMapping
    @Operation(
            summary = "Create a new campaign",
            description = "Creates a new campaign with the provided details."
    )
    public ResponseEntity<ApiResponse<CampaignDto>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest campaignCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(campaignService.createCampaign(campaignCreateRequest)));
    }

    @PutMapping("{id}")
    @Operation(
            summary = "Update a campaign",
            description = "Updates an existing campaign with the provided details."
    )
    public ResponseEntity<ApiResponse<CampaignDto>> updateCampaign(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CampaignUpdateRequest campaignUpdateRequest) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.updateCampaign(id, campaignUpdateRequest)));
    }

    @PatchMapping("{id}/status")
    @Operation(
            summary = "Update campaign status",
            description = "Updates the status of a campaign (e.g., active, paused, completed)."
    )
    public ResponseEntity<ApiResponse<CampaignDto>> updateCampaignStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CampaignUpdateStatusRequest campaignUpdateStatusRequest) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.updateCampaignStatus(id, campaignUpdateStatusRequest)));
    }

    @GetMapping("{id}/donations")
    @Operation(
            summary = "Get campaign donation summary",
            description = "Returns a paginated summary of donations for the specified campaign."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaignDonationSummary(
            @PathVariable("id") UUID id,
            @ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<DonationDto> pageResult = campaignDonationQueryService.getCampaignDonationSummary(id, pageable);
        Map<String, Object> data = new HashMap<>();
        data.put("content", pageResult.getContent());
        data.put("pagination", buildPagination(pageResult));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private Map<String, Object> buildPagination(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page.getNumber());
        pagination.put("size", page.getSize());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        return pagination;
    }

}
