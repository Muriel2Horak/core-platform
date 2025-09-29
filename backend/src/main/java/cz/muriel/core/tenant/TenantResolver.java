package cz.muriel.core.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class TenantResolver {

  /**
   * 🎯 SIMPLE ARCHITECTURE: Resolve tenant key from JWT realm JWT realm = tenant
   * key (much simpler than complex header/hostname logic)
   */
  public String resolveTenantKey() {
    String tenantKey = getTenantFromJwtRealm();

    if (tenantKey != null) {
      log.debug("🔐 Tenant resolved from JWT realm: {}", tenantKey);
      return tenantKey;
    }

    // 🔒 STRICT SECURITY: If no tenant resolved, FAIL
    log.warn("🚫 No tenant could be resolved from JWT realm");
    throw new TenantNotFoundException("Tenant could not be determined - access denied");
  }

  /**
   * 🔐 JWT REALM: Extract tenant from JWT realm (realm = tenant)
   */
  private String getTenantFromJwtRealm() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        // Use 'iss' (issuer) claim which contains realm info
        String issuer = jwt.getIssuer().toString();
        if (issuer != null && issuer.contains("/realms/")) {
          // Extract realm from issuer URL: http://localhost:8081/realms/core-platform
          String realm = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
          if (realm != null && !realm.trim().isEmpty()) {
            return realm.trim();
          }
        }

        // Fallback: try 'realm' claim if exists
        String realmClaim = jwt.getClaimAsString("realm");
        if (realmClaim != null && !realmClaim.trim().isEmpty()) {
          return realmClaim.trim();
        }
      }
    } catch (Exception e) {
      log.debug("Failed to extract tenant from JWT realm: {}", e.getMessage());
    }
    return null;
  }

  /**
   * 🔍 UTILITY: Get current JWT info for debugging
   */
  public String getRequestDebugInfo() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        return String.format("JWT Issuer: %s, Subject: %s", jwt.getIssuer(), jwt.getSubject());
      }
    } catch (Exception e) {
      return "JWT debug info unavailable: " + e.getMessage();
    }
    return "No JWT authentication";
  }
}
