package com.masjidapp.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    /**
     * There is deliberately no default secret: a missing JWT_SECRET must stop
     * the app at startup instead of signing tokens with a value from git.
     * Checked here because @ConfigurationProperties binding does not fail on
     * a missing value the way @Value does.
     */
    @PostConstruct
    void requireSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) is not set; refusing to start without a token signing key");
        }
    }

}
