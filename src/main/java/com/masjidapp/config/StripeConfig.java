package com.masjidapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.stripe")
@Getter
@Setter
public class StripeConfig {

    private String apiKey;
    private String signingSecret;
}
