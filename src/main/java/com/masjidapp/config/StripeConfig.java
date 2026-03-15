package com.masjidapp.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(secretKey)) {
            Stripe.apiKey = secretKey;
            log.info("Stripe initialized successfully.");
        } else {
            log.warn("Stripe secret key not configured. Stripe features will be unavailable.");
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(secretKey);
    }
}
