package com.masjidapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                Server localServer = new Server()
                                .url("http://localhost:8080/api/v1")
                                .description("Local Server");

                Server devServer = new Server()
                        .url("https://3.6.40.125.nip.io/api/v1")
                                .description("Dev Server (AWS)");

                return new OpenAPI()
                                .servers(List.of(localServer, devServer))
                                .info(new Info()
                                                .title("Masjid App API")
                                                .version("1.0")
                                                .description("API documentation for the Masjid management application."))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth",
                                                                new SecurityScheme()
                                                                                .name("bearerAuth")
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT"))
                                                .addSecuritySchemes("apiKeyAuth",
                                                                new SecurityScheme()
                                                                                .name("X-API-KEY")
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)));
        }
}
