package cz.muriel.core.controller;

import cz.muriel.core.service.TenantDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/tenant-discovery") @RequiredArgsConstructor @Slf4j
public class TenantDiscoveryController {

  private final TenantDiscoveryService tenantDiscoveryService;

  /**
   * 🔍 TENANT DISCOVERY: Najde tenant(y) podle emailu/username Veřejný endpoint -
   * nepotřebuje autentifikaci
   */
  @PostMapping("/find-tenant")
  public ResponseEntity<Map<String, Object>> findTenantByUser(
      @RequestBody FindTenantRequest request) {
    log.info("🔍 Tenant discovery request for: {}", request.getIdentifier());

    try {
      // Validate input
      if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Email or username is required"));
      }

      String identifier = request.getIdentifier().trim().toLowerCase();

      // Find tenant(s) for this user
      List<TenantDiscoveryService.TenantInfo> tenants = tenantDiscoveryService
          .findTenantsForUser(identifier);

      if (tenants.isEmpty()) {
        log.info("🔍 No tenant found for user: {}", identifier);
        return ResponseEntity.ok(Map.of("found", false, "message", "No tenant found for this user",
            "identifier", identifier));
      }

      if (tenants.size() == 1) {
        // Single tenant - auto-redirect
        TenantDiscoveryService.TenantInfo tenant = tenants.get(0);
        log.info("🎯 Single tenant found for {}: {}", identifier, tenant.getTenantKey());

        return ResponseEntity.ok(Map.of("found", true, "autoRedirect", true, "tenantKey",
            tenant.getTenantKey(), "tenantName", tenant.getTenantName(), "redirectUrl",
            "https://" + tenant.getTenantKey() + ".core-platform.local/", "identifier",
            identifier));
      } else {
        // Multiple tenants - user choice needed
        log.info("🔀 Multiple tenants found for {}: {}", identifier,
            tenants.stream().map(TenantDiscoveryService.TenantInfo::getTenantKey).toList());

        return ResponseEntity
            .ok(Map.of("found", true, "autoRedirect", false, "multiple", true, "tenants",
                tenants.stream()
                    .map(tenant -> Map.of("tenantKey", tenant.getTenantKey(), "tenantName",
                        tenant.getTenantName(), "url",
                        "https://" + tenant.getTenantKey() + ".core-platform.local/"))
                    .toList(),
                "identifier", identifier));
      }

    } catch (Exception e) {
      log.error("❌ Tenant discovery failed for: {}", request.getIdentifier(), e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Tenant discovery failed: " + e.getMessage()));
    }
  }

  /**
   * 📋 TENANT DIRECTORY: Veřejný seznam dostupných tenantů (pro fallback)
   */
  @GetMapping("/available-tenants")
  public ResponseEntity<Map<String, Object>> getAvailableTenants() {
    log.info("📋 Requesting available tenants list");

    try {
      List<TenantDiscoveryService.TenantInfo> availableTenants = tenantDiscoveryService
          .getAvailableTenants();

      return ResponseEntity.ok(Map.of("tenants",
          availableTenants.stream()
              .map(tenant -> Map.of("tenantKey", tenant.getTenantKey(), "tenantName",
                  tenant.getTenantName(), "url",
                  "https://" + tenant.getTenantKey() + ".core-platform.local/"))
              .toList(),
          "count", availableTenants.size()));

    } catch (Exception e) {
      log.error("❌ Failed to get available tenants", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Failed to get tenants: " + e.getMessage()));
    }
  }

  // Request DTO
  public static class FindTenantRequest {
    private String identifier; // email nebo username

    public String getIdentifier() {
      return identifier;
    }

    public void setIdentifier(String identifier) {
      this.identifier = identifier;
    }
  }
}
