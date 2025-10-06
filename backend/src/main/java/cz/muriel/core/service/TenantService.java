package cz.muriel.core.service;

import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class TenantService {

  private final TenantRepository tenantRepository;
  private final KeycloakAdminService keycloakAdminService;

  /**
   * 🎯 CLEAN ARCHITECTURE: Get current tenant from JWT context Tenant key =
   * Keycloak realm name (simple and secure)
   */
  public Optional<Tenant> getCurrentTenant() {
    String tenantKey = TenantContext.getTenantKey();

    if (tenantKey == null) {
      log.debug("No tenant context found");
      return Optional.empty();
    }

    // Simple lookup by tenant key (which equals realm name)
    return tenantRepository.findByKey(tenantKey);
  }

  /**
   * 🎯 Get current tenant ID or throw exception
   */
  public UUID getCurrentTenantIdOrThrow() {
    Optional<Tenant> tenant = getCurrentTenant();
    if (tenant.isEmpty()) {
      throw new IllegalStateException(
          "No tenant context found - this should not happen in authenticated requests");
    }
    return tenant.get().getId();
  }

  /**
   * 🆕 Get all tenants (for core admins only)
   */
  public List<Tenant> getAllTenants() {
    return tenantRepository.findAll();
  }

  /**
   * 🎯 OPTIMIZED: Get tenant display name from Keycloak (lazy-loaded) Use
   * sparingly - for display purposes only
   */
  public String getTenantDisplayName(String tenantKey) {
    try {
      // Get realm information from Keycloak to get display name
      List<Map<String, Object>> realms = keycloakAdminService.getAllRealms();

      for (Map<String, Object> realm : realms) {
        if (tenantKey.equals(realm.get("realm"))) {
          String displayName = (String) realm.get("displayName");
          return displayName != null && !displayName.isEmpty() ? displayName : tenantKey;
        }
      }

      log.debug("Realm '{}' not found in Keycloak, using key as display name", tenantKey);
      return tenantKey;

    } catch (Exception e) {
      log.warn("Failed to get tenant display name from Keycloak for '{}': {}", tenantKey,
          e.getMessage());
      // Fallback to tenant key
      return tenantKey;
    }
  }

  /**
   * 🎯 CLEAN ARCHITECTURE: Find tenant by key Used by webhook processing -
   * tenants must exist in DB as registry
   */
  public Optional<Tenant> findTenantByKey(String tenantKey) {
    return tenantRepository.findByKey(tenantKey);
  }

  /**
   * 🔍 CDC SUPPORT: Find tenant by Keycloak realm_id 🆕 OPTIMIZED: Uses
   * keycloak_realm_id column for direct lookup
   */
  public Optional<Tenant> findTenantByRealmId(String realmId) {
    if (realmId == null || realmId.isEmpty()) {
      return Optional.empty();
    }

    try {
      // 🆕 PRIMARY: Direct lookup using keycloak_realm_id column
      Optional<Tenant> tenant = tenantRepository.findByKeycloakRealmId(realmId);

      if (tenant.isPresent()) {
        log.debug("✅ Found tenant by keycloak_realm_id: {} -> {}", realmId, tenant.get().getKey());
        return tenant;
      }

      // FALLBACK: Query Keycloak API (for tenants not yet synced)
      log.debug("🔍 Tenant not found by realm_id, querying Keycloak API: {}", realmId);
      List<Map<String, Object>> realms = keycloakAdminService.getAllRealms();

      for (Map<String, Object> realm : realms) {
        String id = (String) realm.get("id");
        if (realmId.equals(id)) {
          String realmName = (String) realm.get("realm");
          if (realmName != null) {
            log.debug("Mapped realm_id {} to tenant_key {} via Keycloak API", realmId, realmName);

            // Update tenant with keycloak_realm_id for future lookups
            Optional<Tenant> foundTenant = findTenantByKey(realmName);
            if (foundTenant.isPresent()) {
              Tenant t = foundTenant.get();
              if (t.getKeycloakRealmId() == null) {
                t.setKeycloakRealmId(realmId);
                tenantRepository.save(t);
                log.info("✅ Updated tenant {} with keycloak_realm_id: {}", realmName, realmId);
              }
              return foundTenant;
            }
          }
        }
      }

      log.debug("Could not find realm with id: {}", realmId);
      return Optional.empty();

    } catch (Exception e) {
      log.error("Failed to find tenant by realm_id {}: {}", realmId, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * ⚠️ ADMIN ONLY: Create new tenant registry entry 🎯 OPTIMIZED: Uses
   * deterministic UUID generation for consistent tenant IDs 🆕 ENHANCED:
   * Automatically fetches and stores keycloak_realm_id
   */
  public Tenant createTenantRegistry(String tenantKey) {
    log.info("🆕 Creating tenant registry entry with deterministic UUID: key={}", tenantKey);

    // Fetch realm info from Keycloak to get realm ID
    String keycloakRealmId = null;
    try {
      List<Map<String, Object>> realms = keycloakAdminService.getAllRealms();
      for (Map<String, Object> realm : realms) {
        if (tenantKey.equals(realm.get("realm"))) {
          keycloakRealmId = (String) realm.get("id");
          log.debug("✅ Found Keycloak realm_id for {}: {}", tenantKey, keycloakRealmId);
          break;
        }
      }
    } catch (Exception e) {
      log.warn("⚠️ Could not fetch Keycloak realm_id for {}: {}", tenantKey, e.getMessage());
    }

    // Create tenant with deterministic UUID and Keycloak realm ID
    Tenant newTenant = Tenant.builder().key(tenantKey).id(Tenant.generateUuidFromKey(tenantKey))
        .keycloakRealmId(keycloakRealmId).build();

    return tenantRepository.save(newTenant);
  }

  /**
   * 🆕 Create tenant registry with explicitly provided Keycloak realm ID Used
   * when creating tenant through KeycloakRealmManagementService
   */
  public Tenant createTenantRegistryWithRealmId(String tenantKey, String keycloakRealmId) {
    log.info("🆕 Creating tenant registry entry: key={}, realm_id={}", tenantKey, keycloakRealmId);

    // Create tenant with deterministic UUID and provided Keycloak realm ID
    Tenant newTenant = Tenant.builder().key(tenantKey).id(Tenant.generateUuidFromKey(tenantKey))
        .keycloakRealmId(keycloakRealmId).build();

    Tenant saved = tenantRepository.save(newTenant);
    log.info("✅ Tenant registry created: {} (id: {}, realm_id: {})", saved.getKey(), saved.getId(),
        saved.getKeycloakRealmId());

    return saved;
  }

  /**
   * 🆕 Update existing tenant (for updating keycloak_realm_id)
   */
  public Tenant updateTenant(Tenant tenant) {
    log.debug("Updating tenant: {}", tenant.getKey());
    return tenantRepository.save(tenant);
  }

  /**
   * 🗑️ ADMIN ONLY: Delete tenant from registry WARNING: This will cascade delete
   * all related data!
   */
  public void deleteTenantFromRegistry(String tenantKey) {
    log.warn("🗑️ Deleting tenant from registry: {}", tenantKey);

    Optional<Tenant> tenant = tenantRepository.findByKey(tenantKey);
    if (tenant.isPresent()) {
      tenantRepository.delete(tenant.get());
      log.info("✅ Tenant deleted from registry: {}", tenantKey);
    } else {
      log.warn("⚠️ Tenant not found in registry: {}", tenantKey);
    }
  }
}
