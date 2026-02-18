package com.masjidapp.controller;

import com.masjidapp.dto.response.ApiResponse;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.service.EventService;
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
 * Member Events API with API key validation (handled by MemberApiKeyFilter).
 *
 * GET /member/events
 * - Only returns published events (no drafts).
 * - Supports status=upcoming/past, startDate, endDate, page, size.
 * - Sorted by start_time/date DESC.
 */
@RestController
@RequestMapping("/member/events")
@RequiredArgsConstructor
@Slf4j
public class MemberEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
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
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getMemberEventById(@PathVariable UUID id) {
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


