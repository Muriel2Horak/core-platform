package cz.muriel.core.service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * W3: Workflow Proposal Service
 * 
 * Manages workflow change proposals:
 * - Create proposal from draft
 * - Review & approve proposals
 * - Version history tracking
 * - Diff generation (current vs proposed)
 */
@Slf4j
@Service
public class ProposalService {

  // W3: In-memory storage (TODO W4: persist to DB)
  private final Map<Long, WorkflowProposal> proposals = new ConcurrentHashMap<>();
  private final Map<String, List<WorkflowVersion>> versionHistory = new ConcurrentHashMap<>();
  private final AtomicLong proposalIdGenerator = new AtomicLong(1);
  private final AtomicLong versionIdGenerator = new AtomicLong(1);

  /**
   * W3: Create new workflow proposal
   */
  public WorkflowProposal createProposal(
      String entity,
      Map<String, Object> draftData,
      String author,
      String description
  ) {
    log.info("📝 Creating workflow proposal for entity: {} by {}", entity, author);

    Long proposalId = proposalIdGenerator.getAndIncrement();
    WorkflowProposal proposal = new WorkflowProposal(
        proposalId,
        entity,
        draftData,
        author,
        description,
        ProposalStatus.PENDING,
        Instant.now(),
        null,
        null,
        null
    );

    proposals.put(proposalId, proposal);
    log.info("✅ Proposal created: ID={}", proposalId);

    return proposal;
  }

  /**
   * W3: Get proposal by ID
   */
  public Optional<WorkflowProposal> getProposal(Long proposalId) {
    return Optional.ofNullable(proposals.get(proposalId));
  }

  /**
   * W3: List all proposals (optionally filtered by entity/status)
   */
  public List<WorkflowProposal> listProposals(String entity, ProposalStatus status) {
    return proposals.values().stream()
        .filter(p -> entity == null || p.entity.equals(entity))
        .filter(p -> status == null || p.status.equals(status))
        .sorted(Comparator.comparing(WorkflowProposal::createdAt).reversed())
        .collect(Collectors.toList());
  }

  /**
   * W3: Approve proposal and create new version
   */
  public WorkflowProposal approveProposal(Long proposalId, String approver, String comment) {
    log.info("✅ Approving proposal: ID={} by {}", proposalId, approver);

    WorkflowProposal proposal = proposals.get(proposalId);
    if (proposal == null) {
      throw new IllegalArgumentException("Proposal not found: " + proposalId);
    }

    if (proposal.status != ProposalStatus.PENDING) {
      throw new IllegalStateException(
          "Proposal is not pending: " + proposal.status
      );
    }

    // Update proposal status
    proposal.status = ProposalStatus.APPROVED;
    proposal.reviewedAt = Instant.now();
    proposal.reviewedBy = approver;
    proposal.reviewComment = comment;

    // Create new version from approved proposal
    createVersion(proposal);

    log.info("✅ Proposal approved: ID={}, version created", proposalId);
    return proposal;
  }

  /**
   * W3: Reject proposal
   */
  public WorkflowProposal rejectProposal(Long proposalId, String reviewer, String comment) {
    log.info("❌ Rejecting proposal: ID={} by {}", proposalId, reviewer);

    WorkflowProposal proposal = proposals.get(proposalId);
    if (proposal == null) {
      throw new IllegalArgumentException("Proposal not found: " + proposalId);
    }

    if (proposal.status != ProposalStatus.PENDING) {
      throw new IllegalStateException(
          "Proposal is not pending: " + proposal.status
      );
    }

    proposal.status = ProposalStatus.REJECTED;
    proposal.reviewedAt = Instant.now();
    proposal.reviewedBy = reviewer;
    proposal.reviewComment = comment;

    log.info("❌ Proposal rejected: ID={}", proposalId);
    return proposal;
  }

  /**
   * W3: Get version history for entity
   */
  public List<WorkflowVersion> getVersionHistory(String entity) {
    return versionHistory.getOrDefault(entity, List.of());
  }

  /**
   * W3: Get latest active version for entity
   */
  public Optional<WorkflowVersion> getLatestVersion(String entity) {
    List<WorkflowVersion> versions = versionHistory.getOrDefault(entity, List.of());
    return versions.stream()
        .filter(v -> v.status == VersionStatus.ACTIVE)
        .max(Comparator.comparing(WorkflowVersion::createdAt));
  }

