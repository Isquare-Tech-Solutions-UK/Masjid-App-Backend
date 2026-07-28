package com.masjidapp.controller.admin;

import com.masjidapp.dto.request.DonationCauseCreateRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.DonationCauseListResponse;
import com.masjidapp.service.DonationCauseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/donation-cause")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Donation Causes", description = "Endpoints for managing donation causes used by campaigns")
@SecurityRequirement(name = "bearerAuth")
public class AdminDonationCauseController {

    private final DonationCauseService donationCauseService;

    @GetMapping
    @Operation(summary = "Get all donation causes",
            description = "Returns the list of all donation causes available for campaigns.")
    public ResponseEntity<ApiResponse<DonationCauseListResponse>> getDonationCauses() {
        log.info("GET /admin/donation-cause");
        return ResponseEntity.ok(ApiResponse.success(donationCauseService.getAllCauses()));
    }

    @PostMapping
    @Operation(summary = "Create a donation cause",
            description = "Creates a new donation cause and returns the updated list of causes.")
    public ResponseEntity<ApiResponse<DonationCauseListResponse>> createDonationCause(
            @Valid @RequestBody DonationCauseCreateRequest request) {
        log.info("POST /admin/donation-cause - creating donation cause: {}", request.getData().getCause());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(donationCauseService.createCause(request)));
    }
}