package com.masjidapp.service.impl;

import com.masjidapp.dto.request.CreateAnnouncementRequest;
import com.masjidapp.dto.request.UpdateAnnouncementRequest;
import com.masjidapp.dto.response.AnnouncementResponse;
import com.masjidapp.entity.AdminUser;
import com.masjidapp.entity.Announcement;
import com.masjidapp.entity.AnnouncementStatus;
import com.masjidapp.exception.ResourceNotFoundException;
import com.masjidapp.repository.AnnouncementRepository;
import com.masjidapp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, AdminUser createdBy) {
        log.debug("Creating announcement. title={}, createdBy={}", request.getTitle(), createdBy.getEmail());

        LocalDateTime scheduledAt = parseDate(request.getScheduledAt());
        AnnouncementStatus status = resolveStatus(request.getStatus());

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .scheduledAt(scheduledAt)
                .status(status)
                .createdBy(createdBy)
                .build();

        Announcement saved = announcementRepository.save(announcement);

        log.info("Announcement created successfully. id={}, title={}, status={}",
                saved.getId(), saved.getTitle(), saved.getStatus());

        return AnnouncementResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(
            UUID announcementId,
            UpdateAnnouncementRequest request,
            AdminUser updatedBy,
            String ipAddress,
            String userAgent) {

        log.debug("Updating announcement. id={}, updatedBy={}", announcementId,
                updatedBy != null ? updatedBy.getEmail() : "unknown");

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + announcementId));

        if (StringUtils.hasText(request.getTitle())) {
            announcement.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getMessage())) {
            announcement.setMessage(request.getMessage());
        }
        if (StringUtils.hasText(request.getScheduledAt())) {
            announcement.setScheduledAt(parseDate(request.getScheduledAt()));
        }

        if (StringUtils.hasText(request.getStatus())) {
            AnnouncementStatus requestedStatus = resolveStatus(request.getStatus());
            AnnouncementStatus currentStatus = announcement.getStatus();

            if (currentStatus == AnnouncementStatus.sent && requestedStatus != AnnouncementStatus.sent) {
                log.warn("Invalid status transition: sent -> {}. announcementId={}", requestedStatus, announcementId);
                throw new IllegalArgumentException("Cannot change announcement status from 'sent' to something else");
            }

            announcement.setStatus(requestedStatus);
        }

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement updated successfully. id={}, status={}", saved.getId(), saved.getStatus());

        return AnnouncementResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAdminAnnouncements(
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String search,
            Pageable pageable) {

        log.debug("Fetching admin announcements - status={}, search={}, page={}", status, search, pageable.getPageNumber());

        List<Announcement> allAnnouncements = announcementRepository.findAll();

        final AnnouncementStatus statusFilter;
        if (StringUtils.hasText(status)) {
            AnnouncementStatus parsed = null;
            try {
                parsed = AnnouncementStatus.valueOf(status.trim().toLowerCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring invalid status filter value: {}", status);
            }
            statusFilter = parsed;
        } else {
            statusFilter = null;
        }

        List<Announcement> filtered = allAnnouncements.stream()
                .filter(ann -> statusFilter == null || ann.getStatus() == statusFilter)
                .filter(ann -> {
                    if (startDate != null && endDate != null) {
                        return (ann.getScheduledAt() != null) && !ann.getScheduledAt().isBefore(startDate) && !ann.getScheduledAt().isAfter(endDate);
                    }
                    return true;
                })
                .filter(ann -> {
                    if (StringUtils.hasText(search)) {
                        String term = search.toLowerCase();
                        boolean matchesTitle = ann.getTitle() != null && ann.getTitle().toLowerCase().contains(term);
                        boolean matchesMessage = ann.getMessage() != null && ann.getMessage().toLowerCase().contains(term);
                        return matchesTitle || matchesMessage;
                    }
                    return true;
                })
                // Use creation date if scheduled at is null to ensure a stable sort
                .sorted(Comparator.comparing(
                        (Announcement ann) -> ann.getScheduledAt() != null ? ann.getScheduledAt() : ann.getCreatedAt()
                ).reversed())
                .collect(Collectors.toList());

        int total = filtered.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int fromIndex = Math.max(pageNumber * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<AnnouncementResponse> content = (fromIndex >= total) 
                ? List.of() 
                : filtered.subList(fromIndex, toIndex).stream()
                          .map(AnnouncementResponse::fromEntity)
                          .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getMemberAnnouncements(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching member announcements - startDate={}, endDate={}, page={}", startDate, endDate, pageable.getPageNumber());

        List<Announcement> allAnnouncements = announcementRepository.findAll();

        List<Announcement> filtered = allAnnouncements.stream()
                .filter(ann -> ann.getStatus() == AnnouncementStatus.sent)
                .filter(ann -> {
                    if (startDate != null && endDate != null) {
                        return (ann.getScheduledAt() != null) && !ann.getScheduledAt().isBefore(startDate) && !ann.getScheduledAt().isAfter(endDate);
                    }
                    return true;
                })
                .sorted(Comparator.comparing(
                        (Announcement ann) -> ann.getScheduledAt() != null ? ann.getScheduledAt() : ann.getCreatedAt()
                ).reversed())
                .collect(Collectors.toList());

        int total = filtered.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int fromIndex = Math.max(pageNumber * pageSize, 0);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<AnnouncementResponse> content = (fromIndex >= total)
                ? List.of()
                : filtered.subList(fromIndex, toIndex).stream()
                          .map(AnnouncementResponse::fromEntity)
                          .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(UUID announcementId) {
        log.debug("Fetching announcement by ID={} ", announcementId);

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + announcementId));

        return AnnouncementResponse.fromEntity(announcement);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(UUID announcementId, AdminUser deletedBy) {
        log.debug("Deleting announcement. id={}, deletedBy={}", announcementId, deletedBy.getEmail());
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + announcementId));

        if (announcement.getStatus() == AnnouncementStatus.sent) {
            log.warn("Cannot delete sent announcement. id={}", announcementId);
            throw new IllegalArgumentException("Cannot delete announcements that have already been sent.");
        }

        announcementRepository.delete(announcement);
        log.info("Announcement deleted successfully. id={}", announcementId);
    }

    @Override
    @Transactional
    public AnnouncementResponse changeAnnouncementStatus(UUID announcementId, String statusRaw, AdminUser updatedBy, String ipAddress, String userAgent) {
        log.debug("Changing announcement status. id={}, newStatus={}, updatedBy={}", announcementId, statusRaw, updatedBy.getEmail());

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + announcementId));

        AnnouncementStatus requestedStatus = resolveStatus(statusRaw);
        AnnouncementStatus currentStatus = announcement.getStatus();

        if (currentStatus == AnnouncementStatus.sent && requestedStatus != AnnouncementStatus.sent) {
            throw new IllegalArgumentException("Cannot change announcement status from 'sent'");
        }

        announcement.setStatus(requestedStatus);
        Announcement saved = announcementRepository.save(announcement);
        
        log.info("Announcement status changed successfully. id={}, status={}", announcementId, saved.getStatus());

        return AnnouncementResponse.fromEntity(saved);
    }

    private LocalDateTime parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            log.error("Invalid date format for announcement. value={}", date);
            throw new IllegalArgumentException("Invalid date format. Expected ISO-8601, e.g. 2025-02-15T18:00:00");
        }
    }

    private AnnouncementStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return AnnouncementStatus.draft;
        }
        try {
            return AnnouncementStatus.valueOf(status.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            log.error("Invalid announcement status provided. value={}", status);
            throw new IllegalArgumentException("Invalid status. Allowed values: draft, scheduled, sent");
        }
    }
}
