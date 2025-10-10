package cz.muriel.core.monitoring.bff.service;

import cz.muriel.core.monitoring.bff.model.TenantBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of TenantOrgService. Loads tenant-to-org mapping from
 * environment/config. In production, this should load from Vault/Secrets
 * Manager.
 */
@Service @Slf4j
public class TenantOrgServiceImpl implements TenantOrgService {

  @Value("${monitoring.tenant-org-map:{}}")
  private String tenantOrgMapJson;

  private final Map<String, TenantBinding> tenantOrgMap = new HashMap<>();

  @PostConstruct
  public void init() {
    // ✅ PRODUCTION: Service account tokens loaded from environment variables
    // These should be injected via Kubernetes Secrets, Vault, or AWS Secrets Manager
    // Format: GRAFANA_SAT_<TENANT_ID_UPPERCASE> (e.g., GRAFANA_SAT_CORE_PLATFORM)

    log.info("Initializing tenant-org mappings from environment");

    // Load tenant mappings from environment variables
    loadTenantMapping("core-platform", 1L, "GRAFANA_SAT_CORE_PLATFORM");
    loadTenantMapping("test-tenant", 2L, "GRAFANA_SAT_TEST_TENANT");

    log.info("Loaded {} tenant-org mappings", tenantOrgMap.size());
  }

  /**
   * Load tenant mapping from environment variable
   */
  private void loadTenantMapping(String tenantId, Long orgId, String envVarName) {
    String token = System.getenv(envVarName);
    
    if (token == null || token.isBlank()) {
      log.warn("⚠️ Missing service account token for tenant {}: {} not set", tenantId, envVarName);
      // In development, use placeholder (will fail on actual Grafana calls)
      token = "glsa_dev_placeholder_" + tenantId;
    }

    tenantOrgMap.put(tenantId, new TenantBinding(tenantId, orgId, token));
    log.debug("Loaded mapping: {} -> org {} (token masked)", tenantId, orgId);
  }

  @Override
  public TenantBinding resolve(Jwt jwt) {
    String tenantId = extractTenantId(jwt);

    TenantBinding binding = tenantOrgMap.get(tenantId);
    if (binding == null) {
      log.error("No Grafana org mapping found for tenant: {}", tenantId);
      throw new IllegalStateException(
          "Grafana organization not configured for tenant: " + tenantId);
    }

    log.debug("Resolved tenant {} to org {} (token masked)", tenantId, binding.orgId());
    return binding;
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
      log.warn("Falling back to realm {} from preferred_username for tenant resolution", realm);
      return realm;
    }

    log.error("Could not extract tenant_id from JWT. Claims: {}", jwt.getClaims().keySet());
    throw new IllegalArgumentException("No tenant_id found in JWT token");
  }
}
