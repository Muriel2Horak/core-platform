package cz.muriel.core.monitoring.grafana;

import cz.muriel.core.monitoring.grafana.dto.*;
import cz.muriel.core.monitoring.grafana.entity.GrafanaTenantBinding;
import cz.muriel.core.monitoring.grafana.repository.GrafanaTenantBindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 🚀 GRAFANA PROVISIONING SERVICE
 * 
 * ⚠️ DEPRECATED: Grafana FE integration is being removed in favor of native Loki UI
 * This service is disabled by default via monitoring.grafana.enabled=false
 * 
 * Orchestruje automatické zakládání Grafana organizací, service accounts a
 * tokenů při vytváření nových tenantů
 */
@Slf4j 
@Service 
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.grafana.enabled", havingValue = "true", matchIfMissing = false)
public class GrafanaProvisioningService {

  private final GrafanaAdminClient grafanaAdminClient;
  private final GrafanaTenantBindingRepository bindingRepository;

  @Value("${grafana.provisioning.enabled:true}")
  private boolean provisioningEnabled;

  @Value("${grafana.provisioning.service-account-role:Admin}")
  private String defaultServiceAccountRole;

  /**
   * 🏗️ PROVISION TENANT
   * 
   * Kompletní provisioning: 1. Vytvoří Grafana organizaci 2. Vytvoří service
   * account v organizaci 3. Vygeneruje service account token 4. Uloží binding do
   * databáze
   */
  @Transactional
  public GrafanaTenantBinding provisionTenant(String tenantId) {
    if (!provisioningEnabled) {
      log.warn("⚠️ Grafana provisioning is disabled - skipping tenant: {}", tenantId);
      return null;
    }

    log.info("🚀 Starting Grafana provisioning for tenant: {}", tenantId);

    // Check if already provisioned
    if (bindingRepository.existsByTenantId(tenantId)) {
      log.warn("⚠️ Grafana binding already exists for tenant: {}", tenantId);
      return bindingRepository.findByTenantId(tenantId).orElseThrow();
    }

    try {
      // Step 1: Create or find existing Grafana organization
      String orgName = generateOrgName(tenantId);
      Long orgId;

      try {
        CreateOrgResponse orgResponse = grafanaAdminClient.createOrganization(orgName);
        orgId = orgResponse.getOrgId();
        log.info("✅ Created new Grafana organization: {} (orgId: {})", orgName, orgId);
      } catch (GrafanaApiException e) {
        // If organization already exists (409 Conflict), find it
        if (e.getMessage().contains("409") || e.getMessage().contains("Organization name taken")) {
          log.info("ℹ️ Organization already exists, finding existing: {}", orgName);
          var existingOrg = grafanaAdminClient.findOrgByName(orgName)
              .orElseThrow(() -> new GrafanaProvisioningException(
                  "Organization exists but cannot be found: " + orgName));
          orgId = existingOrg.getId();
          log.info("✅ Found existing Grafana organization: {} (orgId: {})", orgName, orgId);
        } else {
          throw e; // Re-throw if it's a different error
        }
      }

      // Step 2: Create or find existing service account
      String saName = generateServiceAccountName(tenantId);
      Long serviceAccountId;

      try {
        CreateServiceAccountResponse saResponse = grafanaAdminClient.createServiceAccount(orgId,
            saName, defaultServiceAccountRole);
        serviceAccountId = saResponse.getId();
        log.info("✅ Created new service account: {} (id: {})", saName, serviceAccountId);
      } catch (GrafanaApiException e) {
        // If service account already exists, find it
        if (e.getMessage().contains("409") || e.getMessage().contains("already exists")) {
          log.info("ℹ️ Service account already exists, finding existing: {}", saName);
          var existingSA = grafanaAdminClient.listServiceAccounts(orgId).stream()
              .filter(sa -> sa.getName().equals(saName)).findFirst()
              .orElseThrow(() -> new GrafanaProvisioningException(
                  "Service account exists but cannot be found: " + saName));
          serviceAccountId = existingSA.getId();
          log.info("✅ Found existing service account: {} (id: {})", saName, serviceAccountId);
        } else {
          throw e; // Re-throw if it's a different error
        }
      }

      // Step 3: Create service account token
      String tokenName = generateTokenName(tenantId);
      CreateServiceAccountTokenResponse tokenResponse = grafanaAdminClient
          .createServiceAccountToken(orgId, serviceAccountId, tokenName);
      String token = tokenResponse.getKey();

      // Step 4: Create Prometheus datasource
      String datasourceName = "Prometheus";
      try {
        CreateDataSourceResponse dsResponse = grafanaAdminClient.createPrometheusDataSource(orgId,
            datasourceName);
        log.info("✅ Created Prometheus datasource: {} (id: {}, uid: {})", datasourceName,
            dsResponse.getId(), dsResponse.getUid());
      } catch (GrafanaApiException e) {
        log.warn("⚠️ Failed to create Prometheus datasource (may already exist): {}",
            e.getMessage());
        // Continue - datasource creation is not critical for binding
      }

      // Step 5: Save binding to database
      GrafanaTenantBinding binding = GrafanaTenantBinding.builder().tenantId(tenantId)
          .grafanaOrgId(orgId).serviceAccountId(serviceAccountId).serviceAccountName(saName)
          .serviceAccountToken(token).build();

      GrafanaTenantBinding saved = bindingRepository.saveAndFlush(binding);

      log.info(
          "✅ Grafana provisioning completed for tenant: {} (orgId: {}, saId: {}, token: {}***)",
          tenantId, orgId, serviceAccountId, token.substring(0, Math.min(10, token.length())));

      return saved;

    } catch (Exception e) {
      log.error("❌ Grafana provisioning failed for tenant: {}", tenantId, e);
      throw new GrafanaProvisioningException("Failed to provision Grafana for tenant: " + tenantId,
          e);
    }
  }

