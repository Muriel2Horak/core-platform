package cz.muriel.core.controller;

import cz.muriel.core.dto.UserDto;
import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/files") @RequiredArgsConstructor
public class FileUploadController {

  private final S3StorageService s3StorageService;
  private final KeycloakAdminService keycloakAdminService;

  /**
   * Upload profilového obrázku pro aktuálního uživatele
   */
  @PostMapping("/profile-picture") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, String>> uploadProfilePicture(
      @RequestParam("file") MultipartFile file, Authentication authentication) {

    String username = getCurrentUsername(authentication);
    log.info("Uploading profile picture for user: {}", username);

    try {
      // Validace souboru
      if (file.isEmpty()) {
        throw new IllegalArgumentException("Soubor je prázdný");
      }

      // Kontrola typu souboru
      String contentType = file.getContentType();
      if (contentType == null || !contentType.startsWith("image/")) {
        throw new IllegalArgumentException("Soubor musí být obrázek");
      }

      // Kontrola velikosti (5MB limit)
      long maxSize = 5 * 1024 * 1024; // 5MB
      if (file.getSize() > maxSize) {
        throw new IllegalArgumentException("Soubor je příliš velký (max 5MB)");
      }

      // Najdi uživatele podle username
      log.info("🔍 PROFIL_UPLOAD: Looking up user by username: {}", username);
      UserDto user = keycloakAdminService.getUserByUsername(username);
      String userId = user.getId();
      log.info("🔍 PROFIL_UPLOAD: Found user with ID: {}", userId);

      // S verzováním už nemusíme mazat staré soubory manuálně - MinIO se postará samo

      // Upload nového obrázku - OPRAVENO: Použijeme specializovanou metodu
      log.info("🔍 PROFIL_UPLOAD: Starting S3 upload for user: {}", userId);
      String s3Key = s3StorageService.uploadProfilePicture(file, userId);
      log.info("🔍 PROFIL_UPLOAD: S3 upload completed. S3 key: {}", s3Key);

      // Aktualizuj profil uživatele v Keycloak attributes
      log.info("🔍 PROFIL_UPLOAD: Starting Keycloak attribute update for user: {} with s3Key: {}",
          userId, s3Key);
      try {
        // Update Keycloak attribute for profile picture
        log.info("🔍 PROFIL_UPLOAD: Starting Keycloak attribute update for user: {} with s3Key: {}", userId, s3Key);
        keycloakAdminService.updateUserAttribute(userId, "profilePic", s3Key);
        log.info("🔍 PROFIL_UPLOAD: Keycloak attribute update completed successfully");
      } catch (Exception keycloakEx) {
        log.error("🔍 PROFIL_UPLOAD: Failed to update Keycloak attribute for user: {}", userId,
            keycloakEx);
        // Pokračujeme i přes chybu v Keycloak - S3 upload už proběhl
      }

      // Vygeneruj URL pro response
      String imageUrl = s3StorageService.getFileUrl(s3Key);
      log.info("🔍 PROFIL_UPLOAD: Generated image URL: {}", imageUrl);

      return ResponseEntity.ok(Map.of("message", "Profilový obrázek byl úspěšně nahrán", "fileName",
          s3Key, "imageUrl", imageUrl));

    } catch (IllegalArgumentException e) {
      log.warn("Invalid file upload request for user {}: {}", username, e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Failed to upload profile picture for user: " + username, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Nepodařilo se nahrát profilový obrázek"));
    }
  }

  /**
   * Smazání profilového obrázku pro aktuálního uživatele
   */
  @DeleteMapping("/profile-picture") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, String>> deleteProfilePicture(Authentication authentication) {

    String username = getCurrentUsername(authentication);
    log.info("Deleting profile picture for user: {}", username);

    try {
      // Najdi uživatele podle username
      UserDto user = keycloakAdminService.getUserByUsername(username);
      String userId = user.getId();

      String profilePicture = keycloakAdminService.getUserAttribute(userId, "profilePicture");
      if (profilePicture == null || profilePicture.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Uživatel nemá profilový obrázek"));
      }

      // Smaž soubor z S3 - používáme verzovanou metodu
      s3StorageService.deleteProfilePicture(userId, profilePicture);
      log.info("Deleted profile picture: {} for user: {}", profilePicture, username);

      // Odstraň z Keycloak attributes
      keycloakAdminService.removeUserAttribute(userId, "profilePicture");

      return ResponseEntity.ok(Map.of("message", "Profilový obrázek byl úspěšně smazán"));

    } catch (Exception e) {
      log.error("Failed to delete profile picture for user: " + username, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "Nepodařilo se smazat profilový obrázek"));
    }
  }

  private String getCurrentUsername(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return jwt.getClaimAsString("preferred_username");
  }
}
