package cz.muriel.core.locks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for edit locks
 */
@Slf4j @RestController @RequestMapping("/api/locks") @RequiredArgsConstructor
public class EditLockController {

  private final EditLockService lockService;

  /**
   * Acquire or renew lock
   */
  @PostMapping("/{entityType}/{entityId}")
  public ResponseEntity<?> acquireLock(@PathVariable String entityType,
      @PathVariable String entityId, @RequestBody AcquireLockRequest request, Authentication auth) {
    try {
      String tenantId = getTenantId(auth);
      String userId = getUserId(auth);

      int ttl = request.ttlSeconds != null ? request.ttlSeconds : 300; // 5 min default

      EditLock lock = lockService.acquireLock(tenantId, entityType, entityId, userId, ttl);

      return ResponseEntity.ok(Map.of("success", true, "lock", lock));

    } catch (LockConflictException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "lock_conflict",
          "message", e.getMessage(), "existingLock", e.getExistingLock()));
    }
  }

  /**
   * Release lock
   */
  @DeleteMapping("/{entityType}/{entityId}")
  public ResponseEntity<?> releaseLock(@PathVariable String entityType,
      @PathVariable String entityId, Authentication auth) {
    String tenantId = getTenantId(auth);
    String userId = getUserId(auth);
    boolean isAdmin = hasRole(auth, "CORE_ROLE_ADMIN");

    boolean released = lockService.releaseLock(tenantId, entityType, entityId, userId, isAdmin);

    if (released) {
      return ResponseEntity.ok(Map.of("success", true));
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "lock_not_found", "message", "Lock not found or already released"));
    }
  }

  /**
   * Get lock status
   */
  @GetMapping("/{entityType}/{entityId}")
  public ResponseEntity<?> getLockStatus(@PathVariable String entityType,
      @PathVariable String entityId, Authentication auth) {
    String tenantId = getTenantId(auth);

    return lockService.getLock(tenantId, entityType, entityId)
        .map(lock -> ResponseEntity.ok((Object) Map.of("locked", true, "lock", lock)))
        .orElse(ResponseEntity.ok(Map.of("locked", false)));
  }

  private String getTenantId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt.getClaimAsString("tenant_id");
    }
    return "admin"; // fallback
  }

  private String getUserId(Authentication auth) {
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String sub = jwt.getSubject();
      return sub != null ? sub : jwt.getClaimAsString("user_id");
    }
    return auth != null ? auth.getName() : "unknown";
  }

  private boolean hasRole(Authentication auth, String role) {
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
  }

  record AcquireLockRequest(Integer ttlSeconds, String lockType) {
  }
}
