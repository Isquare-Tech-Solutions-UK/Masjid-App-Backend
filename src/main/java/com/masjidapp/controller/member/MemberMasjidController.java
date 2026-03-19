package com.masjidapp.controller.member;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.MemberMasjidInfoResponse;
import com.masjidapp.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member/masjid")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Masjid", description = "Masjid information for mobile app")
@SecurityRequirement(name = "apiKeyAuth")
public class MemberMasjidController {

    private final SettingsService settingsService;

    @Operation(
            summary = "Get Masjid Info",
            description = "Get masjid information for mobile app including name, address, contact, services, and facilities.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Masjid information retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<MemberMasjidInfoResponse>> getMasjidInfo() {
        log.info("Fetching masjid info for member API");
        MemberMasjidInfoResponse response = settingsService.getMasjidInfo();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
