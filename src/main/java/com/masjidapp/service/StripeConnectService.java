package com.masjidapp.service;

import com.masjidapp.dto.response.StripeStatusResponse;

public interface StripeConnectService {
    String getOAuthUrl(String redirectUri);
    void handleOAuthCallback(String code, String redirectUri);
    StripeStatusResponse getStatus();
    void disconnect();
}