  /**
   * W3: Generate diff between two workflow definitions
   */
  public WorkflowDiff generateDiff(
      Map<String, Object> currentData,
      Map<String, Object> proposedData
  ) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentNodes = 
        (List<Map<String, Object>>) currentData.getOrDefault("nodes", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> proposedNodes = 
        (List<Map<String, Object>>) proposedData.getOrDefault("nodes", List.of());

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> currentEdges = 
        (List<Map<String, Object>>) currentData.getOrDefault("edges", List.of());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> proposedEdges = 
        (List<Map<String, Object>>) proposedData.getOrDefault("edges", List.of());

    Set<String> currentNodeIds = extractIds(currentNodes);
    Set<String> proposedNodeIds = extractIds(proposedNodes);

    List<String> addedNodes = proposedNodeIds.stream()
        .filter(id -> !currentNodeIds.contains(id))
        .collect(Collectors.toList());

    List<String> removedNodes = currentNodeIds.stream()
        .filter(id -> !proposedNodeIds.contains(id))
        .collect(Collectors.toList());

    return new WorkflowDiff(
        currentNodes.size(),
        proposedNodes.size(),
        currentEdges.size(),
        proposedEdges.size(),
        addedNodes,
        removedNodes
    );
  }

  private void createVersion(WorkflowProposal proposal) {
    Long versionId = versionIdGenerator.getAndIncrement();
    
    // Mark previous versions as superseded
    List<WorkflowVersion> entityVersions = 
        versionHistory.computeIfAbsent(proposal.entity, k -> new ArrayList<>());
    entityVersions.forEach(v -> v.status = VersionStatus.SUPERSEDED);

    // Create new active version
    WorkflowVersion version = new WorkflowVersion(
        versionId,
        proposal.entity,
        proposal.draftData,
        proposal.proposalId,
        proposal.author,
        Instant.now(),
        VersionStatus.ACTIVE
    );

    entityVersions.add(version);
    log.info("📦 Version created: ID={}, entity={}", versionId, proposal.entity);
  }

  private Set<String> extractIds(List<Map<String, Object>> items) {
    return items.stream()
        .map(item -> (String) item.get("id"))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  // ===== DTOs =====

  public static class WorkflowProposal {
    public Long proposalId;
    public String entity;
    public Map<String, Object> draftData;
    public String author;
    public String description;
    public ProposalStatus status;
    public Instant createdAt;
    public Instant reviewedAt;
    public String reviewedBy;
    public String reviewComment;

    public WorkflowProposal(
        Long proposalId,
        String entity,
        Map<String, Object> draftData,
        String author,
        String description,
        ProposalStatus status,
        Instant createdAt,
        Instant reviewedAt,
        String reviewedBy,
        String reviewComment
    ) {
      this.proposalId = proposalId;
      this.entity = entity;
      this.draftData = draftData;
      this.author = author;
      this.description = description;
      this.status = status;
      this.createdAt = createdAt;
      this.reviewedAt = reviewedAt;
      this.reviewedBy = reviewedBy;
      this.reviewComment = reviewComment;
    }

    public Long proposalId() { return proposalId; }
    public String entity() { return entity; }
    public ProposalStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
  }

  public static class WorkflowVersion {
    public Long versionId;
    public String entity;
    public Map<String, Object> data;
    public Long proposalId;
    public String createdBy;
    public Instant createdAt;
    public VersionStatus status;

    public WorkflowVersion(
        Long versionId,
        String entity,
        Map<String, Object> data,
        Long proposalId,
        String createdBy,
        Instant createdAt,
        VersionStatus status
    ) {
      this.versionId = versionId;
      this.entity = entity;
      this.data = data;
      this.proposalId = proposalId;
      this.createdBy = createdBy;
      this.createdAt = createdAt;
      this.status = status;
    }

    public Instant createdAt() { return createdAt; }
  }

  public record WorkflowDiff(
      int currentNodeCount,
      int proposedNodeCount,
      int currentEdgeCount,
      int proposedEdgeCount,
      List<String> addedNodes,
      List<String> removedNodes
  ) {}

  public enum ProposalStatus {
    PENDING,
    APPROVED,
    REJECTED
  }

  public enum VersionStatus {
    ACTIVE,
    SUPERSEDED
  }
}
