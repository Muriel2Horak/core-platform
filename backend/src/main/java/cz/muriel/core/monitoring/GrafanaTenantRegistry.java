package cz.muriel.core.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tenant -> Grafana org mapping
 * 
 * In production, this would call Grafana API or database For now, static
 * mapping with admin=1
 */
@Component @Slf4j
public class GrafanaTenantRegistry {

  private final Map<String, Integer> tenantToOrgId = new ConcurrentHashMap<>();

  public GrafanaTenantRegistry() {
    // Initialize default mappings
    tenantToOrgId.put("admin", 1);
    tenantToOrgId.put("test-tenant", 2);
    tenantToOrgId.put("company-b", 3);
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
