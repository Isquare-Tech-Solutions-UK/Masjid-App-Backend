package com.masjidapp.config;

import com.masjidapp.security.JwtAuthenticationFilter;
import com.masjidapp.security.MemberApiKeyFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final MemberApiKeyFilter memberApiKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/admin/auth/login",
                                "/admin/auth/refresh",
                                "/admin/auth/logout",
                                "/actuator/health",
                                // Swagger UI & OpenAPI docs
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/api-docs/**",
                                "/api-docs",
                                // TODO (PRODUCTION SECURITY): public ONLY for development/testing of a
                                // browser-based FCM receiver (NotificationSubscriptionController). Before
                                // production, do at least one of: require authentication on these routes,
                                // restrict "topic" to an allow-list (e.g. prayer-updates) instead of
                                // accepting any client-supplied topic, or protect with a dedicated API key.
                                // Do not allow arbitrary topic subscriptions in production.
                                "/notifications/subscribe",
                                "/notifications/unsubscribe")
                        .permitAll()

                        // Member endpoints - API Key authentication (handled by MemberApiKeyFilter)
                        .requestMatchers("/member/**").permitAll()

                        // Webhook endpoints - authenticated by Stripe signature (see StripeWebhookController), not JWT
                        .requestMatchers("/webhooks/**").permitAll()

                        // All other admin endpoints require JWT authentication
                        .requestMatchers("/admin/**").authenticated()

                        // Any other request
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                // First enforce API key for /member/** paths
                .addFilterBefore(memberApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // Then handle JWT auth for /admin/** paths
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Unauthorized\"}}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Access Denied\"}}");
                        }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // The API now lives on its own subdomain, so calls from the frontend are
        // cross-origin and CORS actually applies — previously they were
        // same-origin (/api/ on the same host) and this list was never
        // exercised in production.
        configuration.setAllowedOriginPatterns(List.of(
                "https://masjid-app.isquaretechsolutions.com",
                "http://localhost:3000",
                "http://localhost:3001", // Masjid-Notify-Web (dev) — run alongside Admin Web on 3000
                "https://masjid-app.vercel.app",
                "https://*.vercel.app" // Allows Vercel preview deployments
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        // Allowed headers cover the inbound direction; this makes the id the
        // server actually used readable by the browser on cross-origin calls.
        configuration.setExposedHeaders(List.of(RequestIdFilter.REQUEST_ID_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
