package cz.muriel.core.controller;

import cz.muriel.core.dto.CreateTenantRequest;
import cz.muriel.core.entity.Tenant;
import cz.muriel.core.service.KeycloakRealmManagementService;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.service.UserDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🏢 TENANT MANAGEMENT API - Správa tenantů s hierarchií rolí
 */
@Slf4j @RestController @RequestMapping("/api/admin/tenants") @RequiredArgsConstructor @Validated @PreAuthorize("hasAnyAuthority('CORE_ROLE_TENANT_MANAGER', 'CORE_ROLE_SYSTEM_ADMIN', 'CORE_ROLE_ADMIN')")
public class TenantManagementController {

  private final KeycloakRealmManagementService keycloakRealmManagementService;
  private final TenantService tenantService;
  private final UserDirectoryService userDirectoryService;

  @Value("${DOMAIN:core-platform.local}")
  private String domain;

  /**
   * 🆕 POST /api/admin/tenants - Vytvoření nového tenantu
   * 
   * Kompletní workflow: 1. Vytvoří Keycloak realm s konfigurací 2. Založí tenant
   * admin uživatele 3. Zaregistruje tenant v DB
   */
  @PostMapping @PreAuthorize("hasAnyAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> createTenant(
      @Valid @RequestBody CreateTenantRequest request, @AuthenticationPrincipal Jwt jwt) {

    try {
      log.info("🏗️ Creating tenant: key={}, displayName={}", request.getKey(),
          request.getDisplayName());

      // 1. Create tenant using realm management service
      keycloakRealmManagementService.createTenant(request.getKey(), request.getDisplayName());

      // 2. Get created tenant info
      Optional<Tenant> tenant = tenantService.findTenantByKey(request.getKey());

      if (tenant.isEmpty()) {
        throw new RuntimeException("Tenant was created but not found in registry");
      }

      // 3. Return success response with tenant details
      Map<String, Object> response = Map.of("success", true, "message",
          "Tenant created successfully", "tenant",
          Map.of("id", tenant.get().getId(), "key", tenant.get().getKey(), "displayName",
              request.getDisplayName(), "realm", tenant.get().getKey(), "subdomain",
              request.getKey() + "." + domain));

      log.info("✅ Tenant created successfully: {}", request.getKey());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (Exception e) {
      log.error("❌ Failed to create tenant: {}", request.getKey(), e);

      Map<String, Object> errorResponse = Map.of("success", false, "message",
          "Failed to create tenant: " + e.getMessage(), "error", e.getClass().getSimpleName());

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * 🗑️ DELETE /api/admin/tenants/{tenantKey} - Smazání tenantu
   * 
   * POZOR: Toto smaže celý tenant včetně všech dat!
   */
  @DeleteMapping("/{tenantKey}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> deleteTenant(@PathVariable String tenantKey,
      @AuthenticationPrincipal Jwt jwt) {

    try {
      log.warn("🗑️ Deleting tenant: {}", tenantKey);

      // Prevent deletion of admin tenant (renamed from core-platform)
      if ("admin".equals(tenantKey)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("success", false, "message", "Cannot delete admin tenant"));
      }

      // Delete tenant using realm management service
      keycloakRealmManagementService.deleteTenant(tenantKey);

      Map<String, Object> response = Map.of("success", true, "message",
          "Tenant deleted successfully", "tenantKey", tenantKey);

      log.info("✅ Tenant deleted successfully: {}", tenantKey);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Failed to delete tenant: {}", tenantKey, e);

      Map<String, Object> errorResponse = Map.of("success", false, "message",
          "Failed to delete tenant: " + e.getMessage(), "error", e.getClass().getSimpleName());

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * 📋 GET /api/admin/tenants - Seznam všech tenantů (rozšířená verze)
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> getAllTenants(@AuthenticationPrincipal Jwt jwt) {
    try {
      log.debug("📋 Loading all tenants");

      List<Tenant> tenants = tenantService.getAllTenants();

      var tenantList = tenants.stream()
          .map(tenant -> Map.<String, Object>of("id", tenant.getId(), "key", tenant.getKey(),
              "displayName", tenantService.getTenantDisplayName(tenant.getKey()), "realm",
              tenant.getKey(), "subdomain", tenant.getKey() + "." + domain))
          .toList();

      Map<String, Object> response = Map.of("success", true, "tenants", tenantList, "total",
          tenantList.size());

      log.debug("✅ Loaded {} tenants", tenantList.size());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("❌ Failed to load tenants", e);

      Map<String, Object> errorResponse = Map.of("success", false, "message",
          "Failed to load tenants: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }

  /**
   * 📊 GET /api/admin/tenants/{tenantKey}/stats - Statistiky tenantu
   */
  @GetMapping("/{tenantKey}/stats") @PreAuthorize("hasAnyAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> getTenantStats(@PathVariable String tenantKey,
      @AuthenticationPrincipal Jwt jwt) {

    try {
      log.debug("📊 Getting stats for tenant: {}", tenantKey);

      // Get tenant status from Keycloak
      Map<String, Object> tenantStatus = keycloakRealmManagementService.getTenantStatus(tenantKey);

      // Get user count from database
      long userCount = userDirectoryService.countUsersByTenantKey(tenantKey);

      // Get tenant info from database
      Optional<Tenant> tenant = tenantService.findTenantByKey(tenantKey);
      String displayName = tenant.map(t -> tenantService.getTenantDisplayName(t.getKey()))
          .orElse(tenantKey);

      Map<String, Object> stats = Map.of("tenantKey", tenantKey, "displayName", displayName,
          "userCount", userCount, "realmExists", tenantStatus.getOrDefault("realmExists", false),
          "realmEnabled", tenantStatus.getOrDefault("realmEnabled", false), "createdAt",
          tenant.map(t -> t.getId().toString()).orElse("unknown"));

      return ResponseEntity.ok(stats);

    } catch (Exception e) {
      log.error("❌ Failed to get tenant stats: {}", tenantKey, e);

      Map<String, Object> errorResponse = Map.of("success", false, "message",
          "Failed to get tenant stats: " + e.getMessage(), "tenantKey", tenantKey);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
  }
}
