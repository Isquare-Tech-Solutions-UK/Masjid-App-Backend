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

    // Estimate of Stripe's own processing fee, used to gross up the charge so the
    // charity always receives the full donation amount. This is NOT a platform fee.
    // Default: UK card rate — 1.5% + £0.20. The actual fee is reconciled after the
    // charge settles (see StripeWebhookServiceImpl#fetchAndStoreActualFee).
    private BigDecimal feePercent = new BigDecimal("0.015");
    private BigDecimal feeFixed = new BigDecimal("0.20");
}
