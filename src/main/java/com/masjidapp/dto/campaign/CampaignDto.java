package com.masjidapp.dto.campaign;

import com.masjidapp.dto.response.AdminUserResponse;
import com.masjidapp.entity.Campaign;
import com.masjidapp.entity.CampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CampaignDto {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private CampaignStatus status;
    private BigDecimal goalAmount;
    private BigDecimal raisedAmount;
    private Integer donorCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private AdminUserResponse createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant endedAt;
    private Instant publishedAt;

    public static CampaignDto toDto(Campaign campaign) {
        return CampaignDto.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .category(campaign.getCategory())
                .status(campaign.getStatus())
                .goalAmount(campaign.getGoalAmount())
                .raisedAmount(campaign.getRaisedAmount())
                .donorCount(campaign.getDonorCount())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .createdBy(campaign.getCreatedBy() != null ?
                        AdminUserResponse.fromEntity(campaign.getCreatedBy()) : null)
                .publishedAt(campaign.getPublishedAt())
                .endedAt(campaign.getEndedAt())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }
}
