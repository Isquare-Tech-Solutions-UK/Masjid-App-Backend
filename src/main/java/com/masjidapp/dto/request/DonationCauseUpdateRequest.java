package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Request body for updating a donation cause")
public class DonationCauseUpdateRequest {

    @NotBlank
    @Schema(description = "Existing name of the cause to update", example = "Masjid Development")
    private String oldCause;

    @NotBlank
    @Schema(description = "New name for the donation cause", example = "Masjid Renovation")
    private String newCause;
}
