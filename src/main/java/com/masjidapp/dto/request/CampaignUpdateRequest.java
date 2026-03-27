package com.masjidapp.dto.request;

import com.masjidapp.entity.CampaignStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CampaignUpdateRequest {
    @NotNull
    private String title;
    private String description;
    private String category;
    @NotNull
    private BigDecimal goalAmount;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    private CampaignStatus status = CampaignStatus.draft;
}
