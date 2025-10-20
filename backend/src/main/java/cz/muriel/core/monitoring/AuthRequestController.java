package cz.muriel.core.monitoring;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
 * restrict access) - No PII in logs
 */
@RestController @RequestMapping("/internal/auth") @Slf4j
public class AuthRequestController {

  private final GrafanaJwtService jwtService;

  public AuthRequestController(GrafanaJwtService jwtService) {
    this.jwtService = jwtService;
  }

  /**
   * Nginx auth_request endpoint
   * 
   * Called by Nginx before proxying to Grafana Mints short-lived JWT for Grafana
   * SSO
   */
  @GetMapping("/grafana") @RateLimiter(name = "grafana-auth", fallbackMethod = "rateLimitFallback")
  public ResponseEntity<Void> authenticateForGrafana(Authentication authentication) {

    if (authentication == null || !authentication.isAuthenticated()) {
      log.debug("Grafana auth request failed: not authenticated");
      return ResponseEntity.status(401).build();
    }

    try {
      String grafanaJwt = jwtService.mintGrafanaJwt(authentication);

      return ResponseEntity.ok().header("Grafana-JWT", grafanaJwt).build();

    } catch (Exception e) {
      log.error("Failed to mint Grafana JWT for user: {}", authentication.getName(), e);
      return ResponseEntity.status(500).build();
    }
  }

  /**
   * Rate limit fallback
   */
  public ResponseEntity<Void> rateLimitFallback(Authentication authentication, Exception e) {
    log.warn("Rate limit exceeded for Grafana auth: user={}",
        authentication != null ? authentication.getName() : "unknown");
    return ResponseEntity.status(429).build();
  }
}
