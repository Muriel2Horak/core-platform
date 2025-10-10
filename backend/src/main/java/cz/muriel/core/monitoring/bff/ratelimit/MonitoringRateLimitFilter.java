package cz.muriel.core.monitoring.bff.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for monitoring BFF endpoints.
 * Limits requests per user (based on JWT subject) to prevent abuse.
 */
@Component
@Slf4j
public class MonitoringRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Rate limit: 100 requests per minute per user
    private static final int CAPACITY = 100;
    private static final int REFILL_TOKENS = 100;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Only apply to /api/monitoring/** endpoints
        if (!request.getRequestURI().startsWith("/api/monitoring/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // Let security layer handle unauthenticated requests
            filterChain.doFilter(request, response);
            return;
        }

        String userId = auth.getName();
        Bucket bucket = resolveBucket(userId);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for user: {} on endpoint: {}", userId, request.getRequestURI());
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
        }
    }

    private Bucket resolveBucket(String userId) {
        return cache.computeIfAbsent(userId, key -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(REFILL_TOKENS, REFILL_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
