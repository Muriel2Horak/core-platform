package cz.muriel.core.controller;

import cz.muriel.core.entity.UserDirectoryEntity;
import cz.muriel.core.service.UserDirectoryService;
import cz.muriel.core.service.TenantService;
import cz.muriel.core.tenant.TenantContext;
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

@RestController @RequestMapping("/api/users-directory") // OPRAVENO: Vlastný mapping pre Directory
                                                        // API
@RequiredArgsConstructor @Slf4j
public class UserDirectoryController {

  private final UserDirectoryService userDirectoryService;
  private final TenantService tenantService;

  /**
   * 🔍 GET /api/users-directory - Hlavní endpoint pro User Directory Zabezpečený
   * endpoint s role-based access
   */
  @GetMapping @PreAuthorize("isAuthenticated()") // Všichni přihlášení uživatelé mohou číst
                                                 // directory
  public ResponseEntity<Map<String, Object>> getUsersDirectory(
      @RequestParam(required = false) String q, @RequestParam(required = false) String tenantId,
      @RequestParam(required = false) String source, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "username") String sort, @AuthenticationPrincipal Jwt jwt) {

    log.info(
        "🔍 User Directory API called by user: {} - q={}, tenantId={}, source={}, page={}, size={}",
        jwt.getSubject(), q, tenantId, source, page, size);

    try {
      // Permission check - všichni přihlášení mohou číst
      boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");

      // Tenant scoping logic
      String effectiveTenantKey = null;
      if (isCoreAdmin && tenantId != null && !tenantId.isEmpty()) {
        // Core admin s konkrétním tenant filtrem
        // tenantId může být buď UUID nebo tenant key - zkusíme oba formáty
        effectiveTenantKey = resolveTenantKey(tenantId);
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

    } catch (Exception e) {
      log.error("❌ Error in User Directory API for user: {}", jwt.getSubject(), e);
      Map<String, Object> errorResponse = Map.of("error", "Internal server error", "message",
          e.getMessage(), "timestamp", java.time.Instant.now().toString());
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
    boolean sameTenant = user.getTenantId().toString().equals(getCurrentTenantId(jwt));

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

  private String getCurrentTenantId(Jwt jwt) {
    // Try different tenant claim names
    String tenantId = jwt.getClaimAsString("tenant_id");
    if (tenantId != null)
      return tenantId;

    String tenant = jwt.getClaimAsString("tenant");
    if (tenant != null)
      return tenant;

    // Fallback to current tenant context
    try {
      UUID currentTenantId = tenantService.getCurrentTenantIdOrThrow();
      return currentTenantId.toString();
    } catch (Exception e) {
      log.warn("No tenant found in JWT or context", e);
      return null;
    }
  }

  private boolean canEditUser(UserDirectoryEntity user, Jwt jwt) {
    String currentUserId = jwt.getSubject();
    boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");
    boolean isTenantAdmin = hasRole(jwt, "CORE_ROLE_TENANT_ADMIN")
        || hasRole(jwt, "CORE_ROLE_USER_MANAGER");
    boolean isMe = currentUserId.equals(user.getKeycloakUserId());
    boolean sameTenant = user.getTenantId().toString().equals(getCurrentTenantId(jwt));

    return isMe || isCoreAdmin || (isTenantAdmin && sameTenant);
  }

  private boolean canDeleteUser(UserDirectoryEntity user, Jwt jwt) {
    boolean isCoreAdmin = hasRole(jwt, "CORE_ROLE_ADMIN");
    boolean isTenantAdmin = hasRole(jwt, "CORE_ROLE_TENANT_ADMIN")
        || hasRole(jwt, "CORE_ROLE_USER_MANAGER");
    boolean sameTenant = user.getTenantId().toString().equals(getCurrentTenantId(jwt));

    return isCoreAdmin || (isTenantAdmin && sameTenant);
  }

  private Map<String, Object> buildUserResponse(UserDirectoryEntity user) {
    return Map.of("id", user.getId(), "username", user.getUsername(), "firstName",
        user.getFirstName() != null ? user.getFirstName() : "", "lastName",
        user.getLastName() != null ? user.getLastName() : "", "email",
        user.getEmail() != null ? user.getEmail() : "", "tenantId", user.getTenantId(),
        "tenantName", getTenantName(user.getTenantId()), "directorySource",
        user.getIsFederated() ? "AD" : "LOCAL", "isFederated", user.getIsFederated(), "updatedAt",
        user.getUpdatedAt());
  }

  private Map<String, Object> buildUserDetailResponse(UserDirectoryEntity user, Jwt jwt) {
    Map<String, Object> response = buildUserResponse(user);

    // Add editability flags
    response.put("isEditableByMe", canEditUser(user, jwt));
    response.put("isDeletableByMe", canDeleteUser(user, jwt));

    // Add detailed fields
    response.put("department", user.getDepartment());
    response.put("title", user.getTitle());
    response.put("phoneNumber", user.getPhoneNumber());
    response.put("status", user.getStatus());
    response.put("active", user.getActive());

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

  private String getTenantName(UUID tenantId) {
    try {
      return tenantService.getTenantNameById(tenantId);
    } catch (Exception e) {
      log.warn("Failed to get tenant name for ID: {}", tenantId);
      return "Unknown Tenant";
    }
  }

  private String resolveTenantKey(String tenantId) {
    try {
      // Try to parse as UUID
      UUID tenantUuid = UUID.fromString(tenantId);
      return tenantService.getTenantKeyById(tenantUuid);
    } catch (IllegalArgumentException e) {
      // Not a UUID, assume it's already a tenant key
      return tenantId;
    }
  }
}
