package com.masjidapp.controller.admin;

import com.masjidapp.dto.container.AuthRequestContainer;
import com.masjidapp.dto.request.CreateAnnouncementRequest;
import com.masjidapp.dto.request.UpdateAnnouncementRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.AnnouncementResponse;
import com.masjidapp.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Announcements", description = "Endpoints for managing masjid announcements")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthRequestContainer authRequestContainer;

    @Operation(summary = "Create Announcement", description = "Create a new announcement.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Announcement created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {

        log.info("Received request to create announcement. title={}", request.getTitle());

        AnnouncementResponse response = announcementService.createAnnouncement(
                request, authRequestContainer.getAdminUser());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "List Announcements (Admin)", description = "Returns all announcements with optional filters and pagination.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnnouncementsForAdmin(
            @Parameter(description = "Filter by status (draft, scheduled, sent)") @RequestParam(required = false) String status,
            @Parameter(description = "Start date range (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date range (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Search title or message") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching admin announcements - status={}, search={}", status, search);

        Pageable pageable = PageRequest.of(page, size);
        Page<AnnouncementResponse> pageResult = announcementService.getAdminAnnouncements(
                status, startDate, endDate, search, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("content", pageResult.getContent());
        data.put("pagination", buildPagination(pageResult));

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Get Announcement by ID", description = "Retrieve a single announcement by its UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(
            @Parameter(description = "UUID of the announcement", required = true) @PathVariable UUID id) {
        log.info("Fetching announcement by ID: {}", id);

        AnnouncementResponse response = announcementService.getAnnouncementById(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update Announcement", description = "Update announcement details. Cannot change status from sent to another state.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @Parameter(description = "UUID of the announcement", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateAnnouncementRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received request to update announcement. id={}", id);

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AnnouncementResponse updated = announcementService.updateAnnouncement(
                id, request, authRequestContainer.getAdminUser(), ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @Operation(summary = "Delete Announcement", description = "Delete an announcement. Only unsent announcements can be deleted.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @Parameter(description = "UUID of the announcement", required = true) @PathVariable UUID id) {
        log.info("Received request to delete announcement: {}", id);
        announcementService.deleteAnnouncement(id, authRequestContainer.getAdminUser());
        return ResponseEntity.noContent().build();
    }

    public static class StatusUpdateRequest {
        @jakarta.validation.constraints.NotBlank(message = "Status is required")
        public String status;
    }

    @Operation(summary = "Change Announcement Status", description = "Update the status for a specific announcement.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> changeAnnouncementStatus(
            @Parameter(description = "UUID of the announcement", required = true) @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        log.info("Received request to change announcement status. id={}, newStatus={}", id, request.status);

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AnnouncementResponse updated = announcementService.changeAnnouncementStatus(
                id, request.status, authRequestContainer.getAdminUser(), ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * POST /admin/announcements/{id}/notify
     * Send a push notification for a sent announcement to all registered devices.
     */
    @Operation(summary = "Send Announcement Notification",
            description = "Send a push notification for a sent announcement to all registered devices.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Announcement is not in sent status",
                    content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Announcement not found",
                    content = @Content(schema = @Schema(implementation = com.masjidapp.exception.GlobalExceptionHandler.ErrorResponse.class)))
    })
    @PostMapping("/{id}/notify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> notifyAnnouncement(
            @Parameter(description = "UUID of the announcement", required = true) @PathVariable UUID id) {

        log.info("Received request to notify announcement. id={}", id);

        int notifiedCount = announcementService.notifyAnnouncement(id);

        Map<String, Object> result = new HashMap<>();
        result.put("devicesNotified", notifiedCount);
        result.put("announcementId", id);

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
