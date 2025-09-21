package cz.muriel.core.controller;

import cz.muriel.core.dto.*;
import cz.muriel.core.auth.KeycloakAdminService;
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

  @GetMapping
  @PreAuthorize("hasAuthority('CORE_ROLE_USER')")
  public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
    String userId = getCurrentUserId(authentication);
    log.info("Getting profile for user: {}", userId);

    UserDto user = keycloakAdminService.getUserById(userId);
    return ResponseEntity.ok(user);
  }

  @PutMapping
  @PreAuthorize("hasAuthority('CORE_ROLE_USER')")
  public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UserUpdateRequest request,
      Authentication authentication) {
    String userId = getCurrentUserId(authentication);
    log.info("Updating profile for user: {}", userId);

    UserDto updatedUser = keycloakAdminService.updateUser(userId, request);
    return ResponseEntity.ok(updatedUser);
  }

  @PutMapping("/password")
  @PreAuthorize("hasAuthority('CORE_ROLE_USER')")
  public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody PasswordChangeRequest request,
      Authentication authentication) {
    String userId = getCurrentUserId(authentication);
    String username = getCurrentUsername(authentication);

    log.info("Changing password for user: {}", userId);

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

  private String getCurrentUserId(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();

    // DEBUG: Vypíšeme všechny claims pro debugging
    log.debug("JWT Claims: {}", jwt.getClaims());
    log.debug("Available claims: {}", jwt.getClaims().keySet());

    // Zkusíme několik možných claims pro user ID
    String userId = jwt.getClaimAsString("sub");
    if (userId == null) {
      userId = jwt.getClaimAsString("user_id");
    }
    if (userId == null) {
      userId = jwt.getClaimAsString("id");
    }

    log.debug("Extracted user ID: {}", userId);
    return userId;
  }

  private String getCurrentUsername(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return jwt.getClaimAsString("preferred_username");
  }
}
