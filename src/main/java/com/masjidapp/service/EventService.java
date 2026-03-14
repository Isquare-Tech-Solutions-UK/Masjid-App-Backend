package com.masjidapp.service;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.request.UpdateEventRequest;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventService {

    /**
     * Create a new event with optional image uploads.
     */
    EventResponse createEvent(CreateEventRequest request, List<MultipartFile> images, AdminUser createdBy);

    /**
     * Update an existing event by ID.
     * - Supports partial updates (only non-null fields are changed).
     * - Handles optional image replacement.
     * - Enforces business rules on status transitions.
     */
    EventResponse updateEvent(
            UUID eventId,
            UpdateEventRequest request,
            List<MultipartFile> newImages,
            AdminUser updatedBy,
            String ipAddress,
            String userAgent);

    /**
     * Delete a draft event.
     */
    void deleteEvent(UUID eventId, AdminUser deletedBy);

    /**
     * Change the status of an event.
     */
    EventResponse changeEventStatus(UUID eventId, String status, AdminUser updatedBy, String ipAddress, String userAgent);

    /**
     * Get all paginated events with filters.
     * - Returns all events (no admin filtering).
     * - Supports status, upcoming/past, and date range filters.
     */
    Page<EventResponse> getAdminEvents(
            String status,
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable);

    /**
     * Get a single event by ID.
     * - Only returns the event if it was created by the given admin.
     * - Throws ResourceNotFoundException if event not found or doesn't belong to admin.
     */
    EventResponse getEventById(UUID eventId);

    /**
     * Get published events for members (API key protected).
     * - Only returns events with status = published.
     * - Supports upcoming/past and date range filters.
     */
    Page<EventResponse> getMemberEvents(
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    /**
     * Send a push notification for a published event to all registered devices.
     * - Can only notify published events.
     * - Sets notification_sent = true and records notification_sent_at.
     * - Returns the number of devices successfully notified.
     */
    int notifyEvent(UUID eventId);
}


