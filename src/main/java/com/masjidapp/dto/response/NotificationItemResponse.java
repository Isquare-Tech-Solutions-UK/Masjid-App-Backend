package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class NotificationItemResponse {
    private UUID id;
    private String type;
    private String title;
    private String message;

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
