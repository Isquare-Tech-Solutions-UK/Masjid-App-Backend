package com.masjidapp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeStatusResponse {
    private String accountId;
    private boolean connected;
    private boolean onboardingComplete;
    private boolean acceptingDonations;
    private boolean payoutsEnabled;
}
