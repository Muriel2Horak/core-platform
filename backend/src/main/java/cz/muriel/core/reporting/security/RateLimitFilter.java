package cz.muriel.core.reporting.security;

import cz.muriel.core.reporting.cube.CubeSecurityContext;
import cz.muriel.core.reporting.support.ReportingMetrics;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate limiting filter using Bucket4j.
 */
@Slf4j 
@Component 
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitFilter extends OncePerRequestFilter {

  private final ProxyManager<String> buckets;
  private final CubeSecurityContext cubeSecurityContext;
  private final ReportingMetrics metrics;

  private static final int CAPACITY = 120; // tokens per minute
  private static final Duration REFILL_DURATION = Duration.ofSeconds(60);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Only apply to /api/reports/** endpoints
    String path = request.getRequestURI();
    if (!path.startsWith("/api/reports/")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Extract tenant ID
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    String tenantId = cubeSecurityContext.extractTenantId(authentication);
    if (tenantId == null) {
      log.warn("No tenant ID found in authentication for rate limiting");
      filterChain.doFilter(request, response);
      return;
    }

    // Get or create bucket for tenant
    String bucketKey = "rate-limit:tenant:" + tenantId;
    Bucket bucket = buckets.builder().build(bucketKey, getBucketConfiguration());

    // Try to consume 1 token
    if (bucket.tryConsume(1)) {
      // Add rate limit headers
      long availableTokens = bucket.getAvailableTokens();
      response.addHeader("X-RateLimit-Limit", String.valueOf(CAPACITY));
      response.addHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));

      filterChain.doFilter(request, response);
    } else {
      // Rate limit exceeded
      log.warn("Rate limit exceeded for tenant: {}", tenantId);
      metrics.recordRateLimitExceeded(tenantId);

      response.setStatus(429); // Too Many Requests
      response.addHeader("X-RateLimit-Limit", String.valueOf(CAPACITY));
      response.addHeader("X-RateLimit-Remaining", "0");
      response.addHeader("Retry-After", "60");
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}");
    }
  }

  /**
   * Create bucket configuration with refill policy.
   */
  private Supplier<BucketConfiguration> getBucketConfiguration() {
    return () -> BucketConfiguration.builder()
        .addLimit(limit -> limit.capacity(CAPACITY).refillGreedy(CAPACITY, REFILL_DURATION))
        .build();
  }
}
