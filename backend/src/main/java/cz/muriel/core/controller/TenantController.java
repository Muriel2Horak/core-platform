package cz.muriel.core.controller;

import cz.muriel.core.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/tenants") @RequiredArgsConstructor @Slf4j
public class TenantController {

  private final TenantService tenantService;

  @GetMapping("/me") @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER', 'CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN')")
  public ResponseEntity<Map<String, Object>> getCurrentTenant() {
    var tenant = tenantService.getCurrentTenant();

    if (tenant.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // 🎯 OPTIMIZED: Lazy-load display name from Keycloak only when needed
    var response = Map.<String, Object>of("key", tenant.get().getKey(), "name",
        tenantService.getTenantDisplayName(tenant.get().getKey()), // Lazy-loaded
        "realm", tenant.get().getKey() // realm = key
    );

    log.debug("Returning current tenant info: {}", response);
    return ResponseEntity.ok(response);
  }

  /**
   * 🆕 GET /api/tenants - Seznam všech tenantů (pouze pro core-admin)
   */
  @GetMapping @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<List<Map<String, Object>>> getAllTenants() {
    log.debug("Core admin requesting all tenants");

    var tenants = tenantService.getAllTenants();

    // 🎯 OPTIMIZED: Lazy-load display names from Keycloak only when needed
    var response = tenants.stream()
        .map(tenant -> Map.<String, Object>of("id", tenant.getId(), "key", tenant.getKey(), "name",
            tenantService.getTenantDisplayName(tenant.getKey()), // Lazy-loaded
            "realm", tenant.getKey() // realm = key
        )).toList();

    log.debug("Returning {} tenants for core admin", response.size());
    return ResponseEntity.ok(response);
  }
}
