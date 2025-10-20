package cz.muriel.core.monitoring;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint for Nginx auth_request
 * 
 * Returns 200 + Grafana-JWT header if authenticated Returns 401 if not
 * authenticated
 * 
 * Security: - Rate limited (20 req/min per user) - Internal only (Nginx should
 * restrict access) - No PII in logs - Reads JWT from HTTP-only cookie (not
 * Authorization header)
 */
@RestController @RequestMapping("/internal/auth") @Slf4j
public class AuthRequestController {

  private static final String ACCESS_COOKIE = "at"; // Same as AuthController

  private final GrafanaJwtService jwtService;
  private final JwtDecoder jwtDecoder;

  public AuthRequestController(GrafanaJwtService jwtService, JwtDecoder jwtDecoder) {
    this.jwtService = jwtService;
    this.jwtDecoder = jwtDecoder;
  }

  /**
   * Nginx auth_request endpoint
   * 
   * Called by Nginx before proxying to Grafana Reads JWT from HTTP-only cookie,
   * validates it, and mints short-lived Grafana JWT
   */
  @GetMapping("/grafana") @RateLimiter(name = "grafana-auth", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<Void> authenticateForGrafana(HttpServletRequest request) {

    // Read JWT token from HTTP-only cookie
    String token = getCookieValue(request, ACCESS_COOKIE);

    if (token == null || token.isEmpty()) {
      log.debug("Grafana auth request failed: no auth cookie found");
      return ResponseEntity.status(401).build();
    }

    try {
      // Decode and validate JWT
      Jwt jwt = jwtDecoder.decode(token);

      if (jwt == null || !jwt.getExpiresAt().isAfter(java.time.Instant.now())) {
        log.debug("Grafana auth request failed: expired or invalid JWT");
        return ResponseEntity.status(401).build();
      }

      // Mint short-lived Grafana JWT
      String grafanaJwt = jwtService.mintGrafanaJwtFromKeycloakJwt(jwt);

      return ResponseEntity.ok().header("Grafana-JWT", grafanaJwt).build();

    } catch (Exception e) {
      log.error("Failed to mint Grafana JWT: {}", e.getMessage());
      return ResponseEntity.status(500).build();
    }
  }

  /**
   * Rate limit fallback
   */
  public ResponseEntity<Void> rateLimitFallback(HttpServletRequest request, Exception e) {
    log.warn("Rate limit exceeded for Grafana auth from IP: {}", request.getRemoteAddr());
    return ResponseEntity.status(429).build();
  }

  /**
   * Extract cookie value from request
   */
  private String getCookieValue(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return null;
    }

    for (Cookie cookie : request.getCookies()) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }
}
