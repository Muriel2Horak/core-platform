package cz.muriel.core.controller.admin;

import cz.muriel.core.service.workflow.ProposalService;
import cz.muriel.core.service.workflow.WorkflowSimulator;
import cz.muriel.core.service.workflow.WorkflowValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * W3: Workflow Admin API
 * 
 * Phase W0: Health endpoint
 * Phase W1: Draft CRUD (in-memory) + basic validation
 * Phase W2: Enhanced validation + Simulation
 * Phase W3: Proposals & Approvals + Version history
 */
@Slf4j @RestController @RequestMapping("/api/admin/workflows") @RequiredArgsConstructor @PreAuthorize("hasAuthority('CORE_ADMIN_WORKFLOW')")
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

  // W1: In-memory draft storage (TODO W4: persist to DB)
  private final Map<String, WorkflowDraft> draftStore = new ConcurrentHashMap<>();

  // W2-W3: Services
  private final WorkflowValidator validator;
  private final WorkflowSimulator simulator;
  private final ProposalService proposalService;

  /**
   * W0: Health check
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    log.info("🔄 Workflow Admin API health check");
    return ResponseEntity.ok(Map.of("status", "ok", "phase", "W3", "message",
        "Workflow Admin API ready - proposals + approvals available"));
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
  public ResponseEntity<Map<String, Object>> saveDraft(@PathVariable String entity,
      @RequestBody Map<String, Object> draftData) {
    log.info("💾 Saving draft workflow for entity: {} (nodes: {}, edges: {})", entity,
        ((Map<?, ?>) draftData.getOrDefault("nodes", Map.of())).size(),
        ((Map<?, ?>) draftData.getOrDefault("edges", Map.of())).size());

    WorkflowDraft draft = new WorkflowDraft(entity, draftData);
    draft.updatedAt = Instant.now();
    draftStore.put(entity, draft);

    return ResponseEntity
        .ok(Map.of("status", "saved", "entity", entity, "updatedAt", draft.updatedAt));
  }

  /**
   * W2: Validate draft workflow (enhanced)
   */
  @PostMapping("/{entity}/validate")
  public ResponseEntity<Map<String, Object>> validateDraft(@PathVariable String entity,
      @RequestBody Map<String, Object> draftData) {
    log.info("✅ Validating draft workflow for entity: {}", entity);

    // Parse nodes and edges
    List<WorkflowValidator.WorkflowNode> nodes = parseNodes(draftData);
    List<WorkflowValidator.WorkflowEdge> edges = parseEdges(draftData);

    // W2: Enhanced validation
    WorkflowValidator.ValidationResult result = validator.validate(nodes, edges);

    return ResponseEntity.ok(Map.of("valid", result.valid(), "entity", entity, "errors",
        result.errors(), "warnings", result.warnings()));
  }

  /**
   * W2: Simulate workflow execution
   */
  @PostMapping("/{entity}/simulate")
  public ResponseEntity<Map<String, Object>> simulateWorkflow(@PathVariable String entity,
      @RequestBody Map<String, Object> request) {
    log.info("🎬 Simulating workflow for entity: {}", entity);

    @SuppressWarnings("unchecked")
    Map<String, Object> draftData = (Map<String, Object>) request.get("workflow");
    @SuppressWarnings("unchecked")
    Map<String, Object> startData = (Map<String, Object>) request.getOrDefault("data", Map.of());

    // Parse nodes and edges
    List<WorkflowValidator.WorkflowNode> nodes = parseNodes(draftData);
    List<WorkflowValidator.WorkflowEdge> edges = parseEdges(draftData);

    // Run simulation
    WorkflowSimulator.SimulationResult result = simulator.simulate(nodes, edges, startData);

    return ResponseEntity.ok(Map.of("success", result.success(), "entity", entity, "trace",
        result.trace(), "message", result.message()));
  }

  // ===== Helper Methods =====

  @SuppressWarnings("unchecked")
  private List<WorkflowValidator.WorkflowNode> parseNodes(Map<String, Object> draftData) {
    List<Map<String, Object>> nodesData = (List<Map<String, Object>>) draftData
        .getOrDefault("nodes", List.of());
    List<WorkflowValidator.WorkflowNode> nodes = new ArrayList<>();

    for (Map<String, Object> nodeData : nodesData) {
      WorkflowValidator.WorkflowNode node = new WorkflowValidator.WorkflowNode();
      node.setId((String) nodeData.get("id"));
      node.setType((String) ((Map<String, Object>) nodeData.getOrDefault("data", Map.of()))
          .get("stateType"));
      node.setLabel(
          (String) ((Map<String, Object>) nodeData.getOrDefault("data", Map.of())).get("label"));

      @SuppressWarnings("unchecked")
      List<String> branches = (List<String>) ((Map<String, Object>) nodeData.getOrDefault("data",
          Map.of())).get("branches");
      node.setBranches(branches);

      nodes.add(node);
    }

    return nodes;
  }

  @SuppressWarnings("unchecked")
  private List<WorkflowValidator.WorkflowEdge> parseEdges(Map<String, Object> draftData) {
    List<Map<String, Object>> edgesData = (List<Map<String, Object>>) draftData
        .getOrDefault("edges", List.of());
    List<WorkflowValidator.WorkflowEdge> edges = new ArrayList<>();

    for (Map<String, Object> edgeData : edgesData) {
      WorkflowValidator.WorkflowEdge edge = new WorkflowValidator.WorkflowEdge();
      edge.setSource((String) edgeData.get("source"));
      edge.setTarget((String) edgeData.get("target"));
      edges.add(edge);
    }

    return edges;
  }

  // ===== W3: Proposals & Approvals =====

  /**
   * W3: Create workflow proposal
   */
  @PostMapping("/{entity}/proposals")
  public ResponseEntity<Map<String, Object>> createProposal(
      @PathVariable String entity,
      @RequestBody Map<String, Object> request
  ) {
    log.info("📝 Creating proposal for entity: {}", entity);

    @SuppressWarnings("unchecked")
    Map<String, Object> draftData = (Map<String, Object>) request.get("draftData");
    String author = (String) request.getOrDefault("author", "system");
    String description = (String) request.getOrDefault("description", "Workflow update");

    ProposalService.WorkflowProposal proposal = 
        proposalService.createProposal(entity, draftData, author, description);

    return ResponseEntity.ok(Map.of(
        "proposalId", proposal.proposalId,
        "entity", proposal.entity,
        "status", proposal.status.toString(),
        "author", proposal.author,
        "createdAt", proposal.createdAt
    ));
  }

  /**
   * W3: List proposals (optionally filtered)
   */
  @GetMapping("/proposals")
  public ResponseEntity<List<Map<String, Object>>> listProposals(
      @RequestParam(required = false) String entity,
      @RequestParam(required = false) String status
  ) {
    log.info("📋 Listing proposals: entity={}, status={}", entity, status);

    ProposalService.ProposalStatus statusEnum = 
        status != null ? ProposalService.ProposalStatus.valueOf(status.toUpperCase()) : null;

    List<ProposalService.WorkflowProposal> proposals = 
        proposalService.listProposals(entity, statusEnum);

    List<Map<String, Object>> result = proposals.stream()
        .map(p -> Map.<String, Object>of(
            "proposalId", p.proposalId,
            "entity", p.entity,
            "status", p.status.toString(),
            "author", p.author,
            "description", p.description,
            "createdAt", p.createdAt,
            "reviewedAt", p.reviewedAt != null ? p.reviewedAt : "",
            "reviewedBy", p.reviewedBy != null ? p.reviewedBy : ""
        ))
        .collect(Collectors.toList());

    return ResponseEntity.ok(result);
  }

  /**
   * W3: Approve proposal
   */
  @PostMapping("/proposals/{proposalId}/approve")
  public ResponseEntity<Map<String, Object>> approveProposal(
      @PathVariable Long proposalId,
      @RequestBody Map<String, Object> request
  ) {
    log.info("✅ Approving proposal: {}", proposalId);

    String approver = (String) request.getOrDefault("approver", "system");
    String comment = (String) request.getOrDefault("comment", "Approved");

    ProposalService.WorkflowProposal proposal = 
        proposalService.approveProposal(proposalId, approver, comment);

    return ResponseEntity.ok(Map.of(
        "proposalId", proposal.proposalId,
        "status", proposal.status.toString(),
        "reviewedBy", proposal.reviewedBy,
        "reviewedAt", proposal.reviewedAt,
        "message", "Proposal approved and version created"
    ));
  }

  /**
   * W3: Reject proposal
   */
  @PostMapping("/proposals/{proposalId}/reject")
  public ResponseEntity<Map<String, Object>> rejectProposal(
      @PathVariable Long proposalId,
      @RequestBody Map<String, Object> request
  ) {
    log.info("❌ Rejecting proposal: {}", proposalId);

    String reviewer = (String) request.getOrDefault("reviewer", "system");
    String comment = (String) request.getOrDefault("comment", "Rejected");

    ProposalService.WorkflowProposal proposal = 
        proposalService.rejectProposal(proposalId, reviewer, comment);

    return ResponseEntity.ok(Map.of(
        "proposalId", proposal.proposalId,
        "status", proposal.status.toString(),
        "reviewedBy", proposal.reviewedBy,
        "reviewedAt", proposal.reviewedAt,
        "message", "Proposal rejected"
    ));
  }

  /**
   * W3: Get version history for entity
   */
  @GetMapping("/{entity}/versions")
  public ResponseEntity<List<Map<String, Object>>> getVersionHistory(
      @PathVariable String entity
  ) {
    log.info("📦 Getting version history for entity: {}", entity);

    List<ProposalService.WorkflowVersion> versions = 
        proposalService.getVersionHistory(entity);

    List<Map<String, Object>> result = versions.stream()
        .map(v -> Map.<String, Object>of(
            "versionId", v.versionId,
            "entity", v.entity,
            "proposalId", v.proposalId,
            "createdBy", v.createdBy,
            "createdAt", v.createdAt,
            "status", v.status.toString()
        ))
        .collect(Collectors.toList());

    return ResponseEntity.ok(result);
  }

  /**
   * W3: Get proposal diff
   */
  @GetMapping("/proposals/{proposalId}/diff")
  public ResponseEntity<ProposalService.WorkflowDiff> getProposalDiff(
      @PathVariable Long proposalId
  ) {
    log.info("🔍 Getting diff for proposal: {}", proposalId);

    Optional<ProposalService.WorkflowProposal> proposalOpt = 
        proposalService.getProposal(proposalId);

    if (proposalOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    ProposalService.WorkflowProposal proposal = proposalOpt.get();
    
    // Get current version
    Optional<ProposalService.WorkflowVersion> currentVersion = 
        proposalService.getLatestVersion(proposal.entity);

    Map<String, Object> currentData = currentVersion
        .map(v -> v.data)
        .orElse(Map.of("nodes", List.of(), "edges", List.of()));

    ProposalService.WorkflowDiff diff = 
        proposalService.generateDiff(currentData, proposal.draftData);

    return ResponseEntity.ok(diff);
  }
}
