package cz.muriel.core.controller;

import cz.muriel.core.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🔐 Permission Controller API endpoint pro zjišťování permissions aktuálního
 * uživatele nebo konkrétní role
 */
@RestController @RequestMapping("/api/permissions") @RequiredArgsConstructor @Slf4j
public class PermissionController {

  private final PermissionService permissionService;

  /**
   * GET /api/permissions/me Vrátí permissions aktuálně přihlášeného uživatele
   */
  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getMyPermissions(Authentication authentication) {
    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).map(auth -> auth.replace("ROLE_", ""))
        .collect(Collectors.toList());

    log.debug("Getting permissions for user with roles: {}", roles);

    Map<String, Object> permissions = permissionService.getPermissionInfo(roles);
    return ResponseEntity.ok(permissions);
  }

  /**
   * GET /api/permissions/roles/{roleName} Vrátí permissions pro konkrétní roli
   * (pro admin UI)
   */
  @GetMapping("/roles/{roleName}")
  public ResponseEntity<Map<String, Object>> getRolePermissions(@PathVariable String roleName) {
    log.debug("Getting permissions for role: {}", roleName);

    Map<String, Object> permissions = permissionService.getRolePermissionInfo(roleName);

    if (permissions.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(permissions);
  }

  /**
   * POST /api/permissions/check Zkontroluje, zda aktuální uživatel má dané
   * permission
   * 
   * Body: { "permission": "users:read:all" }
   */
  @PostMapping("/check")
  public ResponseEntity<Map<String, Object>> checkPermission(
      @RequestBody Map<String, String> request, Authentication authentication) {

    String permission = request.get("permission");

    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).map(auth -> auth.replace("ROLE_", ""))
        .collect(Collectors.toList());

    boolean hasPermission = permissionService.hasPermission(roles, permission);

    Map<String, Object> response = Map.of("permission", permission, "granted", hasPermission);

    return ResponseEntity.ok(response);
  }
}
