package cz.muriel.core.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.entities.MetamodelCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * W4: Workflow Proposal Service (Metamodel-based persistence)
 * 
 * Manages workflow change proposals using MetamodelCrudService
 */
@Slf4j @Service @RequiredArgsConstructor
public class ProposalService {

  private final MetamodelCrudService crudService;
  private final ObjectMapper objectMapper;

  @Transactional
  public Map<String, Object> createProposal(String entity, Map<String, Object> draftData,
      String author, String description, Authentication auth) {
    log.info("📝 Creating workflow proposal for entity: {} by {}", entity, author);

    Map<String, Object> proposalData = new HashMap<>();
    proposalData.put("entity", entity);
    proposalData.put("draftData", serializeData(draftData));
    proposalData.put("author", author);
    proposalData.put("description", description);
    proposalData.put("status", "PENDING");

    Map<String, Object> created = crudService.create("WorkflowProposal", proposalData, auth);
    log.info("✅ Proposal created: ID={}", created.get("id"));

    return created;
  }

  public Map<String, Object> getProposal(String proposalId, Authentication auth) {
    return crudService.getById("WorkflowProposal", proposalId, auth);
  }

  public List<Map<String, Object>> listProposals(String entity, String status,
      Authentication auth) {
    Map<String, String> filters = new HashMap<>();
    if (entity != null) {
      filters.put("entity", entity);
    }
    if (status != null) {
      filters.put("status", status);
    }

    return crudService.list("WorkflowProposal", filters, "-created_at", 0, 100, auth);
  }

  @Transactional
  public Map<String, Object> approveProposal(String proposalId, String approver, String comment,
      Authentication auth) {
    log.info("✅ Approving proposal: ID={} by {}", proposalId, approver);

    Map<String, Object> proposal = crudService.getById("WorkflowProposal", proposalId, auth);

    if (!"PENDING".equals(proposal.get("status"))) {
      throw new IllegalStateException("Proposal is not pending: " + proposal.get("status"));
    }

    Map<String, Object> updates = new HashMap<>();
    updates.put("status", "APPROVED");
    updates.put("reviewedAt", Instant.now());
    updates.put("reviewedBy", approver);
    updates.put("reviewComment", comment);

    Object versionObj = proposal.get("version");
    long version = versionObj instanceof Number ? ((Number) versionObj).longValue() : 0L;
    Map<String, Object> updated = crudService.update("WorkflowProposal", proposalId, version,
        updates, auth);

    createVersion(proposal, approver, auth);

    log.info("✅ Proposal approved: ID={}, version created", proposalId);
    return updated;
  }

  @Transactional
  public Map<String, Object> rejectProposal(String proposalId, String reviewer, String comment,
      Authentication auth) {
    log.info("❌ Rejecting proposal: ID={} by {}", proposalId, reviewer);

    Map<String, Object> proposal = crudService.getById("WorkflowProposal", proposalId, auth);

    if (!"PENDING".equals(proposal.get("status"))) {
      throw new IllegalStateException("Proposal is not pending: " + proposal.get("status"));
    }

    Map<String, Object> updates = new HashMap<>();
    updates.put("status", "REJECTED");
    updates.put("reviewedAt", Instant.now());
    updates.put("reviewedBy", reviewer);
    updates.put("reviewComment", comment);

    Object versionObj = proposal.get("version");
    long version = versionObj instanceof Number ? ((Number) versionObj).longValue() : 0L;
    Map<String, Object> updated = crudService.update("WorkflowProposal", proposalId, version,
        updates, auth);

    log.info("❌ Proposal rejected: ID={}", proposalId);
    return updated;
  }

  public List<Map<String, Object>> getVersionHistory(String entity, Authentication auth) {
    Map<String, String> filters = Map.of("entity", entity);
    return crudService.list("WorkflowVersion", filters, "-created_at", 0, 100, auth);
  }

  public Optional<Map<String, Object>> getLatestVersion(String entity, Authentication auth) {
    Map<String, String> filters = new HashMap<>();
    filters.put("entity", entity);
    filters.put("status", "ACTIVE");

    List<Map<String, Object>> versions = crudService.list("WorkflowVersion", filters, "-created_at",
        0, 1, auth);

    return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
  }

