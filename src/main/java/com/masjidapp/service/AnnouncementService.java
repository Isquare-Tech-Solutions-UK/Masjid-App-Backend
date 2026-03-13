package com.masjidapp.service;

import com.masjidapp.dto.request.CreateAnnouncementRequest;
import com.masjidapp.dto.request.UpdateAnnouncementRequest;
import com.masjidapp.dto.response.AnnouncementResponse;
import com.masjidapp.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AnnouncementService {

    AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request, AdminUser createdBy);

    AnnouncementResponse updateAnnouncement(UUID announcementId, UpdateAnnouncementRequest request, AdminUser updatedBy, String ipAddress, String userAgent);

    Page<AnnouncementResponse> getAdminAnnouncements(String status, LocalDateTime startDate, LocalDateTime endDate, String search, Pageable pageable);

    Page<AnnouncementResponse> getMemberAnnouncements(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    AnnouncementResponse getAnnouncementById(UUID announcementId);

    void deleteAnnouncement(UUID announcementId, AdminUser deletedBy);

    AnnouncementResponse changeAnnouncementStatus(UUID announcementId, String statusRaw, AdminUser updatedBy, String ipAddress, String userAgent);
}
