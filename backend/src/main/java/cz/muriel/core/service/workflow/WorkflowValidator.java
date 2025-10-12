package cz.muriel.core.service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * W2: Workflow Validation Service
 * 
 * Enhanced validation: - Reachability check (všechny stavy dosažitelné?) -
 * Cycle detection (cyklické přechody) - Dead-end detection (stavy bez výstupu,
 * kromě END) - Transition completeness (všechny větve pokryté)
 */
@Slf4j @Service
public class WorkflowValidator {

  /**
   * W2: Comprehensive workflow validation
   */
  public ValidationResult validate(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
    log.info("🔍 Validating workflow: {} nodes, {} edges", nodes.size(), edges.size());

    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();

    // 1. Basic structure validation
    validateStructure(nodes, edges, errors);

    // 2. Reachability check
    validateReachability(nodes, edges, errors, warnings);

    // 3. Cycle detection
    validateCycles(nodes, edges, warnings);

    // 4. Dead-end detection
    validateDeadEnds(nodes, edges, errors);

    // 5. Transition completeness
    validateTransitionCompleteness(nodes, edges, warnings);

    boolean isValid = errors.isEmpty();
    log.info("✅ Validation complete: valid={}, errors={}, warnings={}", isValid, errors.size(),
        warnings.size());

    return new ValidationResult(isValid, errors, warnings);
  }

  private void validateStructure(List<WorkflowNode> nodes, List<WorkflowEdge> edges,
      List<ValidationError> errors) {
    // Must have at least one START node
    long startNodes = nodes.stream().filter(n -> "START".equalsIgnoreCase(n.getType())).count();
    if (startNodes == 0) {
      errors
          .add(new ValidationError("MISSING_START", "Workflow must have at least one START node"));
    }
    if (startNodes > 1) {
      errors.add(new ValidationError("MULTIPLE_STARTS", "Workflow can have only one START node"));
    }

    // Must have at least one END node
    long endNodes = nodes.stream().filter(n -> "END".equalsIgnoreCase(n.getType())).count();
    if (endNodes == 0) {
      errors.add(new ValidationError("MISSING_END", "Workflow must have at least one END node"));
    }

    // All edges must reference existing nodes
    Set<String> nodeIds = nodes.stream().map(WorkflowNode::getId).collect(Collectors.toSet());
    for (WorkflowEdge edge : edges) {
      if (!nodeIds.contains(edge.getSource())) {
        errors.add(new ValidationError("INVALID_EDGE_SOURCE",
            "Edge references non-existent source node: " + edge.getSource()));
      }
      if (!nodeIds.contains(edge.getTarget())) {
        errors.add(new ValidationError("INVALID_EDGE_TARGET",
            "Edge references non-existent target node: " + edge.getTarget()));
      }
    }
  }

  private void validateReachability(List<WorkflowNode> nodes, List<WorkflowEdge> edges,
      List<ValidationError> errors, List<ValidationWarning> warnings) {
    // Find START nodes
    List<String> startNodeIds = nodes.stream().filter(n -> "START".equalsIgnoreCase(n.getType()))
        .map(WorkflowNode::getId).collect(Collectors.toList());

    if (startNodeIds.isEmpty()) {
      return; // Already reported in structure validation
    }

    // Build adjacency list
    Map<String, List<String>> adjacency = buildAdjacencyList(edges);

    // BFS to find reachable nodes
    Set<String> reachable = new HashSet<>();
    Queue<String> queue = new LinkedList<>(startNodeIds);
    reachable.addAll(startNodeIds);

    while (!queue.isEmpty()) {
      String current = queue.poll();
      List<String> neighbors = adjacency.getOrDefault(current, List.of());
      for (String neighbor : neighbors) {
        if (!reachable.contains(neighbor)) {
          reachable.add(neighbor);
          queue.add(neighbor);
        }
      }
    }

    // Check unreachable nodes
    for (WorkflowNode node : nodes) {
      if (!reachable.contains(node.getId()) && !"START".equalsIgnoreCase(node.getType())) {
        warnings.add(new ValidationWarning("UNREACHABLE_NODE",
            "Node '" + node.getLabel() + "' is not reachable from START"));
      }
    }
  }