  @SuppressWarnings("unchecked")
  public WorkflowDiff generateDiff(Map<String, Object> currentData,
      Map<String, Object> proposedData) {
    List<Map<String, Object>> currentNodes = (List<Map<String, Object>>) currentData
        .getOrDefault("nodes", List.of());
    List<Map<String, Object>> proposedNodes = (List<Map<String, Object>>) proposedData
        .getOrDefault("nodes", List.of());

    List<Map<String, Object>> currentEdges = (List<Map<String, Object>>) currentData
        .getOrDefault("edges", List.of());
    List<Map<String, Object>> proposedEdges = (List<Map<String, Object>>) proposedData
        .getOrDefault("edges", List.of());

    Set<String> currentNodeIds = currentNodes.stream().map(n -> (String) n.get("id"))
        .collect(Collectors.toSet());
    Set<String> proposedNodeIds = proposedNodes.stream().map(n -> (String) n.get("id"))
        .collect(Collectors.toSet());

    List<Map<String, Object>> addedNodes = proposedNodes.stream()
        .filter(n -> !currentNodeIds.contains(n.get("id"))).collect(Collectors.toList());

    List<Map<String, Object>> removedNodes = currentNodes.stream()
        .filter(n -> !proposedNodeIds.contains(n.get("id"))).collect(Collectors.toList());

    Set<String> currentEdgeIds = currentEdges.stream().map(e -> (String) e.get("id"))
        .collect(Collectors.toSet());
    Set<String> proposedEdgeIds = proposedEdges.stream().map(e -> (String) e.get("id"))
        .collect(Collectors.toSet());

    List<Map<String, Object>> addedEdges = proposedEdges.stream()
        .filter(e -> !currentEdgeIds.contains(e.get("id"))).collect(Collectors.toList());

    List<Map<String, Object>> removedEdges = currentEdges.stream()
        .filter(e -> !proposedEdgeIds.contains(e.get("id"))).collect(Collectors.toList());

    return new WorkflowDiff(addedNodes, removedNodes, addedEdges, removedEdges);
  }

  @Transactional
  protected void createVersion(Map<String, Object> proposal, String createdBy,
      Authentication auth) {
    String entity = (String) proposal.get("entity");
    String proposalId = (String) proposal.get("id");
    String draftData = (String) proposal.get("draftData");

    log.info("📦 Creating version for entity: {} from proposal: {}", entity, proposalId);

    supersedePreviousVersions(entity, auth);

    Map<String, Object> versionData = new HashMap<>();
    versionData.put("entity", entity);
    versionData.put("data", draftData);
    versionData.put("proposalId", proposalId);
    versionData.put("createdBy", createdBy);
    versionData.put("status", "ACTIVE");

    crudService.create("WorkflowVersion", versionData, auth);
    log.info("✅ Version created for entity: {}", entity);
  }

  @Transactional
  protected void supersedePreviousVersions(String entity, Authentication auth) {
    Map<String, String> filters = new HashMap<>();
    filters.put("entity", entity);
    filters.put("status", "ACTIVE");

    List<Map<String, Object>> activeVersions = crudService.list("WorkflowVersion", filters, null, 0,
        100, auth);

    for (Map<String, Object> version : activeVersions) {
      String versionId = (String) version.get("id");
      Object versionObj = version.get("version");
      long versionNum = versionObj instanceof Number ? ((Number) versionObj).longValue() : 0L;

      Map<String, Object> updates = Map.of("status", "SUPERSEDED");
      crudService.update("WorkflowVersion", versionId, versionNum, updates, auth);

      log.info("📦 Marked version {} as SUPERSEDED", versionId);
    }
  }

  private String serializeData(Map<String, Object> data) {
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize workflow data", e);
    }
  }

  public record WorkflowDiff(List<Map<String, Object>> addedNodes,
      List<Map<String, Object>> removedNodes, List<Map<String, Object>> addedEdges,
      List<Map<String, Object>> removedEdges) {
  }
}
