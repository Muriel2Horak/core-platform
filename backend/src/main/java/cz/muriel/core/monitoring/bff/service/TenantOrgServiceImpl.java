package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 🔄 REFACTORED: Dynamic tenant-org resolution
 * 
 * Místo static init() nyní používá database storage s Grafana provisioning
 */
@Service @Slf4j @RequiredArgsConstructor
public class TenantOrgServiceImpl implements TenantOrgService {

  private final GrafanaTenantBindingRepository bindingRepository;

  @Override @Cacheable(value = "tenantOrgBindings", key = "#tenantId")
  public TenantBinding resolve(Jwt jwt) {
    String tenantId = extractTenantId(jwt);
    return resolveTenantBinding(tenantId);
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

    // 3. realm_access.groups (extract tenant from group name)
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
    if (realmAccess != null) {
      Object rolesObj = realmAccess.get("roles");
      if (rolesObj instanceof List) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) rolesObj;
        for (String role : roles) {
          if (role.startsWith("TENANT_")) {
            return role.substring(7).toLowerCase(); // TENANT_CORE_PLATFORM -> core-platform
          }
        }
      }
    }

    // 4. Fallback to preferred_username realm
    String preferredUsername = jwt.getClaimAsString("preferred_username");
    if (preferredUsername != null && preferredUsername.contains("@")) {
      String realm = preferredUsername.split("@")[1];
      log.warn("⚠️ Falling back to realm {} from preferred_username for tenant resolution", realm);
      return realm;
    }

    log.error("❌ Could not extract tenant_id from JWT. Claims: {}", jwt.getClaims().keySet());
    throw new IllegalArgumentException("No tenant_id found in JWT token");
  }
}
