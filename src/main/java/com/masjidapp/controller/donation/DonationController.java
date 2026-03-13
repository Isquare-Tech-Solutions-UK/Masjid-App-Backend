package com.masjidapp.controller.donation;

import com.masjidapp.dto.campaign.CampaignDto;
import com.masjidapp.dto.donation.DonationCreateRequest;
import com.masjidapp.dto.donation.DonationDto;
import com.masjidapp.service.DonationService;
import com.masjidapp.service.impl.CampaignDonationQueryService;
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
public class DonationController {

    private final DonationService donationService;
    private final CampaignDonationQueryService campaignDonationQueryService;

    @GetMapping("campaigns")
    public ResponseEntity<Page<CampaignDto>> getActivePausedCampaigns(@PageableDefault(page = 0, size = 5) Pageable pageable) {
        return ResponseEntity.ok(campaignDonationQueryService.getActivePausedCampaigns(pageable));
    }

    @PostMapping("campaigns/{id}/donate")
    public ResponseEntity<Map<String, String>> donateToCampaign(@PathVariable("id") String campaignId,
                                                                @RequestBody DonationCreateRequest donationCreateRequest) {
        return ResponseEntity.ok(donationService.donateToCampaign(campaignId, donationCreateRequest));
    }

    @GetMapping("donations/{id}/status")
    public ResponseEntity<DonationDto> getDonationStatus(@PathVariable("id") UUID donationId) {
        return ResponseEntity.ok(donationService.getDonationStatus(donationId));
    }

}
