package com.masjidapp.controller.member;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Member Events API — read-only endpoints for the mobile app.
 * Protected by MemberApiKeyFilter (X-API-KEY header).
 */
@RestController
@RequestMapping("/member/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member Events", description = "Public endpoints for viewing masjid events")
@SecurityRequirement(name = "apiKeyAuth")
public class MemberEventController {

    private final EventService eventService;

    /**
     * GET /member/events
     * Returns all published events for members with optional filters and pagination.
     */
    @Operation(
            summary = "List Events",
            description = "Returns all published events for members with optional filters and pagination.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Events retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberEvents(
            @Parameter(description = "Filter by status (upcoming, past)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Start date range (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date range (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Boolean upcoming = null;
        Boolean past = null;

        if ("upcoming".equalsIgnoreCase(status)) {
            upcoming = true;
        } else if ("past".equalsIgnoreCase(status)) {
            past = true;
        }

        log.info("Fetching member events - status={}, upcoming={}, past={}, startDate={}, endDate={}, page={}, size={}",
                status, upcoming, past, startDate, endDate, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<EventResponse> pageResult = eventService.getMemberEvents(
                upcoming, past, startDate, endDate, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("content", pageResult.getContent());
        data.put("pagination", buildPagination(pageResult));

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /member/events/{id}
     * Retrieves a single published event by its UUID.
     */
    @Operation(
            summary = "Get Event by ID",
            description = "Retrieve a single published event by its UUID.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Event retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Event not found",
                    content = @Content(schema = @Schema(
                            implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getMemberEventById(
            @Parameter(description = "UUID of the event", required = true)
            @PathVariable UUID id) {
        log.info("Fetching member event by ID: {}", id);
        EventResponse eventResponse = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success(eventResponse));
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

