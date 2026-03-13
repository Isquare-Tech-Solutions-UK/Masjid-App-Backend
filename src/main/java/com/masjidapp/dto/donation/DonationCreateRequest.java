package com.masjidapp.dto.donation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DonationCreateRequest {
    private String donorName;
    private String donorEmail;
    private BigDecimal amount;
    private String campaignId;
    private Boolean isAnonymous = Boolean.FALSE;
    private boolean coverFee = false;
    private String successUrl;
    private String cancelUrl;

}
