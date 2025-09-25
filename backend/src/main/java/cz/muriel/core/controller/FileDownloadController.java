package cz.muriel.core.controller;

import cz.muriel.core.dto.UserDto;
import cz.muriel.core.auth.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.springframework.beans.factory.annotation.Value;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 🛡️ ZABEZPEČENÝ download souborů z MinIO - Ověřuje autentizaci uživatele -
 * Kontroluje oprávnění k souborům - Future-proof pro pokročilé bezpečnostní
 * pravidla
 */
@Slf4j @RestController @RequestMapping("/api/files") @RequiredArgsConstructor
public class FileDownloadController {

  private final S3Client s3Client;
  private final KeycloakAdminService keycloakAdminService;

  @Value("${storage.s3.bucket-name}")
  private String bucketName;

  /**
   * 🛡️ Zabezpečený download souboru - Ověřuje autentizaci - Kontroluje oprávnění
   * k souboru - Podporuje různé typy souborů (avatary, dokumenty, smlouvy)
   */
  @GetMapping("/download") @PreAuthorize("isAuthenticated()")
  public ResponseEntity<?> downloadFile(@RequestParam("key") String key,
      Authentication authentication) {

    String username = getCurrentUsername(authentication);
    log.info("🔒 DOWNLOAD: User {} requesting file: {}", username, key);

    try {
      // URL decode pro bezpečnost
      String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

      // 🛡️ BEZPEČNOSTNÍ KONTROLY
      if (!isUserAuthorizedForFile(decodedKey, username)) {
        log.warn("🚫 SECURITY: User {} unauthorized for file: {}", username, decodedKey);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Nemáte oprávnění k tomuto souboru");
      }

      // Stáhnout soubor z MinIO
      GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName)
          .key(decodedKey).build();

      ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
      GetObjectResponse response = s3Object.response();

      // Určit MIME typ
      String contentType = response.contentType();
      if (contentType == null || contentType.isEmpty()) {
        contentType = determineMimeType(decodedKey);
      }

      // Určit název souboru pro download
      String filename = extractFilename(decodedKey);

      // Hlavičky pro HTTP response
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(contentType));
      headers.setContentLength(response.contentLength());
      headers.setContentDispositionFormData("inline", filename);

      // Cache headers pro obrázky
      if (contentType.startsWith("image/")) {
        headers.setCacheControl("public, max-age=3600");
      }

      log.info("✅ DOWNLOAD: Serving file {} to user {} ({})", decodedKey, username, contentType);

      return ResponseEntity.ok().headers(headers).body(new InputStreamResource(s3Object));

    } catch (NoSuchKeyException e) {
      log.warn("📁 FILE_NOT_FOUND: File not found: {}", key);
      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      log.error("❌ DOWNLOAD_ERROR: Failed to download file: {}", key, e);
      return ResponseEntity.internalServerError().body("Nepodařilo se stáhnout soubor");
    }
  }

  /**
   * 🛡️ BEZPEČNOSTNÍ LOGIKA - kontroluje oprávnění k souboru Future-proof pro
   * pokročilé oprávnění na úrovni entit/záznamů
   */
  private boolean isUserAuthorizedForFile(String key, String username) {
    try {
      // Načteme uživatele
      UserDto user = keycloakAdminService.getUserByUsername(username);
      String userId = user.getId();

      // 📸 PROFILOVÉ OBRÁZKY - vlastník nebo admin
      if (key.startsWith("profile-pictures/")) {
        return isOwnerOrAdmin(key, userId, user);
      }

      // 📄 DOKUMENTY - budoucí implementace
      if (key.startsWith("documents/")) {
        return isAuthorizedForDocument(key, userId, user);
      }

      // 📋 SMLOUVY - budoucí implementace
      if (key.startsWith("contracts/")) {
        return isAuthorizedForContract(key, userId, user);
      }

      // 📊 REPORTY - budoucí implementace
      if (key.startsWith("reports/")) {
        return isAuthorizedForReport(key, userId, user);
      }

      // Defaultně zamítni přístup k neznámým typům souborů
      log.warn("🚫 UNKNOWN_FILE_TYPE: Unknown file type, denying access: {}", key);
      return false;

    } catch (Exception e) {
      log.error("🚫 AUTH_ERROR: Error checking authorization for file: {}", key, e);
      return false;
    }
  }

  /**
   * Kontrola oprávnění pro profilové obrázky
   */
  private boolean isOwnerOrAdmin(String key, String userId, UserDto user) {
    // Profilové obrázky: profile-pictures/{userId}/avatar.ext
    if (key.startsWith("profile-pictures/" + userId + "/")) {
      log.debug("✅ OWNER: User owns profile picture: {}", key);
      return true;
    }

    // Admini můžou přistupovat ke všem profilovým obrázkům
    if (user.getRoles() != null && user.getRoles().contains("CORE_ROLE_USER_MANAGER")) {
      log.debug("✅ ADMIN: Admin access to profile picture: {}", key);
      return true;
    }

    log.debug("🚫 NOT_OWNER: User {} not authorized for profile picture: {}", userId, key);
    return false;
  }

  /**
   * 🚀 FUTURE: Kontrola oprávnění pro dokumenty Zde bude implementována logika
   * pro: - Vlastník dokumentu - Sdílené dokumenty - Oprávnění podle rolí -
   * Oprávnění podle organizační struktury (manager/deputy)
   */
  private boolean isAuthorizedForDocument(String key, String userId, UserDto user) {
    // TODO: Implementovat pokročilou logiku pro dokumenty
    log.info("🚀 FUTURE: Document authorization not implemented yet: {}", key);
    return false;
  }

  /**
   * 🚀 FUTURE: Kontrola oprávnění pro smlouvy
   */
  private boolean isAuthorizedForContract(String key, String userId, UserDto user) {
    // TODO: Implementovat pokročilou logiku pro smlouvy
    log.info("🚀 FUTURE: Contract authorization not implemented yet: {}", key);
    return false;
  }

  /**
   * 🚀 FUTURE: Kontrola oprávnění pro reporty
   */
  private boolean isAuthorizedForReport(String key, String userId, UserDto user) {
    // TODO: Implementovat pokročilou logiku pro reporty
    log.info("🚀 FUTURE: Report authorization not implemented yet: {}", key);
    return false;
  }

  /**
   * Určí MIME typ na základě přípony souboru
   */
  private String determineMimeType(String key) {
    String lowerKey = key.toLowerCase();

    if (lowerKey.endsWith(".png"))
      return "image/png";
    if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg"))
      return "image/jpeg";
    if (lowerKey.endsWith(".gif"))
      return "image/gif";
    if (lowerKey.endsWith(".webp"))
      return "image/webp";
    if (lowerKey.endsWith(".pdf"))
      return "application/pdf";
    if (lowerKey.endsWith(".docx"))
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (lowerKey.endsWith(".xlsx"))
      return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    return "application/octet-stream";
  }

  /**
   * Extrahuje název souboru z cesty
   */
  private String extractFilename(String key) {
    int lastSlash = key.lastIndexOf('/');
    if (lastSlash >= 0 && lastSlash < key.length() - 1) {
      return key.substring(lastSlash + 1);
    }
    return key;
  }

  private String getCurrentUsername(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return jwt.getClaimAsString("preferred_username");
  }
}
