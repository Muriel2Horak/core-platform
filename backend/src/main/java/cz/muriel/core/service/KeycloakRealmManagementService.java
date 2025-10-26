package cz.muriel.core.service;

import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.monitoring.grafana.GrafanaProvisioningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * 🏢 KEYCLOAK REALM MANAGEMENT SERVICE
 * 
 * Služba pro správu Keycloak realms s podporou template processing
 * 
 * ⚠️ GrafanaProvisioningService je OPTIONAL - funguje i když monitoring.grafana.enabled=false
 */
@Slf4j @Service @Transactional
public class KeycloakRealmManagementService {

  private final KeycloakAdminService keycloakAdminService;
  private final TenantService tenantService;
  private final ResourceLoader resourceLoader;
  private final Optional<GrafanaProvisioningService> grafanaProvisioningService;

  public KeycloakRealmManagementService(
      KeycloakAdminService keycloakAdminService,
      TenantService tenantService,
      ResourceLoader resourceLoader,
      @Autowired(required = false) GrafanaProvisioningService grafanaProvisioningService) {
    this.keycloakAdminService = keycloakAdminService;
    this.tenantService = tenantService;
    this.resourceLoader = resourceLoader;
    this.grafanaProvisioningService = Optional.ofNullable(grafanaProvisioningService);
  }

  @Value("${DOMAIN:core-platform.local}")
  private String domain;

  @Value("${KEYCLOAK_ADMIN_CLIENT_SECRET:your-secure-secret}")
  private String keycloakAdminClientSecret;

  @Value("${keycloak.realm.template-path:keycloak/realm-tenant-template.json}")
  private String realmTemplatePath;