  private void validateCycles(List<WorkflowNode> nodes, List<WorkflowEdge> edges,
      List<ValidationWarning> warnings) {
    Map<String, List<String>> adjacency = buildAdjacencyList(edges);
    Set<String> visited = new HashSet<>();
    Set<String> recursionStack = new HashSet<>();

    for (WorkflowNode node : nodes) {
      if (hasCycle(node.getId(), adjacency, visited, recursionStack)) {
        warnings.add(new ValidationWarning("CYCLE_DETECTED",
            "Workflow contains cycles starting from node: " + node.getLabel()));
        break; // Report only first cycle
      }
    }
  }

  private boolean hasCycle(String nodeId, Map<String, List<String>> adjacency, Set<String> visited,
      Set<String> recursionStack) {
    if (recursionStack.contains(nodeId)) {
      return true; // Cycle detected
    }
    if (visited.contains(nodeId)) {
      return false; // Already processed
    }

    visited.add(nodeId);
    recursionStack.add(nodeId);

    List<String> neighbors = adjacency.getOrDefault(nodeId, List.of());
    for (String neighbor : neighbors) {
      if (hasCycle(neighbor, adjacency, visited, recursionStack)) {
        return true;
      }
    }

    recursionStack.remove(nodeId);
    return false;
  }

  private void validateDeadEnds(List<WorkflowNode> nodes, List<WorkflowEdge> edges,
      List<ValidationError> errors) {
    Map<String, List<String>> adjacency = buildAdjacencyList(edges);

    for (WorkflowNode node : nodes) {
      // END nodes are allowed to have no outgoing edges
      if ("END".equalsIgnoreCase(node.getType())) {
        continue;
      }

      List<String> outgoing = adjacency.getOrDefault(node.getId(), List.of());
      if (outgoing.isEmpty()) {
        errors.add(new ValidationError("DEAD_END",
            "Node '" + node.getLabel() + "' has no outgoing transitions (not an END node)"));
      }
    }
  }

  private void validateTransitionCompleteness(List<WorkflowNode> nodes, List<WorkflowEdge> edges,
      List<ValidationWarning> warnings) {
    Map<String, List<String>> adjacency = buildAdjacencyList(edges);

    for (WorkflowNode node : nodes) {
      if ("DECISION".equalsIgnoreCase(node.getType())) {
        List<String> branches = node.getBranches() != null ? node.getBranches()
            : List.of("YES", "NO");
        List<String> outgoing = adjacency.getOrDefault(node.getId(), List.of());

        if (outgoing.size() < branches.size()) {
          warnings.add(new ValidationWarning("INCOMPLETE_DECISION",
              "Decision node '" + node.getLabel() + "' has only " + outgoing.size() + " of "
                  + branches.size() + " branches defined"));
        }
      }
    }
  }

  private Map<String, List<String>> buildAdjacencyList(List<WorkflowEdge> edges) {
    Map<String, List<String>> adjacency = new HashMap<>();
    for (WorkflowEdge edge : edges) {
      adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
    }
    return adjacency;
  }

  // ===== DTOs =====

  public record ValidationResult(boolean valid, List<ValidationError> errors,
      List<ValidationWarning> warnings) {
  }

  public record ValidationError(String code, String message) {
  }

  public record ValidationWarning(String code, String message) {
  }

  public static class WorkflowNode {
    private String id;
    private String type;
    private String label;
    private List<String> branches;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public List<String> getBranches() {
      return branches;
    }

    public void setBranches(List<String> branches) {
      this.branches = branches;
    }
  }

  public static class WorkflowEdge {
    private String source;
    private String target;

    public String getSource() {
      return source;
    }

    public void setSource(String source) {
      this.source = source;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }
  }
}
