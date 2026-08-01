package com.masjidapp.dto.request;

import com.masjidapp.entity.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Schema(description = "Request body for creating a new campaign")
public class CampaignCreateRequest {

    @NotNull
    @Schema(description = "Campaign title", example = "Ramadan Food Drive", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Campaign description", example = "Providing food packages to families in need during Ramadan.")
    private String description;

    @Schema(description = "Campaign category", example = "Charity")
    private String category;

    @NotNull
    @Positive
    @Schema(description = "Fundraising goal amount in GBP", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal goalAmount;

    @NotNull
    @Schema(description = "Campaign start date", example = "2026-03-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Campaign end date", example = "2026-06-30")
    private LocalDate endDate;

    @Schema(description = "Initial status — draft or active", example = "active", defaultValue = "draft")
    private CampaignStatus status = CampaignStatus.draft;
}
