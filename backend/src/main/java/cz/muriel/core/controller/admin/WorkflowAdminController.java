package cz.muriel.core.controller.admin;

import cz.muriel.core.service.workflow.DraftService;
import cz.muriel.core.service.workflow.ProposalService;
import cz.muriel.core.service.workflow.WorkflowSimulator;
import cz.muriel.core.service.workflow.WorkflowValidator;
import cz.muriel.core.workflow.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * W7: Workflow Admin API (Full Metamodel persistence + Execution Engine)
 * 
 * Phase W0: Health endpoint Phase W1: Draft CRUD (in-memory) + basic validation
 * Phase W2: Enhanced validation + Simulation Phase W3: Proposals & Approvals +
 * Version history Phase W4: Metamodel persistence (Proposals + Versions) Phase
 * W5: Metamodel persistence (Drafts) - All data now in DB Phase W6: WebSocket
 * real-time collaboration Phase W7: Workflow execution engine
 */
@Slf4j @RestController @RequestMapping("/api/admin/workflows") @RequiredArgsConstructor @PreAuthorize("hasAuthority('CORE_ADMIN_WORKFLOW')")
public class WorkflowAdminController {

  // W7: Services (all using metamodel persistence)
  private final DraftService draftService;
  private final WorkflowValidator validator;
  private final WorkflowSimulator simulator;
  private final ProposalService proposalService;
  private final WorkflowExecutionService executionService;

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🔄 Workflow Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "W7", "message",
        "Workflow Admin API ready - full metamodel persistence + execution engine"));
  }

  /**
   * W5: Get draft workflow for entity (metamodel persistence)
   */
  @GetMapping("/{entity}/draft")
  public ResponseEntity<Map<String, Object>> getDraft(@PathVariable String entity,
      Authentication auth) {
    log.info("📂 Loading draft workflow for entity: {}", entity);

    Optional<Map<String, Object>> draft = draftService.getDraft(entity, auth);

    if (draft.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(draft.get());
  }

  /**
   * W5: Save draft workflow for entity (metamodel persistence)
   */
  @PutMapping("/{entity}/draft")
  public ResponseEntity<Map<String, Object>> saveDraft(@PathVariable String entity,
      @RequestBody Map<String, Object> draftData, Authentication auth) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) draftData.getOrDefault("nodes",
        List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges = (List<Map<String, Object>>) draftData.getOrDefault("edges",
        List.of());

    log.info("💾 Saving draft workflow for entity: {} (nodes: {}, edges: {})", entity, nodes.size(),
        edges.size());

    Map<String, Object> saved = draftService.saveDraft(entity, draftData, auth);

    return ResponseEntity.ok(saved);
  }

  /**
   * W2: Validate draft workflow
   */
  @PostMapping("/{entity}/validate")
  public ResponseEntity<WorkflowValidator.ValidationResult> validateDraft(
      @PathVariable String entity, @RequestBody Map<String, Object> request) {
    log.info("✅ Validating draft workflow for entity: {}", entity);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) request.getOrDefault("nodes",
        List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges = (List<Map<String, Object>>) request.getOrDefault("edges",
        List.of());

    WorkflowValidator.ValidationResult result = validator.validate(parseNodes(nodes),
        parseEdges(edges));

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
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflow.getOrDefault("nodes",
        List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> edges = (List<Map<String, Object>>) workflow.getOrDefault("edges",
        List.of());

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) request.get("data");

    WorkflowSimulator.SimulationResult result = simulator.simulate(parseNodes(nodes),
        parseEdges(edges), data);

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

    Map<String, Object> proposal = proposalService.createProposal(entity, draftData, author,
        description, auth);

    return ResponseEntity.ok(proposal);
  }

  /**
   * W4: List proposals (optionally filtered)
   */
  @GetMapping("/proposals")
  public ResponseEntity<List<Map<String, Object>>> listProposals(
      @RequestParam(required = false) String entity, @RequestParam(required = false) String status,
      Authentication auth) {
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

    Map<String, Object> proposal = proposalService.approveProposal(proposalId, approver, comment,
        auth);

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

    Map<String, Object> proposal = proposalService.rejectProposal(proposalId, reviewer, comment,
        auth);

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

  // ===== W7: Workflow Execution =====

  /**
   * W7: Execute workflow
   */
  @PostMapping("/{entity}/execute")
  public ResponseEntity<WorkflowExecutionService.ExecutionResult> executeWorkflow(
      @PathVariable String entity, @RequestBody Map<String, Object> context, Authentication auth) {
    log.info("🎯 Executing workflow for entity: {}", entity);

    WorkflowExecutionService.ExecutionResult result = executionService.executeWorkflow(entity,
        context, auth);

    if ("ERROR".equals(result.status())) {
      return ResponseEntity.badRequest().body(result);
    }

    return ResponseEntity.ok(result);
  }
}
