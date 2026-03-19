package com.masjidapp.controller.donation;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.service.DonationService;
import com.masjidapp.service.impl.CampaignDonationQueryService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Member donation and campaign donation APIs")
public class DonationController {

    private final DonationService donationService;
    private final CampaignDonationQueryService campaignDonationQueryService;

    @GetMapping("campaigns")
    @Operation(
            summary = "Get active and paused campaigns",
            description = "Returns a paginated list of active and paused campaigns available for member donations."
    )
    public ResponseEntity<Page<CampaignDto>> getActivePausedCampaigns(
            @PageableDefault(page = 0, size = 5) Pageable pageable) {
        return ResponseEntity.ok(campaignDonationQueryService.getActivePausedCampaigns(pageable));
    }

    @PostMapping("campaigns/{id}/donate")
    @Operation(
            summary = "Donate to a campaign",
            description = "Creates a donation for the specified campaign and returns information required to complete the payment."
    )
    public ResponseEntity<Map<String, String>> donateToCampaign(
            @PathVariable("id") String campaignId,
            @RequestBody DonationCreateRequest donationCreateRequest) {
        return ResponseEntity.ok(donationService.donateToCampaign(campaignId, donationCreateRequest));
    }

    @Hidden
    @GetMapping("donations/{id}/status")
    @Operation(
            summary = "Get donation status",
            description = "Returns the current status of a donation by ID. This endpoint is hidden from the public API documentation."
    )
    public ResponseEntity<DonationDto> getDonationStatus(@PathVariable("id") UUID donationId) {
        return ResponseEntity.ok(donationService.getDonationStatus(donationId));
    }

    @GetMapping("campaigns/{id}/donations")
    @Operation(
            summary = "Get donations for a campaign",
            description = "Returns a paginated list of active donations for the specified campaign."
    )
    public ResponseEntity<Page<DonationDto>> getCampaignDonations(
            @PathVariable("id") UUID campaignId,
            @PageableDefault(page = 0, size = 5) Pageable pageable) {
        return ResponseEntity.ok(campaignDonationQueryService.getCampaignActiveDonations(campaignId, pageable));
    }

}
