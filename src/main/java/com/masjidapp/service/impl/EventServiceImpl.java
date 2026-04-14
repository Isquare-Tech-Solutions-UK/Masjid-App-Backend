package com.masjidapp.service.impl;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.request.UpdateEventRequest;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.Event;
import com.masjidapp.entity.EventStatus;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.EventRepository;
import com.masjidapp.service.AuditLogService;
import com.masjidapp.service.EventService;
import com.masjidapp.service.FcmService;
import com.masjidapp.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final S3Service s3Service;
    private final AuditLogService auditLogService;
    private final FcmService fcmService;

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, List<MultipartFile> images, AdminUser createdBy) {
        log.debug("Creating event. title={}, speaker={}, createdBy={}",
                request.getTitle(), request.getSpeaker(), createdBy.getEmail());

        LocalDateTime eventDate = parseDate(request.getDate());
        EventStatus status = resolveStatus(request.getStatus());

        // Upload images to S3 and get URLs
        List<String> imageUrls = s3Service.uploadEventImages(images);

        Event event = Event.builder()
                .title(request.getTitle())
                .speaker(request.getSpeaker())
                .date(eventDate)
                .link(request.getLink())
                .images(imageUrls)
                .description(request.getDescription())
                .venue(request.getVenue())
                .status(status)
                .createdBy(createdBy)
                .build();

        if (status == EventStatus.published) {
            event.setPublishedAt(LocalDateTime.now());
        }

        Event saved = eventRepository.save(event);

        log.info("Event created successfully. id={}, title={}, status={}",
                saved.getId(), saved.getTitle(), saved.getStatus());

        return EventResponse.fromEntity(saved);
    }

    /**
     * Update an existing event by ID.
     * - Supports partial updates (only non-null fields are applied).
     * - Handles image replacement: deletes old images from S3, uploads new ones.
     * - Enforces status rule: cannot revert from published back to draft.
     * - Sets publishedAt timestamp when event is first published.
     * - Records an audit trail via AuditLogService.
     */
    @Override
    @Transactional
    public EventResponse updateEvent(
            UUID eventId,
            UpdateEventRequest request,
            List<MultipartFile> newImages,
            AdminUser updatedBy,
            String ipAddress,
            String userAgent) {

        log.debug("Updating event. id={}, updatedBy={}", eventId,
                updatedBy != null ? updatedBy.getEmail() : "unknown");

        // 1. Fetch existing event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Update failed: Event not found. id={}", eventId);
                    return new ResourceNotFoundException("Event not found with id: " + eventId);
                });

        // 2. Snapshot old state for audit log
        Event oldSnapshot = snapshotEvent(event);

        // 3. Apply partial field updates (only non-null / non-blank values)
        if (StringUtils.hasText(request.getTitle())) {
            event.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getSpeaker())) {
            event.setSpeaker(request.getSpeaker());
        }
        if (StringUtils.hasText(request.getDate())) {
            event.setDate(parseDate(request.getDate()));
        }
        if (StringUtils.hasText(request.getLink())) {
            event.setLink(request.getLink());
        }
        if (StringUtils.hasText(request.getDescription())) {
            event.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getVenue())) {
            event.setVenue(request.getVenue());
        }

        // 4. Validate and apply status change
        if (StringUtils.hasText(request.getStatus())) {
            EventStatus requestedStatus = resolveStatus(request.getStatus());
            EventStatus currentStatus = event.getStatus();

            // Business rule: cannot revert from published back to draft
            if (currentStatus == EventStatus.published && requestedStatus == EventStatus.draft) {
                log.warn("Invalid status transition: published -> draft. eventId={}", eventId);
                throw new IllegalArgumentException(
                        "Cannot change event status from 'published' back to 'draft'");
            }

            // Set publishedAt when event is first published
            if (requestedStatus == EventStatus.published && currentStatus != EventStatus.published) {
                event.setPublishedAt(LocalDateTime.now());
                log.info("Event published for the first time. id={}", eventId);
            }

            event.setStatus(requestedStatus);
        }

        // 5. Handle image replacement (delete old → upload new)
        boolean hasNewImages = newImages != null && !newImages.isEmpty();
        if (hasNewImages) {
            List<String> oldImages = event.getImages();
            if (oldImages != null && !oldImages.isEmpty()) {
                log.info("Deleting {} old image(s) from S3 for eventId={}", oldImages.size(), eventId);
                s3Service.deleteEventImages(oldImages);
            }
            List<String> uploadedUrls = s3Service.uploadEventImages(newImages);
            event.setImages(uploadedUrls);
            log.info("Uploaded {} new image(s) for eventId={}", uploadedUrls.size(), eventId);
        }

        // 6. Save the updated event
        Event saved = eventRepository.save(event);
        log.info("Event updated successfully. id={}, status={}", saved.getId(), saved.getStatus());

        // 7. Record audit trail
        auditLogService.logEventUpdate(updatedBy, oldSnapshot, saved, ipAddress, userAgent);

        return EventResponse.fromEntity(saved);
    }

    /**
     * Get all paginated events with filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAdminEvents(
            String status,
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable) {

        log.debug("Fetching all events - status={}, upcoming={}, past={}, startDate={}, endDate={}, search={}, page={}, size={}",
                status, upcoming, past, startDate, endDate, search, pageable.getPageNumber(), pageable.getPageSize());

        List<Event> allEvents = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        // Parse status for filtering (do NOT throw on invalid, just ignore)
        final EventStatus statusFilter;
        if (StringUtils.hasText(status)) {
            EventStatus parsed = null;
            try {
                parsed = EventStatus.valueOf(status.trim().toLowerCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid status filter value: {}", status);
            }
            statusFilter = parsed;
        } else {
            statusFilter = null;
        }

        // Filter in-memory then paginate
        List<Event> filtered = allEvents.stream()
                // status / upcoming / past
                .filter(event -> {
                    if (statusFilter != null && event.getStatus() != statusFilter) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(upcoming)) {
                        return !event.getDate().isBefore(now);
                    }
                    if (Boolean.TRUE.equals(past)) {
                        return event.getDate().isBefore(now);
                    }
                    return true;
                })
                // date range
                .filter(event -> {
                    if (startDate != null && endDate != null) {
                        return !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate);
                    }
                    return true;
                })
                // search title or description
                .filter(event -> {
                    if (StringUtils.hasText(search)) {
                        String term = search.toLowerCase();
                        boolean matchesTitle = event.getTitle() != null && event.getTitle().toLowerCase().contains(term);
                        boolean matchesDesc = event.getDescription() != null && event.getDescription().toLowerCase().contains(term);
                        return matchesTitle || matchesDesc;
                    }
                    return true;
                })
                // sort by start_time/date DESC
                .sorted(Comparator.comparing(Event::getDate).reversed())
                .collect(Collectors.toList());

        int total = filtered.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int fromIndex = Math.max(pageNumber * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<EventResponse> content;
        if (fromIndex >= total) {
            content = List.of();
        } else {
            content = filtered.subList(fromIndex, toIndex).stream()
                    .map(EventResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Get published events for members (API key protected).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getMemberEvents(
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching member events - upcoming={}, past={}, startDate={}, endDate={}, page={}, size={}",
                upcoming, past, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        List<Event> allEvents = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        List<Event> filtered = allEvents.stream()
                // only published events (exclude drafts and others)
                .filter(event -> event.getStatus() == EventStatus.published)
                // upcoming / past
                .filter(event -> {
                    if (Boolean.TRUE.equals(upcoming)) {
                        return !event.getDate().isBefore(now);
                    }
                    if (Boolean.TRUE.equals(past)) {
                        return event.getDate().isBefore(now);
                    }
                    return true;
                })
                // date range
                .filter(event -> {
                    if (startDate != null && endDate != null) {
                        return !event.getDate().isBefore(startDate) && !event.getDate().isAfter(endDate);
                    }
                    return true;
                })
                // sort by start_time/date DESC
                .sorted(Comparator.comparing(Event::getDate).reversed())
                .collect(Collectors.toList());

        int total = filtered.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int fromIndex = Math.max(pageNumber * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<EventResponse> content;
        if (fromIndex >= total) {
            content = List.of();
        } else {
            content = filtered.subList(fromIndex, toIndex).stream()
                    .map(EventResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        log.debug("Fetching event by ID={} ", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found with id: {}", eventId);
                    return new ResourceNotFoundException("Event not found with id: " + eventId);
                });

        log.info("Event retrieved successfully. id={}, title={}", event.getId(), event.getTitle());
        return EventResponse.fromEntity(event);
    }

    @Override
    @Transactional
    public int notifyEvent(UUID eventId) {
        log.debug("Sending push notification for event. id={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.published) {
            throw new IllegalArgumentException("Only published events can be notified. Current status: " + event.getStatus());
        }

        String body = String.format("Join us on %s at %s",
                event.getDate().toLocalDate(),
                event.getVenue());

        Map<String, String> data = new HashMap<>();
        data.put("type", "event");
        data.put("id", event.getId().toString());

        fcmService.sendToTopic("events", event.getTitle(), body, data);

        event.setNotificationSent(true);
        event.setNotificationSentAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Event notification sent via topic. id={}", eventId);
        return 1;
    }

    private LocalDateTime parseDate(String date) {
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            log.error("Invalid date format for event. value={}", date);
            throw new IllegalArgumentException("Invalid date format. Expected ISO-8601, e.g. 2025-02-15T18:00:00");
        }
    }

    private EventStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return EventStatus.draft;
        }
        try {
            // compare in lowercase to match DB enum values
            return EventStatus.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            log.error("Invalid event status provided. value={}", status);
            throw new IllegalArgumentException("Invalid status. Allowed values: draft, published");
        }
    }

    /**
     * Creates a shallow copy of an Event to snapshot its state before updates.
     * Used by the audit log to record what changed.
     */
    private Event snapshotEvent(Event event) {
        return Event.builder()
                .id(event.getId())
                .title(event.getTitle())
                .speaker(event.getSpeaker())
                .date(event.getDate())
                .link(event.getLink())
                .images(event.getImages() != null ? new java.util.ArrayList<>(event.getImages())
                        : new java.util.ArrayList<>())
                .description(event.getDescription())
                .venue(event.getVenue())
                .status(event.getStatus())
                .createdBy(event.getCreatedBy())
                .publishedAt(event.getPublishedAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteEvent(UUID eventId, AdminUser deletedBy) {
        log.debug("Deleting event. id={}, deletedBy={}", eventId, deletedBy.getEmail());
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new com.masjidapp.exception.EventNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.cancelled) {
            log.warn("Cannot delete non-cancelled event. id={}, status={}", eventId, event.getStatus());
            throw new com.masjidapp.exception.InvalidEventStateException(
                    "Only events with status CANCELLED can be deleted. Current status: " + event.getStatus());
        }

        // Soft delete
        event.setDeleted(true);
        event.setDeletedAt(LocalDateTime.now());
        event.setDeletedBy(deletedBy);
        eventRepository.save(event);
        
        log.info("Event soft-deleted successfully. id={}", eventId);
    }

    @Override
    @Transactional
    public EventResponse changeEventStatus(UUID eventId, String statusRaw, AdminUser updatedBy, String ipAddress, String userAgent) {
        log.debug("Changing event status. id={}, newStatus={}, updatedBy={}", eventId, statusRaw, updatedBy.getEmail());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        Event oldSnapshot = snapshotEvent(event);
        EventStatus requestedStatus = resolveStatus(statusRaw);
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == EventStatus.published && requestedStatus == EventStatus.draft) {
            throw new IllegalArgumentException("Cannot change event status from 'published' back to 'draft'");
        }

        if (requestedStatus == EventStatus.published && currentStatus != EventStatus.published) {
            event.setPublishedAt(LocalDateTime.now());
        }

        event.setStatus(requestedStatus);
        Event saved = eventRepository.save(event);
        
        auditLogService.logEventUpdate(updatedBy, oldSnapshot, saved, ipAddress, userAgent);
        log.info("Event status changed successfully. id={}, status={}", eventId, saved.getStatus());

        return EventResponse.fromEntity(saved);
    }
}


