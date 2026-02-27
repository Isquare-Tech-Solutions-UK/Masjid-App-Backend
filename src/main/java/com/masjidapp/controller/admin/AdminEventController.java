package com.masjidapp.controller.admin;

import com.masjidapp.dto.container.AuthRequestContainer;
import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Events", description = "Endpoints for managing masjid events — create, list, and view event details")
public class AdminEventController {

        private final EventService eventService;
        private final AuthRequestContainer authRequestContainer;

        /**
         * POST /admin/events
         * Create a new event with optional image uploads.
         */
        @Operation(summary = "Create Event", description = "Create a new event with optional image uploads. Accepts multipart/form-data.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Event created successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<EventResponse>> createEvent(
                        @Valid @ModelAttribute CreateEventRequest request,
                        @RequestPart(value = "images", required = false) List<MultipartFile> images) {

                log.info("Received request to create event. title={}, speaker={}, date={}",
                                request.getTitle(), request.getSpeaker(), request.getDate());

                EventResponse eventResponse = eventService.createEvent(request, images,
                                authRequestContainer.getAdminUser());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(eventResponse));
        }

        /**
         * GET /admin/events
         * Returns all events (including drafts) for admin with filters and pagination.
         */
        @Operation(summary = "List Events (Admin)", description = "Returns all events (including drafts) for admin with optional filters and pagination. Sorted by event date descending.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @GetMapping
        public ResponseEntity<ApiResponse<Map<String, Object>>> getEventsForAdmin(
                        @Parameter(description = "Filter by status (draft, published, cancelled, completed)") @RequestParam(required = false) String status,
                        @Parameter(description = "Future events only") @RequestParam(required = false) Boolean upcoming,
                        @Parameter(description = "Past events only") @RequestParam(required = false) Boolean past,
                        @Parameter(description = "Start date range (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @Parameter(description = "End date range (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {

                log.info("Fetching all events - status={}, upcoming={}, past={}, startDate={}, endDate={}, page={}, size={}",
                                status, upcoming, past, startDate, endDate, page, size);

                Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
                Page<EventResponse> pageResult = eventService.getAdminEvents(
                                status, upcoming, past, startDate, endDate, pageable);

                Map<String, Object> data = new HashMap<>();
                data.put("content", pageResult.getContent());
                data.put("pagination", buildPagination(pageResult));

                return ResponseEntity.ok(ApiResponse.success(data));
        }

        /**
         * GET /admin/events/{id}
         * Get a single event by ID.
         * Only returns events created by the authenticated admin.
         */
        @Operation(summary = "Get Event by ID", description = "Retrieve a single event by its UUID.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<EventResponse>> getEventById(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id) {
                log.info("Fetching event by ID: {}", id);

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
