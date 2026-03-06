package com.masjidapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "app.stripe")
@Getter
@Setter
public class StripeConfig {

    private String clientId;
    private String secretKey;
    private String signingSecret;
    private String oauthRedirectUri;

    // Fee charged to the donor (covers Stripe's processing cost)
    // Default: UK card rate — 1.5% + £0.20
    private BigDecimal feePercent = new BigDecimal("0.015");
    private BigDecimal feeFixed = new BigDecimal("0.20");
}
