package cz.muriel.core.controller;

import cz.muriel.core.dto.UiCapabilitiesDto;
import cz.muriel.core.service.UiCapabilitiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 🎨 UI Capabilities Controller
 * 
 * REST endpoint pro získání UI capabilities (menu, features). Podporuje ETag
 * caching pro optimalizaci: - První request: 200 OK s ETag - Další requesty s
 * If-None-Match: 304 Not Modified (pokud se permVersion nezměnil)
 */
@RestController @RequestMapping("/api/me") @RequiredArgsConstructor @Slf4j
public class UiCapabilitiesController {

  private final UiCapabilitiesService capabilitiesService;

  /**
   * GET /api/me/ui-capabilities
   * 
   * Vrací UI capabilities (menu, features) pro aktuálního uživatele.
   * 
   * Response headers: - ETag: {permVersion} - hash metamodelu pro cache
   * invalidation - Cache-Control: private, max-age=300 - klientský cache 5 min
   * 
   * Request headers: - If-None-Match: {permVersion} - pro 304 Not Modified
   * 
   * @param ifNoneMatch ETag z předchozího requestu
   * @param auth aktuální uživatel
   * @return 200 OK s capabilities nebo 304 Not Modified
   */
  @GetMapping("/ui-capabilities")
  public ResponseEntity<UiCapabilitiesDto> getCapabilities(
      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
      Authentication auth) {
    // Získat aktuální permVersion
    String currentPermVersion = capabilitiesService.getPermVersion();

    // Pokud klient má aktuální verzi, vrátit 304
    if (ifNoneMatch != null && ifNoneMatch.equals("\"" + currentPermVersion + "\"")) {
      log.debug("Permission version unchanged for user {}, returning 304", auth.getName());
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(currentPermVersion).build();
    }

    // Získat capabilities
    UiCapabilitiesDto capabilities = capabilitiesService.getCapabilities(auth);

    log.debug("Returning UI capabilities for user {}: {} menu items, {} features", auth.getName(),
        capabilities.getMenu().size(), capabilities.getFeatures().size());

    // Vrátit 200 s ETag
    return ResponseEntity.ok().eTag(currentPermVersion).cacheControl(CacheControl.noCache()) // Vždy
                                                                                             // validovat
                                                                                             // na
                                                                                             // serveru
        .body(capabilities);
  }
}
