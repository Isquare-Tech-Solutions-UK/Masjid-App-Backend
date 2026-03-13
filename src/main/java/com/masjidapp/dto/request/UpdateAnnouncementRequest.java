package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating an announcement")
public class UpdateAnnouncementRequest {

    @Schema(description = "Title of the announcement", example = "Updated Maintenance Work")
    private String title;

    @Schema(description = "Message/Details of the announcement", example = "Updated: Maintenance work will begin at 10 AM.")
    private String message;

    @Schema(description = "Scheduled date and time (ISO-8601)", example = "2025-10-17T09:00:00")
    private String scheduledAt;

    @Schema(description = "Status of the announcement (draft, scheduled, sent)", example = "scheduled")
    private String status;
}
