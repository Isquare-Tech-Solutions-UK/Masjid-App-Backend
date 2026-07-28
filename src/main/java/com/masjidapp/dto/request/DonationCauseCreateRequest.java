package com.masjidapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Request body for creating a new donation cause")
public class DonationCauseCreateRequest {

    @NotNull
    @Valid
    @Schema(description = "Donation cause payload", requiredMode = Schema.RequiredMode.REQUIRED)
    private DonationCauseData data;

    @Data
    @NoArgsConstructor
    public static class DonationCauseData {

        @NotBlank
        @Schema(description = "Name of the donation cause", example = "Charity & Welfare", requiredMode = Schema.RequiredMode.REQUIRED)
        private String cause;
    }
}