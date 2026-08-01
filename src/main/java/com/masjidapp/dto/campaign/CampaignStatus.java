package com.masjidapp.dto.campaign;

public enum CampaignStatus {
    DRAFT("draft"),
    ACTIVE("active"),
    PAUSED("paused"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    private String name;

    CampaignStatus(String name) { this.name = name; }

    public String getName() { return name; }

}
