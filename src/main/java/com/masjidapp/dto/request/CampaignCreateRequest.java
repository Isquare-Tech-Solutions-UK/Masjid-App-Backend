package com.masjidapp.dto.request;

import com.masjidapp.entity.CampaignStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CampaignCreateRequest {
    @NotNull
    private String title;
    private String description;
    private String category;
    @NotNull
    @Positive
    private BigDecimal goalAmount;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    private CampaignStatus status = CampaignStatus.draft;
}
