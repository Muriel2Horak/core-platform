package cz.muriel.core.controller;

import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j @RestController @RequestMapping("/api/users") @RequiredArgsConstructor @Validated @PreAuthorize("hasAnyAuthority('CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN')") @Tag(name = "User Management", description = "User CRUD operations, role assignments, and password management. Requires USER_MANAGER or ADMIN role.") @SecurityRequirement(name = "bearerAuth")
public class UserManagementController {

  private final KeycloakAdminService keycloakAdminService;

  @Operation(summary = "Search users", description = "Search users with flexible filters. Supports pagination. All filters are optional and"
      + " use case-insensitive partial matching.") @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Users found (may be empty list)", content = @Content(schema = @Schema(implementation = UserDto.class))),
          @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT"),
          @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions") }) @GetMapping
  public ResponseEntity<List<UserDto>> searchUsers(
      @Parameter(description = "Username filter (partial match)", example = "john.doe") @RequestParam(required = false) String username,
      @Parameter(description = "Email filter (partial match)", example = "john@example.com") @RequestParam(required = false) String email,
      @Parameter(description = "First name filter (partial match)", example = "John") @RequestParam(required = false) String firstName,
      @Parameter(description = "Last name filter (partial match)", example = "Doe") @RequestParam(required = false) String lastName,
      @Parameter(description = "Enabled status filter", example = "true") @RequestParam(required = false) Boolean enabled,
      @Parameter(description = "Pagination offset (0-based)", example = "0") @RequestParam(defaultValue = "0") Integer first,
      @Parameter(description = "Maximum results per page", example = "20") @RequestParam(defaultValue = "20") Integer max) {

    log.info(
        "Searching users with params: username={}, email={}, firstName={}, lastName={}, enabled={}",
        username, email, firstName, lastName, enabled);

    List<UserDto> users = keycloakAdminService.searchUsers(username, email, firstName, lastName,
        enabled, first, max);

    return ResponseEntity.ok(users);
  }

  @Operation(summary = "Get user by ID", description = "Retrieve full user details by Keycloak ID") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(
      @Parameter(description = "Keycloak user ID", example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String id) {
    log.info("Getting user by ID: {}", id);

    UserDto user = keycloakAdminService.getUserById(id);
    return ResponseEntity.ok(user);
  }

  @Operation(summary = "Create new user", description = "Create a new user in Keycloak. Username and email must be unique. Password will be"
      + " generated if not provided.") @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = UserDto.class))),
          @ApiResponse(responseCode = "400", description = "Validation error - invalid request body"),
          @ApiResponse(responseCode = "409", description = "Conflict - username or email exists"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "403", description = "Forbidden") }) @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
    log.info("Creating new user: {}", request.getUsername());

    UserDto createdUser = keycloakAdminService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @Operation(summary = "Update user", description = "Update user profile information") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @PutMapping("/{id}")
  public ResponseEntity<UserDto> updateUser(
      @Parameter(description = "Keycloak user ID") @PathVariable String id,
      @Valid @RequestBody UserUpdateRequest request) {
    log.info("Updating user: {}", id);

    UserDto updatedUser = keycloakAdminService.updateUser(id, request);
    return ResponseEntity.ok(updatedUser);
  }

  @Operation(summary = "Reset user password", description = "Set a new password for user. Optionally require password change on next login.") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Password reset successfully"),
      @ApiResponse(responseCode = "400", description = "Validation error"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @PutMapping("/{id}/password")
  public ResponseEntity<Void> resetUserPassword(
      @Parameter(description = "Keycloak user ID") @PathVariable String id,
      @Valid @RequestBody PasswordResetRequest request) {
    log.info("Resetting password for user: {}", id);

    keycloakAdminService.changeUserPassword(id, request.getNewPassword(),
        request.isRequirePasswordChange());
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Delete user", description = "Permanently delete user from Keycloak. This action cannot be undone.") @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "User deleted successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "Keycloak user ID") @PathVariable String id) {
    log.info("Deleting user: {}", id);

    keycloakAdminService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get user roles", description = "Retrieve all roles assigned to a specific user") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @GetMapping("/{id}/roles")
  public ResponseEntity<List<String>> getUserRoles(
      @Parameter(description = "Keycloak user ID") @PathVariable String id) {
    log.info("Getting roles for user: {}", id);

    List<String> roles = keycloakAdminService.getUserRoles(id);
    return ResponseEntity.ok(roles);
  }

  @Operation(summary = "Assign role to user", description = "Add a role to user's role assignments") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Role assigned successfully"),
      @ApiResponse(responseCode = "404", description = "User or role not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @PostMapping("/{id}/roles")
  public ResponseEntity<Void> assignRoleToUser(
      @Parameter(description = "Keycloak user ID") @PathVariable String id,
      @RequestBody RoleAssignmentRequest request) {
    log.info("Assigning role {} to user: {}", request.getRoleName(), id);

    keycloakAdminService.assignRoleToUser(id, request.getRoleName());
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Remove role from user", description = "Remove a role from user's role assignments") @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Role removed successfully"),
      @ApiResponse(responseCode = "404", description = "User or role not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @DeleteMapping("/{id}/roles/{roleName}")
  public ResponseEntity<Void> removeRoleFromUser(@PathVariable String id,
      @PathVariable String roleName) {
    log.info("Removing role {} from user: {}", roleName, id);

    keycloakAdminService.removeRoleFromUser(id, roleName);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Get user groups", description = "Retrieve all groups where user is a member") @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Groups retrieved successfully"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden") }) @GetMapping("/{id}/groups")
  public ResponseEntity<com.fasterxml.jackson.databind.JsonNode> getUserGroups(
      @Parameter(description = "Keycloak user ID") @PathVariable String id) {
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
