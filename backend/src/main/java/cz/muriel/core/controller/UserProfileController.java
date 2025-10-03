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

@Slf4j @RestController @RequestMapping("/api/me") @RequiredArgsConstructor @Validated
public class UserProfileController {

  private final KeycloakAdminService keycloakAdminService;

  @GetMapping @PreAuthorize("isAuthenticated()") // 🔧 FIX: Změněno z hasAuthority('CORE_ROLE_USER')
                                                 // na isAuthenticated()
  public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Getting profile for username: {}", username);

    // Místo hledání podle ID použijeme username
    UserDto user = keycloakAdminService.getUserByUsername(username);

    // 🏢 Naplnění tenant informace z aktuálního kontextu
    user.setTenant(TenantContext.getTenantKey());

    return ResponseEntity.ok(user);
  }

  @PutMapping @PreAuthorize("isAuthenticated()") // 🔧 FIX: Změněno z hasAuthority('CORE_ROLE_USER')
                                                 // na isAuthenticated()
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

  @PutMapping("/password") @PreAuthorize("isAuthenticated()") // 🔧 FIX: Změněno z
                                                              // hasAuthority('CORE_ROLE_USER') na
                                                              // isAuthenticated()
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