  /**
   * 🗑️ DEPROVISION TENANT
   * 
   * Smaže Grafana organizaci a binding z databáze
   */
  @Transactional
  public void deprovisionTenant(String tenantId) {
    if (!provisioningEnabled) {
      log.warn("⚠️ Grafana provisioning is disabled - skipping deprovision for tenant: {}",
          tenantId);
      return;
    }

    log.info("🗑️ Starting Grafana deprovisioning for tenant: {}", tenantId);

    try {
      // Find binding
      GrafanaTenantBinding binding = bindingRepository.findByTenantId(tenantId)
          .orElseThrow(() -> new GrafanaProvisioningException(
              "Grafana binding not found for tenant: " + tenantId));

      // Delete Grafana organization (cascades to service accounts and tokens)
      grafanaAdminClient.deleteOrganization(binding.getGrafanaOrgId());

      // Delete binding from database
      bindingRepository.deleteByTenantId(tenantId);

      log.info("✅ Grafana deprovisioning completed for tenant: {} (orgId: {})", tenantId,
          binding.getGrafanaOrgId());

    } catch (Exception e) {
      log.error("❌ Grafana deprovisioning failed for tenant: {}", tenantId, e);
      throw new GrafanaProvisioningException(
          "Failed to deprovision Grafana for tenant: " + tenantId, e);
    }
  }

  /**
   * 🔍 GET TENANT BINDING
   * 
   * Vrátí binding pro daný tenant (pokud existuje)
   */
  public GrafanaTenantBinding getTenantBinding(String tenantId) {
    return bindingRepository.findByTenantId(tenantId).orElse(null);
  }

  /**
   * ✅ IS TENANT PROVISIONED
   * 
   * Zkontroluje, jestli je tenant provisionovaný
   */
  public boolean isTenantProvisioned(String tenantId) {
    return bindingRepository.existsByTenantId(tenantId);
  }

  // ==================== HELPER METHODS ====================

  private String generateOrgName(String tenantId) {
    return "Tenant: " + tenantId;
  }

  private String generateServiceAccountName(String tenantId) {
    return "sa-" + tenantId;
  }

  private String generateTokenName(String tenantId) {
    return "token-" + tenantId;
  }
}
