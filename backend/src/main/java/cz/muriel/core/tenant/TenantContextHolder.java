package cz.muriel.core.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Helper for extracting tenant ID from the current security context.
 */
public final class TenantContextHolder {

  private TenantContextHolder() {}

  public static Optional<UUID> getTenantId() {
    return getTenantId(SecurityContextHolder.getContext().getAuthentication());
  }

  public static Optional<UUID> getTenantId(Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) {
      return Optional.empty();
    }

    if (auth.getPrincipal() instanceof Jwt jwt) {
      String tenantIdStr = jwt.getClaimAsString("tenant_id");
      if (tenantIdStr == null || tenantIdStr.isBlank()) {
        return Optional.empty();
      }
      try {
        return Optional.of(UUID.fromString(tenantIdStr));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid tenant_id claim format", e);
      }
    }

    return Optional.empty();
  }
}