  /**
   * 🆕 CREATE TENANT: Vytvoří nový tenant s kompletní Keycloak realm konfigurací
   * + automatický Grafana provisioning
   */
  public void createTenant(String tenantKey, String displayName) {
    log.info("🏗️ Creating tenant: {} with display name: {}", tenantKey, displayName);

    try {
      // 1. Load tenant template
      String tenantTemplate = loadTenantTemplate();

      // 2. Process template variables
      String processedTemplate = processTemplate(tenantTemplate, tenantKey, displayName);

      // 3. Create Keycloak realm
      keycloakAdminService.createRealm(processedTemplate);
      log.info("✅ Keycloak realm created: {}", tenantKey);

      // 4. Wait a bit for Keycloak to finalize realm creation
      Thread.sleep(500);

      // 5. Get realm ID from Keycloak
      String keycloakRealmId = null;
      try {
        Map<String, Object> realmInfo = keycloakAdminService.getRealmInfo(tenantKey);
        keycloakRealmId = (String) realmInfo.get("id");
        log.info("✅ Retrieved Keycloak realm_id: {}", keycloakRealmId);
      } catch (Exception e) {
        log.warn("⚠️ Could not retrieve realm_id immediately after creation: {}", e.getMessage());
      }

      // 6. Register tenant in database with Keycloak realm ID
      tenantService.createTenantRegistryWithRealmId(tenantKey, keycloakRealmId);

      // 7. 🚀 AUTOMATIC GRAFANA PROVISIONING (only if Grafana enabled)
      grafanaProvisioningService.ifPresentOrElse(
          service -> {
            try {
              service.provisionTenant(tenantKey);
              log.info("✅ Grafana provisioning completed for tenant: {}", tenantKey);
            } catch (Exception e) {
              log.error(
                  "⚠️ Grafana provisioning failed for tenant: {} (tenant created but monitoring unavailable)",
                  tenantKey, e);
              // Don't fail entire tenant creation if Grafana provisioning fails
            }
          },
          () -> log.info("⏭️ Grafana provisioning skipped (monitoring.grafana.enabled=false)")
      );

      log.info("✅ Tenant created successfully: {} (realm_id: {})", tenantKey, keycloakRealmId);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("❌ Thread interrupted while creating tenant: {}", tenantKey, e);
      throw new RuntimeException("Thread interrupted: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("❌ Failed to create tenant: {}", tenantKey, e);
      throw new RuntimeException("Failed to create tenant: " + e.getMessage(), e);
    }
  }

  /**
   * 🗑️ DELETE TENANT: Smaže tenant včetně Keycloak realm a Grafana organizace
   */
  public void deleteTenant(String tenantKey) {
    log.warn("🗑️ Deleting tenant: {}", tenantKey);

    try {
      // 1. 🗑️ AUTOMATIC GRAFANA DEPROVISIONING (only if Grafana enabled)
      grafanaProvisioningService.ifPresentOrElse(
          service -> {
            try {
              service.deprovisionTenant(tenantKey);
              log.info("✅ Grafana deprovisioning completed for tenant: {}", tenantKey);
            } catch (Exception e) {
              log.error(
                  "⚠️ Grafana deprovisioning failed for tenant: {} (continuing with tenant deletion)",
                  tenantKey, e);
              // Don't fail entire tenant deletion if Grafana deprovisioning fails
            }
          },
          () -> log.info("⏭️ Grafana deprovisioning skipped (monitoring.grafana.enabled=false)")
      );

      // 2. Delete Keycloak realm
      keycloakAdminService.deleteRealm(tenantKey);

      // 3. Delete from database registry
      tenantService.deleteTenantFromRegistry(tenantKey);

      log.info("✅ Tenant deleted successfully: {}", tenantKey);

    } catch (Exception e) {
      log.error("❌ Failed to delete tenant: {}", tenantKey, e);
      throw new RuntimeException("Failed to delete tenant: " + e.getMessage(), e);
    }
  }

  /**
   * ✏️ UPDATE TENANT DISPLAY NAME: Aktualizuje display name tenantu v Keycloak
   */
  public void updateTenantDisplayName(String tenantKey, String displayName) {
    log.info("✏️ Updating display name for tenant: {} to: {}", tenantKey, displayName);

    try {
      keycloakAdminService.updateRealmDisplayName(tenantKey, displayName);
      log.info("✅ Tenant display name updated successfully: {}", tenantKey);

    } catch (Exception e) {
      log.error("❌ Failed to update tenant display name: {}", tenantKey, e);
      throw new RuntimeException("Failed to update tenant display name: " + e.getMessage(), e);
    }
  }

  /**
   * 📊 GET TENANT STATUS: Získá status tenantu z Keycloak
   */
  public Map<String, Object> getTenantStatus(String tenantKey) {
    try {
      boolean realmExists = keycloakAdminService.realmExists(tenantKey);

      if (realmExists) {
        Map<String, Object> realmInfo = keycloakAdminService.getRealmInfo(tenantKey);
        return Map.of("realmExists", true, "realmEnabled", realmInfo.getOrDefault("enabled", false),
            "realmInfo", realmInfo);
      } else {
        return Map.of("realmExists", false, "realmEnabled", false);
      }

    } catch (Exception e) {
      log.error("❌ Failed to get tenant status: {}", tenantKey, e);
      return Map.of("realmExists", false, "realmEnabled", false, "error", e.getMessage());
    }
  }

  /**
   * 📄 LOAD TENANT TEMPLATE: Unified template loading - classpath first, Docker
   * fallback
   */
  private String loadTenantTemplate() throws IOException {
    try {
      // Primary: Load from classpath (works in JAR, Docker, everywhere)
      Resource resource = resourceLoader.getResource("classpath:" + realmTemplatePath);

      if (resource.exists()) {
        log.debug("📄 Loading tenant template from classpath: {}", realmTemplatePath);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }

      // Fallback 1: Docker directory (for development)
      resource = resourceLoader.getResource("file:docker/keycloak/realm-tenant-template.json");

      if (resource.exists()) {
        log.debug("📄 Loading tenant template from Docker directory (fallback)");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }

      // Fallback 2: Relative Docker path (for different working directories)
      resource = resourceLoader.getResource("file:../docker/keycloak/realm-tenant-template.json");

      if (resource.exists()) {
        log.debug("📄 Loading tenant template from relative Docker directory (fallback)");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      }

      throw new IOException("Tenant template not found in classpath or docker directories");

    } catch (Exception e) {
      log.error("❌ Failed to load tenant template", e);
      throw new IOException("Failed to load tenant template: " + e.getMessage(), e);
    }
  }

  /**
   * 🔄 PROCESS TEMPLATE: Unified processing pro ${VAR} formát (kompatibilní s
   * envsubst)
   */
  private String processTemplate(String template, String tenantKey, String displayName) {
    log.debug("🔄 Processing template for tenant: {} with domain: {}", tenantKey, domain);

    // Generate secure tenant admin password
    String tenantAdminPassword = generateSecureTenantPassword();

    // Replace template variables - unified ${VAR} format
    String processed = template.replace("${TENANT_KEY}", tenantKey)
        .replace("${TENANT_DISPLAY_NAME}", displayName != null ? displayName : tenantKey)
        .replace("${DOMAIN}", domain)
        .replace("${KEYCLOAK_ADMIN_CLIENT_SECRET}", keycloakAdminClientSecret)
        .replace("${TENANT_ADMIN_PASSWORD}", tenantAdminPassword)

        // Legacy support for {VAR} format (backwards compatibility)
        .replace("{TENANT_KEY}", tenantKey)
        .replace("{TENANT_DISPLAY_NAME}", displayName != null ? displayName : tenantKey)
        .replace("{DOMAIN}", domain);

    log.debug("✅ Template processed successfully for tenant: {}", tenantKey);
    return processed;
  }

  /**
   * 🔐 GENERATE SECURE TENANT PASSWORD: Vytvoří bezpečné heslo pro tenant admina
   */
  private String generateSecureTenantPassword() {
    // Generate cryptographically secure password
    // Format: TenantAdmin{TIMESTAMP}{RANDOM} - ensures uniqueness and security
    long timestamp = System.currentTimeMillis() % 10000; // Last 4 digits
    int random = (int) (Math.random() * 9000) + 1000; // 4-digit random

    return String.format("TenantAdmin%d%d!", timestamp, random);
  }
}
