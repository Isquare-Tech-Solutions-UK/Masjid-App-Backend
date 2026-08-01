package com.masjidapp.dto.donation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body for making a donation to a campaign")
public class DonationCreateRequest {

    @NotBlank
    @Schema(description = "Donor's full name", example = "Ahmed Ali", requiredMode = Schema.RequiredMode.REQUIRED)
    private String donorName;

    @NotBlank
    @Schema(description = "Donor's email address", example = "ahmed@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String donorEmail;

    @NotNull
    @Positive
    @Schema(description = "Donation amount in GBP", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Whether to donate anonymously", example = "false", defaultValue = "false")
    private Boolean isAnonymous = Boolean.FALSE;
}
