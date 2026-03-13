package com.masjidapp.settings.controller;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.settings.dto.MasjidSettingsResponse;
import com.masjidapp.settings.dto.UpdateMasjidSettingsRequest;
import com.masjidapp.settings.dto.UpdatePaymentSettingsRequest;
import com.masjidapp.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Settings", description = "Endpoints for managing masjid settings, services, facilities, and payment configuration")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AdminSettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get masjid settings", description = "Retrieve current masjid settings including address, contact, services, facilities, and payment info")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> getSettings() {
        log.info("GET /admin/settings");
        MasjidSettingsResponse response = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Update masjid settings", description = "Update masjid details including name, about, address, contact, services, facilities, and capacity")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdateMasjidSettingsRequest request) {
        log.info("POST /admin/settings - updating masjid settings");
        MasjidSettingsResponse response = settingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payment")
    @Operation(summary = "Update payment settings", description = "Update bank and payment configuration")
    public ResponseEntity<ApiResponse<MasjidSettingsResponse>> updatePaymentSettings(
            @Valid @RequestBody UpdatePaymentSettingsRequest request) {
        log.info("POST /admin/settings/payment - updating payment settings");
        MasjidSettingsResponse response = settingsService.updatePaymentSettings(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
