package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * 🔄 SIMPLIFIED: Tenant resolution without Grafana org mapping
 * 
 * Po odstranění Grafana integrace pouze extrahuje tenant ID z JWT
 */
@Service @Slf4j @RequiredArgsConstructor
public class TenantOrgServiceImpl implements TenantOrgService {

  @Override @Cacheable(value = "tenantOrgBindings", key = "T(cz.muriel.core.monitoring.bff.service.TenantOrgServiceImpl).extractTenantIdStatic(#jwt)")
  public TenantBinding resolve(Jwt jwt) {
    String tenantId = extractTenantId(jwt);
    return resolveTenantBinding(tenantId);
  }

  /**
   * Static method for cache key generation
   */
  public static String extractTenantIdStatic(Jwt jwt) {
    // Try multiple claim locations

    // 1. Direct tenant_id claim
    String tenantId = jwt.getClaimAsString("tenant_id");
    if (tenantId != null && !tenantId.isEmpty()) {
      return tenantId;
    }

    // 2. tenant claim
    tenantId = jwt.getClaimAsString("tenant");
    if (tenantId != null && !tenantId.isEmpty()) {
      return tenantId;
    }

    // 3. Extract from issuer (realm)
    String issuer = jwt.getClaimAsString("iss");
    if (issuer != null && issuer.contains("/realms/")) {
      String realm = issuer.substring(issuer.lastIndexOf("/realms/") + 8);
      if (!realm.isEmpty()) {
        return realm;
      }
    }

    throw new IllegalStateException("No tenant information in JWT");
  }

  /**
   * 🔍 RESOLVE TENANT BINDING
   * 
   * Vytvoří simple binding pouze s tenant ID (bez Grafana org)
   */
  private TenantBinding resolveTenantBinding(String tenantId) {
    log.debug("🔍 Resolving tenant binding for: {}", tenantId);

    // Simple binding without Grafana org (post-Grafana-integration cleanup)
    return new TenantBinding(tenantId, null, null);
  }

  @Override
  public String extractTenantId(Jwt jwt) {
    return extractTenantIdStatic(jwt);
  }
}
