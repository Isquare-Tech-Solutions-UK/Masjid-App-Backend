package com.masjidapp.controller.donation;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.service.DonationService;
import com.masjidapp.service.impl.CampaignDonationQueryService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Member donation and campaign donation APIs")
@SecurityRequirement(name = "apiKeyAuth")
public class DonationController {

    private final DonationService donationService;
    private final CampaignDonationQueryService campaignDonationQueryService;

    @GetMapping("campaigns")
    @Operation(
            summary = "Get active and paused campaigns",
            description = "Returns a paginated list of active and paused campaigns available for member donations."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivePausedCampaigns(
            @ParameterObject @PageableDefault(page = 0, size = 5) Pageable pageable) {
        Page<CampaignDto> pageResult = campaignDonationQueryService.getActivePausedCampaigns(pageable);
        Map<String, Object> data = new HashMap<>();
        data.put("content", pageResult.getContent());
        data.put("pagination", buildPagination(pageResult));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("campaigns/{id}/donate")
    @Operation(
            summary = "Donate to a campaign",
            description = "Creates a donation for the specified campaign and returns information required to complete the payment."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> donateToCampaign(
            @PathVariable("id") String campaignId,
            @Valid @RequestBody DonationCreateRequest donationCreateRequest) {
        return ResponseEntity.ok(ApiResponse.success(donationService.donateToCampaign(campaignId, donationCreateRequest)));
    }

    @Hidden
    @GetMapping("donations/{id}/status")
    public ResponseEntity<ApiResponse<DonationDto>> getDonationStatus(@PathVariable("id") UUID donationId) {
        return ResponseEntity.ok(ApiResponse.success(donationService.getDonationStatus(donationId)));
    }

    @GetMapping("campaigns/{id}/donations")
    @Operation(
            summary = "Get donations for a campaign",
            description = "Returns a paginated list of active donations for the specified campaign."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaignDonations(
            @PathVariable("id") UUID campaignId,
            @ParameterObject @PageableDefault(page = 0, size = 5) Pageable pageable) {
        Page<DonationDto> pageResult = campaignDonationQueryService.getCampaignActiveDonations(campaignId, pageable);
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
