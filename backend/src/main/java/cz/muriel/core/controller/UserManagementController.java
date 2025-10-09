package cz.muriel.core.controller;

import cz.muriel.core.dto.*;
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

@Slf4j @RestController @RequestMapping("/api/users") @RequiredArgsConstructor @Validated @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')")
public class UserManagementController {

  private final KeycloakAdminService keycloakAdminService;

  @GetMapping
  public ResponseEntity<List<UserDto>> searchUsers(@RequestParam(required = false) String username,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") Integer first,
      @RequestParam(defaultValue = "20") Integer max) {

    log.info(
        "Searching users with params: username={}, email={}, firstName={}, lastName={}, enabled={}",
        username, email, firstName, lastName, enabled);

    List<UserDto> users = keycloakAdminService.searchUsers(username, email, firstName, lastName,
        enabled, first, max);

    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(@PathVariable String id) {
    log.info("Getting user by ID: {}", id);

    UserDto user = keycloakAdminService.getUserById(id);
    return ResponseEntity.ok(user);
  }

  @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("Creating new user: {}", request.getUsername());

    UserDto createdUser = keycloakAdminService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserDto> updateUser(@PathVariable String id,
      @Valid @RequestBody UserUpdateRequest request) {
    log.info("Updating user: {}", id);

    UserDto updatedUser = keycloakAdminService.updateUser(id, request);
    return ResponseEntity.ok(updatedUser);
  }

  @PutMapping("/{id}/password")
  public ResponseEntity<Void> resetUserPassword(@PathVariable String id,
      @Valid @RequestBody PasswordResetRequest request) {
    log.info("Resetting password for user: {}", id);

    keycloakAdminService.changeUserPassword(id, request.getNewPassword(),
        request.isRequirePasswordChange());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable String id) {
    log.info("Deleting user: {}", id);

    keycloakAdminService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/roles")
  public ResponseEntity<List<String>> getUserRoles(@PathVariable String id) {
    log.info("Getting roles for user: {}", id);

    List<String> roles = keycloakAdminService.getUserRoles(id);
    return ResponseEntity.ok(roles);
  }

  @PostMapping("/{id}/roles")
  public ResponseEntity<Void> assignRoleToUser(@PathVariable String id,
      @RequestBody RoleAssignmentRequest request) {
    log.info("Assigning role {} to user: {}", request.getRoleName(), id);

    keycloakAdminService.assignRoleToUser(id, request.getRoleName());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}/roles/{roleName}")
  public ResponseEntity<Void> removeRoleFromUser(@PathVariable String id,
      @PathVariable String roleName) {
    log.info("Removing role {} from user: {}", roleName, id);

    keycloakAdminService.removeRoleFromUser(id, roleName);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/groups")
  public ResponseEntity<com.fasterxml.jackson.databind.JsonNode> getUserGroups(
      @PathVariable String id) {
    log.info("Getting groups for user: {}", id);

    com.fasterxml.jackson.databind.JsonNode groups = keycloakAdminService.getUserGroups(id);
    return ResponseEntity.ok(groups);
  }

  // Helper DTO for role assignment
  public static class RoleAssignmentRequest {
    private String roleName;

    public String getRoleName() {
      return roleName;
    }

    public void setRoleName(String roleName) {
      this.roleName = roleName;
    }
  }
}
