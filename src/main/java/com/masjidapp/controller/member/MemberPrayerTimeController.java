package com.masjidapp.controller.member;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.MemberPrayerTimeResponse;
import com.masjidapp.service.PrayerTimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Member Prayer Times API — read-only endpoints for the mobile app.
 * Protected by MemberApiKeyFilter (X-API-KEY header).
 */
@RestController
@RequestMapping("/member/prayer-times")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Prayer Times", description = "Public endpoints for viewing daily prayer times")
@SecurityRequirement(name = "apiKeyAuth")
public class MemberPrayerTimeController {

    private final PrayerTimeService prayerTimeService;

    /**
     * GET /member/prayer-times/today
     * Returns today's prayer times with nextPrayer calculation.
     */
    @Operation(
            summary = "Get Today's Prayer Times",
            description = "Returns prayer times for today with the next upcoming prayer information.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Today's prayer times retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Prayer times not found for today",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<MemberPrayerTimeResponse>> getTodayPrayerTimes() {
        log.info("Fetching today's prayer times");
        MemberPrayerTimeResponse response = prayerTimeService.getTodayPrayerTimes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /member/prayer-times/week
     * Returns prayer times for the current week (Monday to Sunday).
     */
    @Operation(
            summary = "Get Weekly Prayer Times",
            description = "Returns prayer times for the current week (Monday to Sunday).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Weekly prayer times retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<List<MemberPrayerTimeResponse>>> getWeekPrayerTimes() {
        log.info("Fetching weekly prayer times");
        List<MemberPrayerTimeResponse> response = prayerTimeService.getWeekPrayerTimes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /member/prayer-times/month
     * Returns prayer times for a given month (defaults to current month).
     */
    @Operation(
            summary = "Get Monthly Prayer Times",
            description = "Returns prayer times for a given month and year. Defaults to current month/year if not provided.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Monthly prayer times retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<MemberPrayerTimeResponse>>> getMonthPrayerTimes(
            @Parameter(description = "Month (1-12), defaults to current month")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Year (e.g. 2026), defaults to current year")
            @RequestParam(required = false) Integer year) {

        log.info("Fetching monthly prayer times - month={}, year={}", month, year);
        List<MemberPrayerTimeResponse> response = prayerTimeService.getMonthPrayerTimes(month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
