package cz.muriel.core.tenant;

import cz.muriel.core.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component @RequiredArgsConstructor @Slf4j
public class TenantResolver {

  private final TenantRepository tenantRepository;
  private final TenantIdCache tenantIdCache;

  @Value("${auth.jwt.tenant-claim:tenant}")
  private String tenantClaimName;

  @Value("${tenancy.default-tenant-key:test-tenant}")
  private String defaultTenantKey;

  /**
   * Resolve tenant key from JWT token or use default
   */
  public String resolveTenantKey() {
    String tenantKey = getTenantFromJwt();

    if (tenantKey == null || tenantKey.trim().isEmpty()) {
      log.debug("No tenant claim found in JWT, using default: {}", defaultTenantKey);
      tenantKey = defaultTenantKey;
    }

    return tenantKey;
  }

  /**
   * Resolve and validate tenant, returns tenant ID
   */
  public UUID resolveTenantId(String tenantKey) {
    // Check cache first
    UUID tenantId = tenantIdCache.getTenantId(tenantKey);
    if (tenantId != null) {
      return tenantId;
    }

    // Load from database
    var tenant = tenantRepository.findByKey(tenantKey);
    if (tenant.isEmpty()) {
      log.error("Tenant not found: {}", tenantKey);
      throw new TenantNotFoundException("Tenant not found: " + tenantKey);
    }

    tenantId = tenant.get().getId();
    tenantIdCache.putTenantId(tenantKey, tenantId);

    return tenantId;
  }

  private String getTenantFromJwt() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
        return jwt.getClaimAsString(tenantClaimName);
      }
    } catch (Exception e) {
      log.warn("Failed to extract tenant from JWT: {}", e.getMessage());
    }
    return null;
  }
}
