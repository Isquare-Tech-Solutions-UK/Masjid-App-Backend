package com.masjidapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Masjid App API")
                        .version("1.0.0")
                        .description("REST API for the Masjid management and donation platform. " +
                                "Admin endpoints require a Bearer JWT token. " +
                                "Member endpoints require an X-API-KEY header.")
                        .contact(new Contact()
                                .name("Masjid App Support")
                                .email("support@masjidapp.com")))
                .tags(List.of(
                        new Tag().name("Auth").description("Admin authentication — login, token refresh, logout"),
                        new Tag().name("Admin Settings").description("Masjid profile, payment and settings management"),
                        new Tag().name("Stripe Settings").description("Configure the charity's own Stripe account keys"),
                        new Tag().name("Campaigns (Admin)").description("Admin campaign management"),
                        new Tag().name("Admin Prayer Times").description("Admin prayer time management"),
                        new Tag().name("Admin Events").description("Admin event management"),
                        new Tag().name("Admin Announcements").description("Admin announcement management"),
                        new Tag().name("Donations").description("Member donation and campaign APIs"),
                        new Tag().name("Member Prayer Times").description("Member prayer time access"),
                        new Tag().name("Member Events").description("Member event access"),
                        new Tag().name("Member Announcements").description("Member announcement access"),
                        new Tag().name("Member Masjid").description("Member masjid info access"),
                        new Tag().name("Member Stripe").description("Stripe publishable key for the mobile app"),
                        new Tag().name("Stripe Webhooks").description("Stripe event webhooks — internal use only")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from POST /admin/auth/login"))
                        .addSecuritySchemes("apiKeyAuth",
                                new SecurityScheme()
                                        .name("X-API-KEY")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Static API key for member-facing endpoints")));
    }
}
