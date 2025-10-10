package cz.muriel.core.reporting.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to add request context to MDC for structured logging.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingContextFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String TENANT_ID = "tenantId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Generate request ID
            String requestId = UUID.randomUUID().toString();
            MDC.put(REQUEST_ID, requestId);

            // Add request info
            MDC.put(REQUEST_URI, request.getRequestURI());
            MDC.put(REQUEST_METHOD, request.getMethod());

            // Add request ID to response headers
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC
            MDC.remove(REQUEST_ID);
            MDC.remove(TENANT_ID);
            MDC.remove(USER_ID);
            MDC.remove(REQUEST_URI);
            MDC.remove(REQUEST_METHOD);
        }
    }

    /**
     * Add tenant ID to MDC.
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null) {
            MDC.put(TENANT_ID, tenantId);
        }
    }

    /**
     * Add user ID to MDC.
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Get request ID from MDC.
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }
}
