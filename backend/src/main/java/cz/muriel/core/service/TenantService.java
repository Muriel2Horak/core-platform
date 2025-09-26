package cz.muriel.core.service;

import cz.muriel.core.entity.Tenant;
import cz.muriel.core.repository.TenantRepository;
import cz.muriel.core.tenant.TenantIdCache;
import cz.muriel.core.tenant.TenantContext;
import cz.muriel.core.tenant.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class TenantService {

  private final TenantRepository tenantRepository;
  private final TenantIdCache tenantIdCache;

  /**
   * Get current tenant ID from context
   */
  public UUID getCurrentTenantIdOrThrow() {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      throw new IllegalStateException("No tenant context available");
    }

    // Check cache first
    UUID tenantId = tenantIdCache.getTenantId(tenantKey);
    if (tenantId != null) {
      return tenantId;
    }

    // Load from database
    Optional<Tenant> tenant = tenantRepository.findByKey(tenantKey);
    if (tenant.isEmpty()) {
      throw new TenantNotFoundException("Tenant not found: " + tenantKey);
    }

    tenantId = tenant.get().getId();
    tenantIdCache.putTenantId(tenantKey, tenantId);

    return tenantId;
  }

  /**
   * Find tenant by key
   */
  public Optional<Tenant> findByKey(String key) {
    return tenantRepository.findByKey(key);
  }

  /**
   * Get current tenant entity
   */
  public Optional<Tenant> getCurrentTenant() {
    String tenantKey = TenantContext.getTenantKey();
    if (tenantKey == null) {
      return Optional.empty();
    }
    return tenantRepository.findByKey(tenantKey);
  }

  /**
   * Check if tenant exists
   */
  public boolean existsByKey(String key) {
    return tenantRepository.existsByKey(key);
  }
}
