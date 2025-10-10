package cz.muriel.core.monitoring.bff.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Audit logging filter for monitoring BFF.
 * Logs tenant, user, endpoint, and duration for all monitoring API calls.
 */
@Component
@Slf4j
public class MonitoringAuditFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Only audit /api/monitoring/** endpoints
        if (!request.getRequestURI().startsWith("/api/monitoring/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String tenantId = "unknown";
        String userId = "anonymous";

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
                tenantId = extractTenantId(jwt);
            }

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            log.info("AUDIT: tenant={}, user={}, method={}, endpoint={}, status={}, duration={}ms",
                    tenantId, userId, request.getMethod(), request.getRequestURI(), status, duration);
        }
    }

    private String extractTenantId(Jwt jwt) {
        // Try multiple claim locations
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId != null) return tenantId;

        tenantId = jwt.getClaimAsString("tenant");
        if (tenantId != null) return tenantId;

        // Fallback to realm from preferred_username
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && preferredUsername.contains("@")) {
            return preferredUsername.split("@")[1];
        }

        return "unknown";
    }
}
