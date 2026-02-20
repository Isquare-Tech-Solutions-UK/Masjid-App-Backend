package com.masjidapp.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing event.
 * All fields are optional; only non-null values will be updated.
 * Bound from multipart/form-data using @ModelAttribute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Speaker must not exceed 255 characters")
    private String speaker;

    /**
     * Event date as ISO-8601 datetime string, e.g. 2025-02-15T18:00:00.
     * Will be parsed into LocalDateTime.
     */
    private String date;

    @Size(max = 500, message = "Link must not exceed 500 characters")
    private String link;

    private String description;

    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    /**
     * Optional status change.
     * Allowed: draft, published, cancelled, completed.
     */
    private String status;
}


