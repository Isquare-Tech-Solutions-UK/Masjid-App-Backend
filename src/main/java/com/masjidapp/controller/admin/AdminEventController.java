package com.masjidapp.controller.admin;

import com.masjidapp.dto.container.AuthRequestContainer;
import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.request.UpdateEventRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.access.prepost.PreAuthorize;
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
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
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
                        @Parameter(description = "Event Title") @RequestParam("title") String title,
                        @Parameter(description = "Speaker Name") @RequestParam("speaker") String speaker,
                        @Parameter(description = "Event Date (ISO-8601, e.g. 2025-02-15T18:00:00)") @RequestParam("date") String date,
                        @Parameter(description = "Event/Registration Link") @RequestParam(value = "link", required = false) String link,
                        @Parameter(description = "Event Description") @RequestParam("description") String description,
                        @Parameter(description = "Event Venue") @RequestParam("venue") String venue,
                        @Parameter(description = "Status (draft, published)", example = "draft") @RequestParam(value = "status", required = false, defaultValue = "draft") String status,
                        @Parameter(description = "Event Images") @RequestPart(value = "images", required = false) List<MultipartFile> images) {

                log.info("Received request to create event. title={}, speaker={}, date={}",
                                title, speaker, date);

                CreateEventRequest request = CreateEventRequest.builder()
                                .title(title)
                                .speaker(speaker)
                                .date(date)
                                .link(link)
                                .description(description)
                                .venue(venue)
                                .status(status)
                                .build();

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
                        @Parameter(description = "Search title or description") @RequestParam(required = false) String search,
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {

                log.info("Fetching all events - status={}, upcoming={}, past={}, startDate={}, endDate={}, search={}, page={}, size={}",
                                status, upcoming, past, startDate, endDate, search, page, size);

                Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
                Page<EventResponse> pageResult = eventService.getAdminEvents(
                                status, upcoming, past, startDate, endDate, search, pageable);

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

        /**
         * PUT /admin/events/{id}
         * Update event details (partial update) with optional image replacement.
         * - Deletes old images if new ones are provided, then uploads new images.
         * - Cannot change status from published to draft.
         * - Returns updated event.
         */
        @Operation(summary = "Update Event", description = "Update event details with optional image replacement.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event updated successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id,
                        @Parameter(description = "Event Title") @RequestParam(value = "title", required = false) String title,
                        @Parameter(description = "Speaker Name") @RequestParam(value = "speaker", required = false) String speaker,
                        @Parameter(description = "Event Date (ISO-8601, e.g. 2025-02-15T18:00:00)") @RequestParam(value = "date", required = false) String date,
                        @Parameter(description = "Event/Registration Link") @RequestParam(value = "link", required = false) String link,
                        @Parameter(description = "Event Description") @RequestParam(value = "description", required = false) String description,
                        @Parameter(description = "Event Venue") @RequestParam(value = "venue", required = false) String venue,
                        @Parameter(description = "Status (draft, published)") @RequestParam(value = "status", required = false) String status,
                        @Parameter(description = "New Images to replace old ones") @RequestPart(value = "images", required = false) List<MultipartFile> images,
                        HttpServletRequest httpRequest) {

                log.info("Received request to update event. id={}, title={}", id, title);

                UpdateEventRequest request = new UpdateEventRequest();
                request.setTitle(title);
                request.setSpeaker(speaker);
                request.setDate(date);
                request.setLink(link);
                request.setDescription(description);
                request.setVenue(venue);
                request.setStatus(status);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");

                EventResponse updated = eventService.updateEvent(id, request, images,
                                authRequestContainer.getAdminUser(), ipAddress,
                                userAgent);

                return ResponseEntity.ok(ApiResponse.success(updated));
        }

        /**
         * DELETE /admin/events/{id}
         * Delete a cancelled event (soft delete).
         */
        @Operation(summary = "Delete Event", description = "Soft delete an event. Only events with 'cancelled' status can be deleted.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Event status is not CANCELLED", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('admin') or hasRole('super_admin')")
        public ResponseEntity<ApiResponse<UUID>> deleteEvent(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id) {
                log.info("Received request to delete event: {}", id);
                eventService.deleteEvent(id, authRequestContainer.getAdminUser());
                return ResponseEntity.ok(ApiResponse.success(id));
        }

        public static class StatusUpdateRequest {
                @jakarta.validation.constraints.NotBlank(message = "Status is required")
                public String status;
        }

        /**
         * PATCH /admin/events/{id}/status
         * Change the status of an event.
         */
        @Operation(summary = "Change Event Status", description = "Update the status for a specific event (e.g., draft to published). Cannot change from published back to draft.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event status updated successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @PatchMapping("/{id}/status")
        public ResponseEntity<ApiResponse<EventResponse>> changeEventStatus(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id,
                        @Valid @RequestBody StatusUpdateRequest request,
                        HttpServletRequest httpRequest) {

                log.info("Received request to change event status. id={}, newStatus={}", id, request.status);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");

                EventResponse updated = eventService.changeEventStatus(id, request.status,
                                authRequestContainer.getAdminUser(), ipAddress, userAgent);

                return ResponseEntity.ok(ApiResponse.success(updated));
        }

        /**
         * PATCH /admin/events/{id}/cancel
         * Cancel a draft or published event.
         */
        @Operation(summary = "Cancel Event",
                description = "Cancel a draft or published event. Cannot cancel completed or already-cancelled events.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event cancelled successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Event cannot be cancelled (completed or already cancelled)",
                                        content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found",
                                        content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
        })
        @PatchMapping("/{id}/cancel")
        public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id,
                        HttpServletRequest httpRequest) {

                log.info("Received request to cancel event. id={}", id);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");

                EventResponse updated = eventService.changeEventStatus(id, "cancelled",
                                authRequestContainer.getAdminUser(), ipAddress, userAgent);

                return ResponseEntity.ok(ApiResponse.success(updated));
        }

        /**
         * POST /admin/events/{id}/notify
         * Send a push notification for a published event to all registered devices.
         */
        @Operation(summary = "Send Event Notification",
                description = "Send a push notification for a published event to all registered mobile devices.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification sent successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Event is not published"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
        })
        @PostMapping("/{id}/notify")
        public ResponseEntity<ApiResponse<Map<String, Object>>> notifyEvent(
                        @Parameter(description = "UUID of the event", required = true) @PathVariable UUID id) {

                log.info("Received request to notify event. id={}", id);

                int notifiedCount = eventService.notifyEvent(id);

                Map<String, Object> result = new HashMap<>();
                result.put("devicesNotified", notifiedCount);
                result.put("eventId", id);

                return ResponseEntity.ok(ApiResponse.success(result));
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
