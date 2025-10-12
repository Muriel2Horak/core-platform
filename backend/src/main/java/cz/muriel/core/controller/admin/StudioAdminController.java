package cz.muriel.core.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * S10/W0: Metamodel Studio Admin API (skeleton)
 * 
 * Phase W0: Empty endpoints for wiring Phase S10: Wire to existing
 * MetamodelAdminController services
 * 
 * Note: MetamodelAdminController already has: - GET /api/admin/metamodel/reload
 * (hot reload + diff) - POST /api/admin/metamodel/apply-safe-changes - GET
 * /api/admin/metamodel/status
 * 
 * This controller adds: - Studio GUI lifecycle
 * (validate/preview/propose/approve) - UI-spec metadata editing
 */
@Slf4j @RestController @RequestMapping("/api/admin/studio") @RequiredArgsConstructor @PreAuthorize("hasAuthority('CORE_ADMIN_STUDIO')")
public class StudioAdminController {

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🎨 Studio Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "W0", "message",
        "Studio Admin API skeleton ready - will wire to existing metamodel services"));
  }

  // TODO S10: GET /api/admin/studio/entities
  // TODO S10: GET /api/admin/studio/entities/{entity}
  // TODO S10: PUT /api/admin/studio/entities/{entity}
  // TODO S10: POST /api/admin/studio/validate
  // TODO S10: POST /api/admin/studio/preview
  // TODO S10: POST /api/admin/studio/proposals
  // TODO S10: POST /api/admin/studio/proposals/{id}/approve
}
