package cz.muriel.core.controller;

import cz.muriel.core.dto.*;
import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/me") @RequiredArgsConstructor @Validated
public class UserProfileController {

  private final KeycloakAdminService keycloakAdminService;

  @GetMapping @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Getting profile for username: {}", username);

    // Místo hledání podle ID použijeme username
    UserDto user = keycloakAdminService.getUserByUsername(username);

    // 🏢 Naplnění tenant informace z aktuálního kontextu
    user.setTenant(TenantContext.getTenantKey());

    return ResponseEntity.ok(user);
  }

  // 🆕 CDC ENDPOINT: Kontrola změn uživatelských dat
  @GetMapping("/changes") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> checkUserChanges(Authentication authentication,
      @RequestParam(required = false) Long since) {

    String username = getCurrentUsername(authentication);
    log.debug("Checking changes for username: {} since: {}", username, since);

    try {
      // Timestamp poslední změny (pro jednoduchost použijeme aktuální čas)
      // V produkci by to mělo být skutečné datum poslední změny z Keycloak/DB
      long currentTimestamp = System.currentTimeMillis();

      // Pokud je zadán parametr 'since', kontrolujeme, jestli došlo ke změně
      boolean hasChanges = false;
      if (since != null) {
        // Pro demo účely: změna nastala, pokud uplynulo více než 30s
        // V produkci by se kontrolovalo skutečné datum modifikace
        hasChanges = (currentTimestamp - since) > 30000;
      }

      Map<String, Object> response = Map.of("timestamp", currentTimestamp, "hasChanges", hasChanges,
          "username", username, "lastModified", currentTimestamp);

      log.debug("Changes check result: {}", response);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Failed to check user changes for username: {}", username, e);
      return ResponseEntity.ok(Map.of("timestamp", System.currentTimeMillis(), "hasChanges", false,
          "error", e.getMessage()));
    }
  }

  @PutMapping
  @PatchMapping  // Support both PUT and PATCH for profile updates
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UserUpdateRequest request,
      Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Updating profile for username: {}", username);

    // Nejdříve najdeme uživatele podle username, abychom získali jeho ID
    UserDto existingUser = keycloakAdminService.getUserByUsername(username);
    String userId = existingUser.getId();

    UserDto updatedUser = keycloakAdminService.updateUser(userId, request);
    return ResponseEntity.ok(updatedUser);
  }

  @PutMapping("/password") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request,
      Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Changing password for username: {}", username);

    // Nejdříve najdeme uživatele podle username, abychom získali jeho ID
    UserDto existingUser = keycloakAdminService.getUserByUsername(username);
    String userId = existingUser.getId();

    // Validate that new password and confirmation match
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new IllegalArgumentException("Nové heslo a potvrzení se neshodují");
    }

    // Validate current password
    if (!keycloakAdminService.validateUserPassword(username, request.getCurrentPassword())) {
      throw new IllegalArgumentException("Současné heslo je nesprávné");
    }

    // Change password
    keycloakAdminService.changeUserPassword(userId, request.getNewPassword(), false);

    return ResponseEntity.ok().build();
  }

  private String getCurrentUsername(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return jwt.getClaimAsString("preferred_username");
  }
}
