package cz.muriel.core.controller;

import cz.muriel.core.dto.*;
import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.service.S3StorageService;
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
  private final S3StorageService s3StorageService;

  @GetMapping @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserDto> getMyProfile(Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Getting profile for username: {}", username);

    // Místo hledání podle ID použijeme username
    UserDto user = keycloakAdminService.getUserByUsername(username);

    // Přidej správnou URL pro profilový obrázek
    enrichUserWithImageUrl(user);

    return ResponseEntity.ok(user);
  }

  /**
   * Speciální endpoint pro načítání profilu po nahrání fotky s retry mechanismem
   */
  @GetMapping("/with-retry")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserDto> getMyProfileWithRetry(
      @RequestParam(required = false, defaultValue = "customProfileImage") String expectedAttribute,
      @RequestParam(required = false, defaultValue = "3") int maxRetries,
      Authentication authentication) {
    
    String username = getCurrentUsername(authentication);
    log.info("🔄 Getting profile with retry for username: {}, expecting attribute: {}", username, expectedAttribute);

    // Použij retry mechanismus pro čerstvě aktualizované atributy
    UserDto user = keycloakAdminService.getUserByUsernameWithRetry(username, expectedAttribute, maxRetries);

    // Přidej správnou URL pro profilový obrázek
    enrichUserWithImageUrl(user);

    return ResponseEntity.ok(user);
  }

  @PutMapping @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UserUpdateRequest request,
      Authentication authentication) {
    String username = getCurrentUsername(authentication);
    log.info("Updating profile for username: {}", username);

    // Nejdříve najdeme uživatele podle username, abychom získali jeho ID
    UserDto existingUser = keycloakAdminService.getUserByUsername(username);
    String userId = existingUser.getId();

    UserDto updatedUser = keycloakAdminService.updateUser(userId, request);

    // Přidej správnou URL pro profilový obrázek
    enrichUserWithImageUrl(updatedUser);

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

  /**
   * Obohacuje UserDto o správnou URL pro profilový obrázek
   */
  private void enrichUserWithImageUrl(UserDto user) {
    if (user != null) {
      log.info("🔍 PROFIL: Enriching user {} with image URL", user.getUsername());

      String profilePictureKey = null;

      // 🧪 TEST: Zkus najít customProfileImage místo profilePicture
      if (user.getCustomAttributes() != null) {
        log.info("🔍 PROFIL: CustomAttributes map exists, size: {}",
            user.getCustomAttributes().size());
        log.info("🔍 PROFIL: All customAttributes keys: {}", user.getCustomAttributes().keySet());

        profilePictureKey = user.getCustomAttributes().get("customProfileImage");
        log.info("🔍 PROFIL: Found customProfileImage in customAttributes: {}", profilePictureKey);
      } else {
        log.info("🔍 PROFIL: No customAttributes found for user: {}", user.getUsername());
      }

      // Fallback na přímé pole (pro zpětnou kompatibilitu)
      if (profilePictureKey == null || profilePictureKey.isEmpty()) {
        profilePictureKey = user.getProfilePicture();
        log.info("🔍 PROFIL: Using direct profilePicture field: {}", profilePictureKey);
      }

      if (profilePictureKey != null && !profilePictureKey.isEmpty()) {
        String imageUrl = s3StorageService.getFileUrl(profilePictureKey);
        user.setProfilePictureUrl(imageUrl);
        // Zajisti aby bylo nastaveno i přímé pole
        user.setProfilePicture(profilePictureKey);
        log.info("✅ PROFIL: Generated profile picture URL for user {}: {} -> {}",
            user.getUsername(), profilePictureKey, imageUrl);
      } else {
        log.warn("❌ PROFIL: No profile picture found for user: {}", user.getUsername());
      }
    } else {
      log.warn("❌ PROFIL: User is null, cannot enrich with image URL");
    }
  }

  private String getCurrentUsername(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return jwt.getClaimAsString("preferred_username");
  }
}
