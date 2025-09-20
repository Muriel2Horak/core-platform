package cz.muriel.core.controller;

import cz.muriel.core.dto.RoleCreateRequest;
import cz.muriel.core.dto.RoleDto;
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

@Slf4j @RestController @RequestMapping("/api/roles") @RequiredArgsConstructor @Validated @PreAuthorize("hasRole('CORE_USER_ADMIN')")
public class RoleManagementController {

  private final KeycloakAdminService keycloakAdminService;

  @GetMapping
  public ResponseEntity<List<RoleDto>> getAllRoles() {
    log.info("Getting all roles");

    List<RoleDto> roles = keycloakAdminService.getAllRoles();
    return ResponseEntity.ok(roles);
  }

  @GetMapping("/{name}")
  public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name) {
    log.info("Getting role by name: {}", name);

    RoleDto role = keycloakAdminService.getRoleByName(name);
    return ResponseEntity.ok(role);
  }

  @PostMapping
  public ResponseEntity<RoleDto> createRole(@Valid @RequestBody RoleCreateRequest request) {
    log.info("Creating new role: {}", request.getName());

    RoleDto createdRole = keycloakAdminService.createRole(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
  }
}
