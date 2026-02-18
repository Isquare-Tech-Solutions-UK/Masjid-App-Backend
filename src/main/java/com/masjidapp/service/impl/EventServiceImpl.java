package com.masjidapp.service.impl;

import com.masjidapp.dto.request.CreateEventRequest;
import com.masjidapp.dto.response.EventResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.Event;
import com.masjidapp.entity.EventStatus;
import com.masjidapp.repository.EventRepository;
import com.masjidapp.service.EventService;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final S3Service s3Service;

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
     * Get paginated events for a specific admin with filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getAdminEvents(
            AdminUser admin,
            String status,
            Boolean upcoming,
            Boolean past,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching events for admin={} - status={}, upcoming={}, past={}, startDate={}, endDate={}, page={}, size={}",
                admin.getId(), status, upcoming, past, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize());

        List<Event> allEvents = eventRepository.findAll();
        log.info("all events {}",allEvents);
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
                // only events created by this admin
                .filter(event -> event.getCreatedBy() != null
                        && event.getCreatedBy().getId().equals(admin.getId()))
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
}


