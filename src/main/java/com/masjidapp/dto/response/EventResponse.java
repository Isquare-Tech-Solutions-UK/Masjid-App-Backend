package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.masjidapp.entity.Event;
import com.masjidapp.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("speaker")
    private String speaker;

    @JsonProperty("date")
    private LocalDateTime date;

    @JsonProperty("link")
    private String link;

    @JsonProperty("images")
    private List<String> images;

    @JsonProperty("description")
    private String description;

    @JsonProperty("venue")
    private String venue;

    @JsonProperty("status")
    private EventStatus status;

    @JsonProperty("notificationSent")
    private boolean notificationSent;

    @JsonProperty("notificationSentAt")
    private LocalDateTime notificationSentAt;

    @JsonProperty("createdBy")
    private UUID createdBy;

    @JsonProperty("publishedAt")
    private LocalDateTime publishedAt;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Convert Event entity to EventResponse DTO.
     */
    public static EventResponse fromEntity(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .speaker(event.getSpeaker())
                .date(event.getDate())
                .link(event.getLink())
                .images(event.getImages())
                .description(event.getDescription())
                .venue(event.getVenue())
                .status(event.getStatus())
                .notificationSent(event.isNotificationSent())
                .notificationSentAt(event.getNotificationSentAt())
                .createdBy(event.getCreatedBy() != null ? event.getCreatedBy().getId() : null)
                .publishedAt(event.getPublishedAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}


