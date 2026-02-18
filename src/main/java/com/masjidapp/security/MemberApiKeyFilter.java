package com.masjidapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * API Key filter for member endpoints.
 * Validates X-API-KEY header for /member/** requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-API-KEY";

    /**
     * Comma-separated list of valid API keys loaded from configuration.
     * Example in application.yml:
     * member:
     *   api:
     *     keys: key1,key2,key3
     */
    @Value("${member.api.key}")
    private String apiKeysConfig;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Only protect member endpoints
        if (!path.startsWith("/member/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_NAME);

        if (!isValidApiKey(apiKey)) {
            log.warn("Invalid or missing API key for member endpoint. path={}, apiKeyPresent={}",
                    path, apiKey != null);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing API key\"}}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        if (!StringUtils.hasText(apiKeysConfig)) {
            // Misconfiguration: no keys defined
            log.error("member.api.keys is not configured");
            return false;
        }

        Set<String> validKeys = new HashSet<>(
                Arrays.asList(apiKeysConfig.split(",")));

        return validKeys.contains(apiKey.trim());
    }
}


