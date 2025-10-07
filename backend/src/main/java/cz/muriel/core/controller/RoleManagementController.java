package cz.muriel.core.controller;

import cz.muriel.core.dto.RoleCreateRequest;
import cz.muriel.core.dto.RoleDto;
import cz.muriel.core.dto.UserDto;
import cz.muriel.core.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 🎭 ROLE MANAGEMENT API
 * 
 * Kompletní správa rolí včetně composite role hierarchies
 */
@Slf4j @RestController @RequestMapping("/api/roles") @RequiredArgsConstructor @Validated
public class RoleManagementController {

  private final KeycloakAdminService keycloakAdminService;

  /**
   * 📋 GET /api/roles - Seznam všech rolí
   */
  @GetMapping @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER', 'CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<List<RoleDto>> getAllRoles() {
    log.info("Getting all roles");
    List<RoleDto> roles = keycloakAdminService.getAllRoles();
    return ResponseEntity.ok(roles);
  }

  /**
   * 🔍 GET /api/roles/{name} - Detail role
   */
  @GetMapping("/{name}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER', 'CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name) {
    log.info("Getting role by name: {}", name);
    RoleDto role = keycloakAdminService.getRoleByName(name);
    return ResponseEntity.ok(role);
  }

  /**
   * 🆕 POST /api/roles - Vytvoření nové role
   */
  @PostMapping @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleCreateRequest request) {
    log.info("Creating new role: {}", request.getName());
    RoleDto createdRole = keycloakAdminService.createRole(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
  }

  /**
   * ✏️ PUT /api/roles/{name} - Aktualizace role
   */
  @PutMapping("/{name}") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<RoleDto> updateRole(@PathVariable String name,
      @Valid @RequestBody RoleCreateRequest request) {
    log.info("Updating role: {}", name);
    RoleDto updatedRole = keycloakAdminService.updateRole(name, request);
    return ResponseEntity.ok(updatedRole);
  }

  /**
   * 🗑️ DELETE /api/roles/{name} - Smazání role
   */
  @DeleteMapping("/{name}") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Void> deleteRole(@PathVariable String name) {
    log.info("Deleting role: {}", name);
    keycloakAdminService.deleteRole(name);
    return ResponseEntity.noContent().build();
  }

  /**
   * 🔗 GET /api/roles/{name}/composites - Získat child role (composite role
   * members)
   */
  @GetMapping("/{name}/composites") @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<List<RoleDto>> getRoleComposites(@PathVariable String name) {
    log.info("Getting composite roles for: {}", name);
    List<RoleDto> composites = keycloakAdminService.getRoleCompositesList(name);
    return ResponseEntity.ok(composites);
  }

  /**
   * 🔗 POST /api/roles/{name}/composites - Přidat child role do composite role
   */
  @PostMapping("/{name}/composites") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Void> addCompositeRole(@PathVariable String name,
      @RequestBody Map<String, String> request) {
    String childRoleName = request.get("childRoleName");
    log.info("Adding composite role {} to {}", childRoleName, name);
    keycloakAdminService.addCompositeRole(name, childRoleName);
    return ResponseEntity.ok().build();
  }

  /**
   * 🔗 DELETE /api/roles/{name}/composites/{childName} - Odebrat child role z
   * composite role
   */
  @DeleteMapping("/{name}/composites/{childName}") @PreAuthorize("hasAuthority('CORE_ROLE_ADMIN')")
  public ResponseEntity<Void> removeCompositeRole(@PathVariable String name,
      @PathVariable String childName) {
    log.info("Removing composite role {} from {}", childName, name);
    keycloakAdminService.removeCompositeRole(name, childName);
    return ResponseEntity.noContent().build();
  }

  /**
   * 👥 GET /api/roles/{name}/users - Získat uživatele s danou rolí
   */
  @GetMapping("/{name}/users") @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
  public ResponseEntity<List<UserDto>> getRoleUsers(@PathVariable String name) {
    log.info("Getting users with role: {}", name);
    List<UserDto> users = keycloakAdminService.getUsersByRole(name);
    return ResponseEntity.ok(users);
  }
}
