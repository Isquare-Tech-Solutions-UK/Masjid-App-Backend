package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.masjidapp.entity.Announcement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnouncementResponse {
    private UUID id;
    private String title;
    private String message;
    private String status;
    private LocalDateTime scheduledAt;
    private Boolean notificationSent;
    private LocalDateTime notificationSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AnnouncementResponse fromEntity(Announcement announcement) {
        if (announcement == null) return null;

        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .message(announcement.getMessage())
                .status(announcement.getStatus() != null ? announcement.getStatus().name() : null)
                .scheduledAt(announcement.getScheduledAt())
                .notificationSent(announcement.getNotificationSent())
                .notificationSentAt(announcement.getNotificationSentAt())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
