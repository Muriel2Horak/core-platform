package cz.muriel.core.controller;

import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.service.UserDirectoryService;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController 
@RequestMapping({ 
    "/api/user-directories",           // NEW: Plural kebab-case (S1 naming standard)
    "/api/users-directory"              // DEPRECATED: Keep for backward compatibility (remove in v2.3.0)
}) 
@RequiredArgsConstructor 
@Slf4j
@Tag(name = "User Directory", description = "User directory API with multi-tenant support and role-based access control")
public class UserDirectoryController {

  private final UserDirectoryService userDirectoryService;
  private final TenantService tenantService;

  /**
   * 🔍 GET /api/user-directories - Hlavní endpoint pro User Directory
   * Zabezpečený endpoint s role-based access.
   * 
   * Note: Legacy path /api/users-directory is still supported but will be removed
   * in v2.3.0. Please use /api/user-directories instead.
   */
  @Operation(
      summary = "Get user directory",
      description = """
          Retrieves paginated user directory with optional filtering and tenant scoping.
          
          **Authentication:** Required (all authenticated users can access)
          
          **Tenant Scoping:**
          - Regular users: See only their own tenant's users
          - CORE_ROLE_ADMIN: Can query any tenant or all tenants
          
          **Deprecated Path:** `/api/users-directory` is still supported but will be removed in v2.3.0.
          Use `/api/user-directories` instead.
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User directory retrieved successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
      @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
      @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
  })
  @GetMapping 
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getUsersDirectory(
      @Parameter(description = "Search query (username, email, firstName, lastName)", example = "john")
      @RequestParam(required = false) String q,
      
      @Parameter(description = "Tenant key filter (CORE_ROLE_ADMIN only)", example = "acme-corp")
      @RequestParam(required = false) String tenantKey,
      
      @Parameter(description = "User source filter (local, keycloak, ldap)", example = "keycloak")
      @RequestParam(required = false) String source,
      
      @Parameter(description = "Page number (0-based)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      
      @Parameter(description = "Page size (1-100)", example = "20")
      @RequestParam(defaultValue = "20") int size,
      
      @Parameter(description = "Sort field", example = "username")
      @RequestParam(defaultValue = "username") String sort,
      
      @AuthenticationPrincipal Jwt jwt) {

    log.info(
        "🔍 User Directory API called by user: {} - q={}, tenantKey={}, source={}, page={}, size={}",
        jwt.getSubject(), q, tenantKey, source, page, size);

    try {
      // Validate pagination parameters
      if (page < 0)
        page = 0;
      if (size < 1)
        size = 20;
      if (size > 100)
        size = 100; // Max 100 items per page

      // Permission check - všichni přihlášení mohou číst
      boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");

      // Tenant scoping logic
      String effectiveTenantKey = null;
      if (isCoreAdmin && tenantKey != null && !tenantKey.isEmpty()) {
        // Core admin s konkrétním tenant filtrem - použij přímo tenant key
        effectiveTenantKey = tenantKey;
      } else if (!isCoreAdmin) {
        // Běžný user/tenant-admin - pouze svůj tenant
        effectiveTenantKey = TenantContext.getTenantKey();
      }

      Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

      // Search with tenant scoping
      Page<UserDirectoryEntity> users;
      if (effectiveTenantKey != null) {
        // Set tenant context for scoped search
        String originalTenant = TenantContext.getTenantKey();
        try {
          TenantContext.setTenantKey(effectiveTenantKey);
          users = userDirectoryService.searchWithPagination(q, source, pageable);
        } finally {
          if (originalTenant != null) {
            TenantContext.setTenantKey(originalTenant);
          } else {
            TenantContext.clear();
          }
        }
      } else {
        // Core admin - all tenants search
        users = userDirectoryService.searchAllTenantsWithPagination(q, source, pageable);
      }

      List<Map<String, Object>> content = users.getContent().stream().map(this::buildUserResponse)
          .toList();

      Map<String, Object> response = Map.of("content", content, "totalElements",
          users.getTotalElements(), "totalPages", users.getTotalPages(), "number",
          users.getNumber(), "size", users.getSize(), "first", users.isFirst(), "last",
          users.isLast());

      log.info("✅ User Directory API returned {} users for user: {}", users.getTotalElements(),
          jwt.getSubject());
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.error("❌ Invalid parameters in User Directory API: {}", e.getMessage());
      Map<String, Object> errorResponse = Map.of("error", "Invalid parameters", "message",
          e.getMessage(), "timestamp", java.time.Instant.now().toString());
      return ResponseEntity.status(400).body(errorResponse);

    } catch (Exception e) {
      log.error("❌ Error in User Directory API for user: {}", jwt.getSubject(), e);
      Map<String, Object> errorResponse = Map.of("error", "Internal server error", "message",
          "Failed to search users. Please try again.", "timestamp",
          java.time.Instant.now().toString());
      return ResponseEntity.status(500).body(errorResponse);
    }
  }

  /**
   * 👤 GET /api/users-directory/{id} - Detail uživatele
   */
  @GetMapping("/{id}") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable UUID id,
      @AuthenticationPrincipal Jwt jwt) {

    log.info("📄 User detail requested: {} by user: {}", id, jwt.getSubject());

    Optional<UserDirectoryEntity> userOpt = userDirectoryService.findById(id);

    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    UserDirectoryEntity user = userOpt.get();

    // Permission check
    String currentUserId = jwt.getSubject();
    boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");
    boolean isTenantAdmin = hasRole(jwt, "CORE_ROLE_TENANT_ADMIN")
        || hasRole(jwt, "CORE_ROLE_USER_MANAGER");
    boolean isMe = currentUserId.equals(user.getKeycloakUserId());
    // Convert tenant key to UUID for comparison
    UUID currentTenantId = tenantService.getTenantIdFromKey(getCurrentTenantKey(jwt));
    boolean sameTenant = user.getTenantId().equals(currentTenantId);

    // Access control
    if (!isCoreAdmin && !isMe && !(isTenantAdmin && sameTenant)) {
      log.warn("Access denied for user {} to view user detail {}", jwt.getSubject(), id);
      return ResponseEntity.notFound().build(); // Hide existence
    }

    Map<String, Object> response = buildUserDetailResponse(user, jwt);
    return ResponseEntity.ok(response);
  }

  /**
   * ✏️ PATCH /api/users-directory/{id} - Editace uživatele
   */
  @PatchMapping("/{id}") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> updateUser(@PathVariable UUID id,
      @RequestBody Map<String, Object> updates, @AuthenticationPrincipal Jwt jwt) {

    log.info("✏️ User update requested: {} by user: {}", id, jwt.getSubject());

    Optional<UserDirectoryEntity> userOpt = userDirectoryService.findById(id);

    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    UserDirectoryEntity user = userOpt.get();

    // Permission check
    if (!canEditUser(user, jwt)) {
      log.warn("Edit access denied for user {} to edit user {}", jwt.getSubject(), id);
      return ResponseEntity.status(403).build();
    }

    // Apply allowed updates
    applyUserUpdates(user, updates);
    UserDirectoryEntity updated = userDirectoryService.save(user);

    Map<String, Object> response = buildUserDetailResponse(updated, jwt);
    return ResponseEntity.ok(response);
  }

  /**
   * 🗑️ DELETE /api/users-directory/{id} - Smazání uživatele
   */
  @DeleteMapping("/{id}") @PreAuthorize("hasAnyAuthority('CORE_ROLE_ADMIN', 'CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_USER_MANAGER')")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {

    log.info("🗑️ User delete requested: {} by user: {}", id, jwt.getSubject());

    Optional<UserDirectoryEntity> userOpt = userDirectoryService.findById(id);

    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    UserDirectoryEntity user = userOpt.get();

    // Permission check
    if (!canDeleteUser(user, jwt)) {
      log.warn("Delete access denied for user {} to delete user {}", jwt.getSubject(), id);
      return ResponseEntity.status(403).build();
    }

    userDirectoryService.deleteById(id);
    log.info("✅ User {} successfully deleted by {}", id, jwt.getSubject());
    return ResponseEntity.noContent().build();
  }

  // Helper methods

  private boolean hasRole(Jwt jwt, String role) {
    try {
      // First try realm_access.roles
      var realmAccess = jwt.getClaim("realm_access");
      if (realmAccess instanceof Map) {
        Object roles = ((Map<?, ?>) realmAccess).get("roles");
        if (roles instanceof List) {
          return ((List<?>) roles).contains(role);
        }
      }

      // Fallback to direct roles claim
      var rolesClaim = jwt.getClaimAsStringList("roles");
      if (rolesClaim != null) {
        return rolesClaim.contains(role);
      }

      return false;
    } catch (Exception e) {
      log.warn("Failed to parse roles from JWT", e);
      return false;
    }
  }

  private boolean canEditUser(UserDirectoryEntity user, Jwt jwt) {
    String currentUserId = jwt.getSubject();
    boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");
    boolean isTenantAdmin = hasRole(jwt, "CORE_ROLE_TENANT_ADMIN")
        || hasRole(jwt, "CORE_ROLE_USER_MANAGER");
    boolean isMe = currentUserId.equals(user.getKeycloakUserId());
    // Convert tenant key to UUID for comparison
    UUID currentTenantId = tenantService.getTenantIdFromKey(getCurrentTenantKey(jwt));
    boolean sameTenant = user.getTenantId().equals(currentTenantId);

    return isMe || isCoreAdmin || (isTenantAdmin && sameTenant);
  }

  private boolean canDeleteUser(UserDirectoryEntity user, Jwt jwt) {
    boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");
    boolean isTenantAdmin = hasRole(jwt, "CORE_ROLE_TENANT_ADMIN")
        || hasRole(jwt, "CORE_ROLE_USER_MANAGER");
    // Convert tenant key to UUID for comparison
    UUID currentTenantId = tenantService.getTenantIdFromKey(getCurrentTenantKey(jwt));
    boolean sameTenant = user.getTenantId().equals(currentTenantId);

    return isCoreAdmin || (isTenantAdmin && sameTenant);
  }

  private Map<String, Object> buildUserResponse(UserDirectoryEntity user) {
    // Convert UUID tenantId to String tenantKey for API response
    String tenantKey = tenantService.getTenantKeyFromId(user.getTenantId());

    // Build manager username from manager entity
    String managerUsername = null;
    if (user.getManager() != null) {
      managerUsername = user.getManager().getUsername();
    }

    Map<String, Object> response = new java.util.HashMap<>();
    response.put("id", user.getId());
    response.put("username", user.getUsername());
    response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
    response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
    response.put("email", user.getEmail() != null ? user.getEmail() : "");
    response.put("tenantKey", tenantKey);
    response.put("tenantName", getTenantNameByKey(tenantKey));
    response.put("directorySource", user.getIsFederated() ? "AD" : "LOCAL");
    response.put("isFederated", user.getIsFederated());
    response.put("enabled", user.getActive() != null ? user.getActive() : false);
    response.put("manager", managerUsername != null ? managerUsername : "");
    response.put("updatedAt", user.getUpdatedAt());

    return response;
  }

  private Map<String, Object> buildUserDetailResponse(UserDirectoryEntity user, Jwt jwt) {
    Map<String, Object> response = new java.util.HashMap<>(buildUserResponse(user));

    // Add editability flags
    response.put("isEditableByMe", canEditUser(user, jwt));
    response.put("isDeletableByMe", canDeleteUser(user, jwt));

    // Add detailed fields
    response.put("department", user.getDepartment());
    response.put("title", user.getTitle());
    response.put("phoneNumber", user.getPhoneNumber());
    response.put("status", user.getStatus());
    // active je již v buildUserResponse jako "enabled"

    return response;
  }

  private void applyUserUpdates(UserDirectoryEntity user, Map<String, Object> updates) {
    // Apply safe updates only
    if (updates.containsKey("firstName")) {
      user.setFirstName((String) updates.get("firstName"));
    }
    if (updates.containsKey("lastName")) {
      user.setLastName((String) updates.get("lastName"));
    }
    if (updates.containsKey("department")) {
      user.setDepartment((String) updates.get("department"));
    }
    if (updates.containsKey("title")) {
      user.setTitle((String) updates.get("title"));
    }
    if (updates.containsKey("phoneNumber")) {
      user.setPhoneNumber((String) updates.get("phoneNumber"));
    }
    // Note: email, username jsou read-only pro AD uživatele
  }

  private String getTenantNameByKey(String tenantKey) {
    try {
      return tenantService.getTenantDisplayName(tenantKey);
    } catch (Exception e) {
      log.warn("Failed to get tenant name for key: {}", tenantKey);
      return "Unknown Tenant";
    }
  }

  private String getCurrentTenantKey(Jwt jwt) {
    // Try different tenant claim names
    String tenantKey = jwt.getClaimAsString("tenant");
    if (tenantKey != null)
      return tenantKey;

    // Fallback to current tenant context
    try {
      return TenantContext.getTenantKey();
    } catch (Exception e) {
      log.warn("No tenant found in JWT or context", e);
      return null;
    }
  }
}
