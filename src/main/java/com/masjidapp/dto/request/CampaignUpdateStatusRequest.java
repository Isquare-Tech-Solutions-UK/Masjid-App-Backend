package com.masjidapp.dto.request;

import com.masjidapp.entity.CampaignStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request body for updating a campaign's status")
public class CampaignUpdateStatusRequest {

    @Schema(description = "New campaign status", example = "paused",
            allowableValues = {"draft", "active", "paused", "completed", "cancelled"})
    private CampaignStatus status = CampaignStatus.draft;
}
