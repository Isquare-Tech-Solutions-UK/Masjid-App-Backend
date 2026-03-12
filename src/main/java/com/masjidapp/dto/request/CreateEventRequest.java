package com.masjidapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new event.
 * Bound from multipart/form-data using @ModelAttribute.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Speaker is required")
    @Size(max = 255, message = "Speaker must not exceed 255 characters")
    private String speaker;

    /**
     * Event date as ISO-8601 datetime string, e.g. 2025-02-15T18:00:00.
     * Will be parsed into LocalDateTime.
     */
    @NotBlank(message = "Date is required")
    private String date;

    @Size(max = 500, message = "Link must not exceed 500 characters")
    private String link;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    /**
     * Optional status, defaults to draft if not provided.
     * Allowed: draft, published.
     */
    private String status;
}


