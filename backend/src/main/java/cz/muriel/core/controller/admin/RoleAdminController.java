package cz.muriel.core.controller.admin;

import cz.muriel.core.dto.RoleDto;
import cz.muriel.core.dto.UserDto;
import cz.muriel.core.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎭 ADMIN ROLE MANAGEMENT API
 * 
 * Tenant-aware správa rolí pro CORE_ADMIN uživatele
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
public class RoleAdminController {

  private final KeycloakAdminService keycloakAdminService;

  /**
   * 📋 GET /api/admin/roles/tenant/{tenantKey} - Role pro konkrétní tenant
   */
  @GetMapping("/tenant/{tenantKey}")
  public ResponseEntity<List<RoleDto>> getRolesByTenant(@PathVariable String tenantKey) {
    log.info("Getting roles for tenant: {}", tenantKey);
    List<RoleDto> roles = keycloakAdminService.getRolesByTenant(tenantKey);
    return ResponseEntity.ok(roles);
  }

  /**
   * 👥 GET /api/admin/users/tenant/{tenantKey} - Všichni uživatelé tenantu
   */
  @GetMapping("/users/tenant/{tenantKey}")
  public ResponseEntity<List<UserDto>> getUsersByTenant(@PathVariable String tenantKey) {
    log.info("Getting all users for tenant: {}", tenantKey);
    List<UserDto> users = keycloakAdminService.getUsersByTenant(tenantKey);
    return ResponseEntity.ok(users);
  }

  /**
   * 👥 GET /api/admin/roles/{roleName}/users - Uživatelé v roli (s tenant filtrem)
   */
  @GetMapping("/{roleName}/users")
  public ResponseEntity<List<UserDto>> getRoleUsers(
      @PathVariable String roleName,
      @RequestParam String tenantKey) {
    log.info("Getting users with role: {} for tenant: {}", roleName, tenantKey);
    List<UserDto> users = keycloakAdminService.getUsersByRoleAndTenant(roleName, tenantKey);
    return ResponseEntity.ok(users);
  }

  /**
   * ➕ POST /api/admin/roles/{roleName}/users/{userId} - Přidat uživatele do role
   */
  @PostMapping("/{roleName}/users/{userId}")
  public ResponseEntity<Void> addUserToRole(
      @PathVariable String roleName,
      @PathVariable String userId,
      @RequestParam String tenantKey) {
    log.info("Adding user {} to role {} in tenant {}", userId, roleName, tenantKey);
    keycloakAdminService.addRoleToUser(userId, roleName, tenantKey);
    return ResponseEntity.ok().build();
  }

  /**
   * ➖ DELETE /api/admin/roles/{roleName}/users/{userId} - Odebrat uživatele z role
   */
  @DeleteMapping("/{roleName}/users/{userId}")
  public ResponseEntity<Void> removeUserFromRole(
      @PathVariable String roleName,
      @PathVariable String userId,
      @RequestParam String tenantKey) {
    log.info("Removing user {} from role {} in tenant {}", userId, roleName, tenantKey);
    keycloakAdminService.removeRoleFromUser(userId, roleName, tenantKey);
    return ResponseEntity.noContent().build();
  }
}
