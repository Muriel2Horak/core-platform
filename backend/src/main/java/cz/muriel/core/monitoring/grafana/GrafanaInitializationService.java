package cz.muriel.core.monitoring.grafana;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 🚀 GRAFANA INITIALIZATION SERVICE
 * 
 * Automaticky provisionuje Grafana organizace pro existující tenanty při startu
 * aplikace (např. admin tenant, který se vytváří v
 * KeycloakInitializationService)
 * 
 * NOTE: Disabled in test profile to avoid automatic provisioning during tests
 */
@Slf4j @Component @RequiredArgsConstructor @Profile("!test")
public class GrafanaInitializationService {

  private final GrafanaProvisioningService grafanaProvisioningService;
  private final TenantRepository tenantRepository;

  /**
   * 🔄 PROVISION EXISTING TENANTS
   * 
   * Spustí se po startu aplikace a provisionuje Grafana pro všechny tenanty,
   * kteří ještě nemají binding
   */
  @EventListener(ApplicationReadyEvent.class) @Order(100) // Po KeycloakInitializationService (má
                                                          // Order 10)
  public void provisionExistingTenants() {
    log.info("🔍 Checking for tenants needing Grafana provisioning...");

    try {
      List<Tenant> tenants = tenantRepository.findAll();
      log.info("Found {} tenants in database", tenants.size());

      int provisionedCount = 0;
      int skippedCount = 0;
      int failedCount = 0;

      for (Tenant tenant : tenants) {
        String tenantKey = tenant.getKey();

        // Check if already provisioned
        if (grafanaProvisioningService.isTenantProvisioned(tenantKey)) {
          log.debug("Tenant already provisioned: {}", tenantKey);
          skippedCount++;
          continue;
        }

        // Provision tenant
        try {
          grafanaProvisioningService.provisionTenant(tenantKey);
          log.info("✅ Grafana provisioned for existing tenant: {}", tenantKey);
          provisionedCount++;
        } catch (Exception e) {
          log.error("⚠️ Failed to provision Grafana for tenant: {}", tenantKey, e);
          failedCount++;
          // Continue with other tenants
        }
      }

      log.info(
          "🎯 Grafana provisioning summary: {} provisioned, {} skipped (already done), {} failed",
          provisionedCount, skippedCount, failedCount);

      if (failedCount > 0) {
        log.warn("⚠️ Some tenants failed to provision. Check logs and retry manually if needed.");
      }

    } catch (Exception e) {
      log.error("❌ Failed to check/provision existing tenants", e);
      // Don't fail application startup
    }
  }
}
