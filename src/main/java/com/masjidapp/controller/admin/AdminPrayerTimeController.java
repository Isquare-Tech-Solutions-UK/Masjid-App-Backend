package com.masjidapp.controller.admin;

import com.masjidapp.dto.request.BulkPrayerTimeRequest;
import com.masjidapp.dto.request.CreatePrayerTimeRequest;
import com.masjidapp.dto.request.UpdatePrayerTimeRequest;
import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.BulkPrayerTimeResult;
import com.masjidapp.dto.response.MessageResponse;
import com.masjidapp.dto.response.PrayerTimeResponse;
import com.masjidapp.service.PrayerTimeService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/prayer-times")
@RequiredArgsConstructor
@Slf4j
public class AdminPrayerTimeController {

    private final PrayerTimeService prayerTimeService;

    /**
     * GET /admin/prayer-times
     * Get prayer times with pagination and optional date range filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrayerTimes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "31") int size) {

        log.info("Fetching prayer times - startDate: {}, endDate: {}, page: {}, size: {}", startDate, endDate, page,
                size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").ascending());
        Page<PrayerTimeResponse> result = prayerTimeService.getPrayerTimes(startDate, endDate, pageable);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("content", result.getContent());
        responseData.put("pagination", buildPagination(result));

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    /**
     * GET /admin/prayer-times/{id}
     * Get single prayer time entry by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrayerTimeResponse>> getPrayerTimeById(@PathVariable UUID id) {
        log.info("Fetching prayer time by ID: {}", id);

        PrayerTimeResponse prayerTime = prayerTimeService.getPrayerTimeById(id);
        return ResponseEntity.ok(ApiResponse.success(prayerTime));
    }

    /**
     * POST /admin/prayer-times
     * Create a new prayer time entry
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PrayerTimeResponse>> createPrayerTime(
            @Valid @RequestBody CreatePrayerTimeRequest request) {
        log.info("Creating prayer time for date: {}", request.getDate());

        PrayerTimeResponse created = prayerTimeService.createPrayerTime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    /**
     * POST /admin/prayer-times/bulk
     * Bulk create or update prayer times (upsert by date)
     */
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkPrayerTimeResult>> bulkCreatePrayerTimes(
            @Valid @RequestBody BulkPrayerTimeRequest request) {
        log.info("Bulk creating/updating {} prayer times", request.getPrayerTimes().size());

        BulkPrayerTimeResult result = prayerTimeService.bulkCreatePrayerTimes(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * PUT /admin/prayer-times/{id}
     * Update an existing prayer time entry
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PrayerTimeResponse>> updatePrayerTime(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePrayerTimeRequest request) {
        log.info("Updating prayer time with ID: {}", id);

        PrayerTimeResponse updated = prayerTimeService.updatePrayerTime(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * DELETE /admin/prayer-times/{id}
     * Delete a prayer time entry
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrayerTime(@PathVariable UUID id) {
        log.info("Deleting prayer time with ID: {}", id);

        prayerTimeService.deletePrayerTime(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Build pagination metadata from Spring's Page object
     */
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
