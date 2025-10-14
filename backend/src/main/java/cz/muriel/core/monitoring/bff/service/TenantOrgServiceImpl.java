package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * 🔄 REFACTORED: Dynamic tenant-org resolution
 * 
 * Místo static init() nyní používá database storage s Grafana provisioning
 */
@Service @Slf4j @RequiredArgsConstructor
public class TenantOrgServiceImpl implements TenantOrgService {

  private final GrafanaTenantBindingRepository bindingRepository;

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
   * Načte binding z databáze (s cache podporou)
   */
  private TenantBinding resolveTenantBinding(String tenantId) {
    log.debug("🔍 Resolving tenant binding for: {}", tenantId);

    GrafanaTenantBinding binding = bindingRepository.findByTenantId(tenantId).orElseThrow(() -> {
      log.error("❌ No Grafana org mapping found for tenant: {}", tenantId);
      return new IllegalStateException("Grafana organization not configured for tenant: " + tenantId
          + ". Please ensure the tenant is properly provisioned.");
    });

    log.debug("✅ Resolved tenant {} to org {} (token masked)", tenantId, binding.getGrafanaOrgId());

    return new TenantBinding(binding.getTenantId(), binding.getGrafanaOrgId(),
        binding.getServiceAccountToken());
  }

  @Override
  public String extractTenantId(Jwt jwt) {
    return extractTenantIdStatic(jwt);
  }
}
