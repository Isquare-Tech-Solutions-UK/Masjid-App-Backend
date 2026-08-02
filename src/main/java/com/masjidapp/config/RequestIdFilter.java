package com.masjidapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Tags every request with a correlation id and puts it in the MDC, so all log
 * lines produced while handling that request carry the same {@code requestId}.
 *
 * <p>With structured logging enabled the id lands in the JSON payload, making a
 * single request traceable end-to-end in Loki:
 * <pre>{@code {container="masjid-backend"} | json | requestId = "..."}</pre>
 *
 * <p>An inbound {@code X-Request-Id} is reused when present, so a id set by
 * nginx or forwarded by the frontend stitches the whole chain together. The id
 * is echoed back on the response to help correlate a user-reported failure.
 *
 * <p>Ordered ahead of the Spring Security chain so authentication failures are
 * logged with an id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Threads are pooled and reused — leaving the id behind would
            // mislabel the next request handled by this thread.
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
