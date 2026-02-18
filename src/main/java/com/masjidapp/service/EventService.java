package com.masjidapp.service;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    /**
     * Create a new event with optional image uploads.
     */
    EventResponse createEvent(CreateEventRequest request, List<MultipartFile> images, AdminUser createdBy);

    /**
     * Get paginated events for a specific admin with filters.
     * - Only events created by the given admin are returned.
     * - Supports status, upcoming/past, and date range filters.
     */
    Page<EventResponse> getAdminEvents(
            AdminUser admin,
            String status,
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}


