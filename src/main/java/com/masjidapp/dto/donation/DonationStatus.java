package com.masjidapp.dto.donation;

public enum DonationStatus {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed"),
    REFUNDED("refunded");

    private String name;

    DonationStatus(String name) { this.name = name; }

    public String getName() { return name; }

}
