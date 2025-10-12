package cz.muriel.core.controller.admin;

import cz.muriel.core.service.workflow.ProposalService;
import cz.muriel.core.service.workflow.WorkflowSimulator;
import cz.muriel.core.service.workflow.WorkflowValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * W4: Workflow Admin API (Metamodel-based persistence)
 * 
 * Phase W0: Health endpoint
 * Phase W1: Draft CRUD (in-memory) + basic validation
 * Phase W2: Enhanced validation + Simulation
 * Phase W3: Proposals & Approvals + Version history
 * Phase W4: Metamodel persistence (WorkflowDraft, WorkflowProposal, WorkflowVersion)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/workflows")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CORE_ADMIN_WORKFLOW')")
public class WorkflowAdminController {

  /**
   * W1: Internal DTO for draft storage (TODO W5: migrate to Workflow Draft entity)
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

    public String getEntity() {
      return entity;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public Instant getUpdatedAt() {
      return updatedAt;
    }
  }

  // W1: In-memory draft storage (TODO W5: persist to WorkflowDraft entity)
  private final Map<String, WorkflowDraft> draftStore = new ConcurrentHashMap<>();

  // W2-W4: Services
  private final WorkflowValidator validator;
  private final WorkflowSimulator simulator;
  private final ProposalService proposalService;

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🔄 Workflow Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "W4", "message",
        "Workflow Admin API ready - metamodel persistence active"));
  }

  /**
   * W1: Get draft workflow for entity
   */
  @GetMapping("/{entity}/draft")
  public ResponseEntity<WorkflowDraft> getDraft(@PathVariable String entity) {
    log.info("📂 Loading draft workflow for entity: {}", entity);

    WorkflowDraft draft = draftStore.get(entity);
    if (draft == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(draft);
  }

  /**
   * W1: Save draft workflow for entity
   */
  @PutMapping("/{entity}/draft")
  public ResponseEntity<Map<String, String>> saveDraft(@PathVariable String entity,
      @RequestBody Map<String, Object> draftData) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodes =
        (List<Map<String, Object>>) draftData.getOrDefault("nodes", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges =
        (List<Map<String, Object>>) draftData.getOrDefault("edges", List.of());

    log.info("💾 Saving draft workflow for entity: {} (nodes: {}, edges: {})", entity,
        nodes.size(), edges.size());

    WorkflowDraft draft = new WorkflowDraft(entity, draftData);
    draftStore.put(entity, draft);

    return ResponseEntity.ok(Map.of("message", "Draft saved successfully", "entity", entity));
  }

  /**
   * W2: Validate draft workflow
   */
  @PostMapping("/{entity}/validate")
  public ResponseEntity<WorkflowValidator.ValidationResult> validateDraft(
      @PathVariable String entity, @RequestBody Map<String, Object> request) {
    log.info("✅ Validating draft workflow for entity: {}", entity);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodes =
        (List<Map<String, Object>>) request.getOrDefault("nodes", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges =
        (List<Map<String, Object>>) request.getOrDefault("edges", List.of());

    WorkflowValidator.ValidationResult result =
        validator.validate(parseNodes(nodes), parseEdges(edges));

    return ResponseEntity.ok(result);
  }

  /**
   * W2: Simulate draft workflow execution
   */
  @PostMapping("/{entity}/simulate")
  public ResponseEntity<WorkflowSimulator.SimulationResult> simulateDraft(
      @PathVariable String entity, @RequestBody Map<String, Object> request) {
    log.info("🎬 Simulating workflow for entity: {}", entity);

    @SuppressWarnings("unchecked")
    Map<String, Object> workflow = (Map<String, Object>) request.get("workflow");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodes =
        (List<Map<String, Object>>) workflow.getOrDefault("nodes", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges =
        (List<Map<String, Object>>) workflow.getOrDefault("edges", List.of());

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) request.get("data");

    WorkflowSimulator.SimulationResult result =
        simulator.simulate(parseNodes(nodes), parseEdges(edges), data);

    return ResponseEntity.ok(result);
  }

  // ===== W2: Helper methods =====

  @SuppressWarnings("unchecked")
  private List<WorkflowValidator.WorkflowNode> parseNodes(List<Map<String, Object>> nodes) {
    return nodes.stream().map(n -> {
      WorkflowValidator.WorkflowNode node = new WorkflowValidator.WorkflowNode();
      node.setId((String) n.get("id"));
      node.setType((String) n.get("type"));
      
      Map<String, Object> data = (Map<String, Object>) n.get("data");
      if (data != null) {
        node.setLabel((String) data.get("label"));
        node.setBranches((List<String>) data.get("branches"));
      }
      
      return node;
    }).toList();
  }

  private List<WorkflowValidator.WorkflowEdge> parseEdges(List<Map<String, Object>> edges) {
    return edges.stream().map(e -> {
      WorkflowValidator.WorkflowEdge edge = new WorkflowValidator.WorkflowEdge();
      edge.setSource((String) e.get("source"));
      edge.setTarget((String) e.get("target"));
      return edge;
    }).toList();
  }

  // ===== W4: Proposals & Approvals (Metamodel-based) =====

  /**
   * W4: Create workflow proposal
   */
  @PostMapping("/{entity}/proposals")
  public ResponseEntity<Map<String, Object>> createProposal(@PathVariable String entity,
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("📝 Creating proposal for entity: {}", entity);

    @SuppressWarnings("unchecked")
    Map<String, Object> draftData = (Map<String, Object>) request.get("draftData");
    String author = (String) request.getOrDefault("author", "system");
    String description = (String) request.getOrDefault("description", "Workflow update");

    Map<String, Object> proposal =
        proposalService.createProposal(entity, draftData, author, description, auth);

    return ResponseEntity.ok(proposal);
  }

  /**
   * W4: List proposals (optionally filtered)
   */
  @GetMapping("/proposals")
  public ResponseEntity<List<Map<String, Object>>> listProposals(
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) String status, Authentication auth) {
    log.info("📋 Listing proposals: entity={}, status={}", entity, status);

    List<Map<String, Object>> proposals = proposalService.listProposals(entity, status, auth);

    return ResponseEntity.ok(proposals);
  }

  /**
   * W4: Approve proposal
   */
  @PostMapping("/proposals/{proposalId}/approve")
  public ResponseEntity<Map<String, Object>> approveProposal(@PathVariable String proposalId,
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("✅ Approving proposal: {}", proposalId);

    String approver = (String) request.getOrDefault("approver", "system");
    String comment = (String) request.getOrDefault("comment", "Approved");

    Map<String, Object> proposal =
        proposalService.approveProposal(proposalId, approver, comment, auth);

    return ResponseEntity.ok(proposal);
  }

  /**
   * W4: Reject proposal
   */
  @PostMapping("/proposals/{proposalId}/reject")
  public ResponseEntity<Map<String, Object>> rejectProposal(@PathVariable String proposalId,
      @RequestBody Map<String, Object> request, Authentication auth) {
    log.info("❌ Rejecting proposal: {}", proposalId);

    String reviewer = (String) request.getOrDefault("reviewer", "system");
    String comment = (String) request.getOrDefault("comment", "Rejected");

    Map<String, Object> proposal =
        proposalService.rejectProposal(proposalId, reviewer, comment, auth);

    return ResponseEntity.ok(proposal);
  }

  /**
   * W4: Get version history for entity
   */
  @GetMapping("/{entity}/versions")
  public ResponseEntity<List<Map<String, Object>>> getVersionHistory(@PathVariable String entity,
      Authentication auth) {
    log.info("📦 Getting version history for entity: {}", entity);

    List<Map<String, Object>> versions = proposalService.getVersionHistory(entity, auth);

    return ResponseEntity.ok(versions);
  }

  /**
   * W4: Get proposal diff
   */
  @GetMapping("/proposals/{proposalId}/diff")
  public ResponseEntity<ProposalService.WorkflowDiff> getProposalDiff(
      @PathVariable String proposalId, Authentication auth) {
    log.info("🔍 Getting diff for proposal: {}", proposalId);

    Map<String, Object> proposal = proposalService.getProposal(proposalId, auth);
    if (proposal == null) {
      return ResponseEntity.notFound().build();
    }

    String entity = (String) proposal.get("entity");

    // Get current version
    Optional<Map<String, Object>> currentVersion = proposalService.getLatestVersion(entity, auth);

    Map<String, Object> currentData = currentVersion.map(v -> (String) v.get("data"))
        .map(this::parseJsonToMap).orElse(Map.of("nodes", List.of(), "edges", List.of()));

    String draftDataJson = (String) proposal.get("draftData");
    Map<String, Object> proposedData = parseJsonToMap(draftDataJson);

    ProposalService.WorkflowDiff diff = proposalService.generateDiff(currentData, proposedData);

    return ResponseEntity.ok(diff);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJsonToMap(String json) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
    } catch (Exception e) {
      log.error("Failed to parse JSON", e);
      return Map.of("nodes", List.of(), "edges", List.of());
    }
  }
}
