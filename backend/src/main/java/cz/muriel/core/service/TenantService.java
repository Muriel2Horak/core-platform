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
   * 🎯 DEPRECATED: Use getTenantDisplayName() instead
   * 
   * @deprecated This method is kept for backward compatibility only
   */
  @Deprecated(forRemoval = true)
  public String getTenantNameByKey(String tenantKey) {
    log.warn("DEPRECATED: getTenantNameByKey() called - use getTenantDisplayName() instead");
    return getTenantDisplayName(tenantKey);
  }

  /**
   * 🎯 CLEAN ARCHITECTURE: Find tenant by key Used by webhook processing -
   * tenants must exist in DB as registry
   */
  public Optional<Tenant> findTenantByKey(String tenantKey) {
    return tenantRepository.findByKey(tenantKey);
  }

  /**
   * ⚠️ ADMIN ONLY: Create new tenant registry entry 🎯 OPTIMIZED: Only stores
   * minimal data - display names from Keycloak
   */
  public Tenant createTenantRegistry(String tenantKey) {
    log.info("🆕 Creating minimal tenant registry entry: key={}", tenantKey);

    Tenant newTenant = Tenant.builder().key(tenantKey).build();

    return tenantRepository.save(newTenant);
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

  /**
   * 🆕 OPTIMIZED: Helper methods for backward compatibility with controllers
   */
  public String getTenantNameById(UUID tenantId) {
    return tenantRepository.findById(tenantId).map(tenant -> getTenantDisplayName(tenant.getKey()))
        .orElse("Unknown Tenant");
  }

  public String getTenantKeyById(UUID tenantId) {
    return tenantRepository.findById(tenantId).map(Tenant::getKey).orElse(null);
  }
}
