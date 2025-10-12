package cz.muriel.core.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W1: Workflow Admin API
 * 
 * Phase W0: Health endpoint
 * Phase W1: Draft CRUD (in-memory) + validation
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
   * W1: Internal DTO for draft storage
   */
  private static class WorkflowDraft {
    String entity;
    Map<String, Object> data;
    Instant updatedAt;

    WorkflowDraft(String entity, Map<String, Object> data) {
      this.entity = entity;
      this.data = data;
      this.updatedAt = Instant.now();
    }

    @SuppressWarnings("unused")
    public String getEntity() {
      return entity;
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getData() {
      return data;
    }

    @SuppressWarnings("unused")
    public Instant getUpdatedAt() {
      return updatedAt;
    }
  }

  // W1: In-memory draft storage (TODO W2: persist to DB)
  private final Map<String, WorkflowDraft> draftStore = new ConcurrentHashMap<>();

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🔄 Workflow Admin API health check");
    return ResponseEntity.ok(
        Map.of(
            "status", "ok",
            "phase", "W1",
            "message", "Workflow Admin API ready - draft CRUD available"
        )
    );
  }

  /**
   * W1: Get draft workflow for entity
   */
  @GetMapping("/{entity}/draft")
  public ResponseEntity<WorkflowDraft> getDraft(@PathVariable String entity) {
    log.info("📂 Loading draft workflow for entity: {}", entity);
    
    WorkflowDraft draft = draftStore.get(entity);
    if (draft == null) {
      // Return empty draft
      draft = new WorkflowDraft(entity, new HashMap<>());
    }
    
    return ResponseEntity.ok(draft);
  }

  /**
   * W1: Save draft workflow for entity
   */
  @PutMapping("/{entity}/draft")
  public ResponseEntity<Map<String, Object>> saveDraft(
      @PathVariable String entity,
      @RequestBody Map<String, Object> draftData
  ) {
    log.info("💾 Saving draft workflow for entity: {} (nodes: {}, edges: {})",
        entity,
        ((Map<?, ?>) draftData.getOrDefault("nodes", Map.of())).size(),
        ((Map<?, ?>) draftData.getOrDefault("edges", Map.of())).size()
    );

    WorkflowDraft draft = new WorkflowDraft(entity, draftData);
    draft.updatedAt = Instant.now();
    draftStore.put(entity, draft);

    return ResponseEntity.ok(
        Map.of(
            "status", "saved",
            "entity", entity,
            "updatedAt", draft.updatedAt
        )
    );
  }

  /**
   * W1: Validate draft workflow
   */
  @PostMapping("/{entity}/validate")
  public ResponseEntity<Map<String, Object>> validateDraft(
      @PathVariable String entity,
      @RequestBody Map<String, Object> draftData
  ) {
    log.info("✅ Validating draft workflow for entity: {}", entity);

    // W1: Basic validation (TODO W2: enhance with state machine rules)
    boolean isValid = draftData.containsKey("nodes") && draftData.containsKey("edges");
    
    return ResponseEntity.ok(
        Map.of(
            "valid", isValid,
            "entity", entity,
            "errors", isValid ? Map.of() : Map.of("graph", "Missing nodes or edges")
        )
    );
  }

  // TODO W2: POST /api/admin/workflows/{entity}/simulate
  // TODO W3: POST /api/admin/workflows/{entity}/proposals
  // TODO W3: POST /api/admin/workflows/proposals/{id}/approve
  // TODO W3: GET /api/admin/workflows/{entity}/versions
}
