package cz.muriel.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 🏢 Tenant Logging Interceptor Automatically adds tenant and user context to
 * all backend logs via MDC
 */
@Component
public class TenantLoggingInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(TenantLoggingInterceptor.class);

  // MDC keys for tenant context
  public static final String MDC_TENANT = "tenant";
  public static final String MDC_USERNAME = "username";
  public static final String MDC_USER_ID = "userId";
  public static final String MDC_REQUEST_ID = "requestId";

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
    try {
      // Generate unique request ID
      String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
      MDC.put(MDC_REQUEST_ID, requestId);

      // Extract tenant and user context from JWT
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        // Extract tenant
        String tenant = extractTenantFromJwt(jwt);
        MDC.put(MDC_TENANT, tenant);

        // Extract username
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null) {
          MDC.put(MDC_USERNAME, username);
        }

        // Extract user ID (subject)
        String userId = jwt.getSubject();
        if (userId != null) {
          MDC.put(MDC_USER_ID, userId);
        }

        logger.debug("🏢 Tenant context set for request: tenant={}, username={}, requestId={}",
            tenant, username, requestId);
      } else {
        // No authentication or JWT - set defaults
        MDC.put(MDC_TENANT, "anonymous");
        MDC.put(MDC_USERNAME, "anonymous");
        MDC.put(MDC_USER_ID, "anonymous");

        logger.debug("🏢 Anonymous context set for request: requestId={}", requestId);
      }

    } catch (Exception e) {
      // Don't fail the request if MDC setup fails
      logger.warn("⚠️ Failed to set tenant context in MDC: {}", e.getMessage());

      // Set fallback values
      MDC.put(MDC_TENANT, "error");
      MDC.put(MDC_USERNAME, "error");
      MDC.put(MDC_USER_ID, "error");
    }

    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex)
      throws Exception {
    // Clean up MDC after request completion
    try {
      String requestId = MDC.get(MDC_REQUEST_ID);
      if (requestId != null) {
        logger.debug("🧹 Cleaning up tenant context for request: requestId={}", requestId);
      }
    } finally {
      // Always clear MDC to prevent memory leaks
      MDC.clear();
    }
  }

  /**
   * Extract tenant from JWT token
   */
  private String extractTenantFromJwt(Jwt jwt) {
    if (jwt == null)
      return "unknown";

    // Try direct tenant claim first
    String tenant = jwt.getClaimAsString("tenant");
    if (tenant != null && !tenant.isEmpty()) {
      return tenant;
    }

    // Fallback: extract from issuer URL
    String issuer = jwt.getClaimAsString("iss");
    if (issuer != null && issuer.contains("/realms/")) {
      tenant = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
      // Clean up any trailing slashes or query parameters
      if (tenant.contains("/")) {
        tenant = tenant.substring(0, tenant.indexOf("/"));
      }
      if (tenant.contains("?")) {
        tenant = tenant.substring(0, tenant.indexOf("?"));
      }

      if (!tenant.isEmpty()) {
        return tenant;
      }
    }

    return "unknown";
  }
}
