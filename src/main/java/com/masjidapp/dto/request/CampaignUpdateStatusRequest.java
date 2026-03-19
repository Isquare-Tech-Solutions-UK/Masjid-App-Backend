package com.masjidapp.dto.request;

import com.masjidapp.entity.CampaignStatus;
import lombok.Data;

@Data
public class CampaignUpdateStatusRequest {
    private CampaignStatus status = CampaignStatus.draft;
}
