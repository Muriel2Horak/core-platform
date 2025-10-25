package cz.muriel.core.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tenant -> Grafana org mapping
 * 
 * ⚠️ DEPRECATED: Grafana FE integration is being removed in favor of native Loki UI
 * This registry is disabled by default via monitoring.grafana.enabled=false
 * 
 * In production, this would call Grafana API or database For now, static
 * mapping with admin=1
 */
@Component 
@Slf4j
@ConditionalOnProperty(name = "monitoring.grafana.enabled", havingValue = "true", matchIfMissing = false)
public class GrafanaTenantRegistry {

  private final Map<String, Integer> tenantToOrgId = new ConcurrentHashMap<>();

  public GrafanaTenantRegistry() {
    // Initialize default mappings
    // NOTE: Org 1 = Main Org (Grafana default), Org 2 = "Tenant: admin" (created by
    // entrypoint.sh)
    tenantToOrgId.put("admin", 2); // ✅ FIXED: admin tenant uses Org 2
    tenantToOrgId.put("test-tenant", 3);
    tenantToOrgId.put("company-b", 4);
  }

  /**
   * Get Grafana org ID for tenant Creates new org if not exists (via
   * provisioning)
   */
  public int getGrafanaOrgId(String tenantId) {
    return tenantToOrgId.computeIfAbsent(tenantId, tid -> {
      log.warn("Tenant {} not found in registry, using default org 1. Run provisioning!", tid);
      return 1; // Fallback to Main org
    });
  }

  /**
   * Register new tenant org mapping
   */
  public void registerTenant(String tenantId, int orgId) {
    tenantToOrgId.put(tenantId, orgId);
    log.info("Registered tenant {} -> Grafana org {}", tenantId, orgId);
  }
}
