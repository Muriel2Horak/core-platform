package cz.muriel.core.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * W0: Workflow Admin API (skeleton)
 * 
 * Phase W0: Empty endpoints for wiring
 * Phase W1: Draft CRUD
 * Phase W2: Validation & Preview
 * Phase W3: Proposals & Approvals
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/workflows")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CORE_ADMIN_WORKFLOW')")
public class WorkflowAdminController {

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🔄 Workflow Admin API health check");
    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "phase", "W0",
        "message", "Workflow Admin API skeleton ready"
    ));
  }

  // TODO W1: GET /api/admin/workflows/{entity}/draft
  // TODO W1: PUT /api/admin/workflows/{entity}/draft
  // TODO W2: POST /api/admin/workflows/{entity}/validate
  // TODO W2: POST /api/admin/workflows/{entity}/simulate
  // TODO W3: POST /api/admin/workflows/{entity}/proposals
  // TODO W3: POST /api/admin/workflows/proposals/{id}/approve
  // TODO W3: GET /api/admin/workflows/{entity}/versions
}
