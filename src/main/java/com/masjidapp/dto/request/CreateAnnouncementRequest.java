package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new announcement")
public class CreateAnnouncementRequest {

    @Schema(description = "Title of the announcement", example = "Masjid Maintenance Work Tomorrow")
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Schema(description = "Message/Details of the announcement", example = "Please note that the masjid will undergo light maintenance tomorrow...")
    @NotBlank(message = "Message is required")
    private String message;

    @Schema(description = "Scheduled date and time (ISO-8601)", example = "2025-10-17T09:00:00")
    private String scheduledAt;

    @Schema(description = "Status of the announcement (draft, scheduled, sent)", example = "draft")
    private String status;
}
