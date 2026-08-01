package com.masjidapp.controller.member;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.DonationCauseListResponse;
import com.masjidapp.service.DonationCauseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member/donation-cause")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Donation Causes", description = "Donation causes available for member/mobile donation flows")
@SecurityRequirement(name = "apiKeyAuth")
public class MemberDonationCauseController {

    private final DonationCauseService donationCauseService;

    @GetMapping
    @Operation(summary = "Get donation causes",
            description = "Returns the list of donation causes available for member donations.")
    public ResponseEntity<ApiResponse<DonationCauseListResponse>> getDonationCauses() {
        log.info("GET /member/donation-cause");
        return ResponseEntity.ok(ApiResponse.success(donationCauseService.getAllCauses()));
    }
}