package cz.muriel.core.service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * W2: Workflow Simulation Service
 * 
 * Simuluje přechody mezi stavy s testovacími daty.
 */
@Slf4j @Service
public class WorkflowSimulator {

  public SimulationResult simulate(List<WorkflowValidator.WorkflowNode> nodes,
      List<WorkflowValidator.WorkflowEdge> edges, Map<String, Object> startData) {
    log.info("Starting workflow simulation");

    Optional<WorkflowValidator.WorkflowNode> startNode = nodes.stream()
        .filter(n -> "START".equalsIgnoreCase(n.getType())).findFirst();

    if (startNode.isEmpty()) {
      return new SimulationResult(false, List.of(), "No START node found");
    }

    Map<String, List<WorkflowValidator.WorkflowEdge>> adjacency = buildAdjacencyMap(edges);

    List<ExecutionStep> trace = new ArrayList<>();
    Set<String> visitedNodes = new HashSet<>();
    String currentNodeId = startNode.get().getId();
    Map<String, Object> currentData = new HashMap<>(startData);
    int maxSteps = 100;
    int stepCount = 0;

    while (stepCount < maxSteps) {
      stepCount++;
      String nodeId = currentNodeId;

      Optional<WorkflowValidator.WorkflowNode> currentNodeOpt = nodes.stream()
          .filter(n -> n.getId().equals(nodeId)).findFirst();

      if (currentNodeOpt.isEmpty()) {
        return new SimulationResult(false, trace, "Invalid node reference: " + nodeId);
      }

      WorkflowValidator.WorkflowNode currentNode = currentNodeOpt.get();
      trace.add(new ExecutionStep(stepCount, currentNode.getId(), currentNode.getType(),
          currentNode.getLabel(), new HashMap<>(currentData)));

      if ("END".equalsIgnoreCase(currentNode.getType())) {
        return new SimulationResult(true, trace, "Workflow completed successfully");
      }

      if (visitedNodes.contains(nodeId)) {
        return new SimulationResult(false, trace,
            "Cycle detected at node: " + currentNode.getLabel());
      }
      visitedNodes.add(nodeId);

      List<WorkflowValidator.WorkflowEdge> outgoing = adjacency.getOrDefault(nodeId, List.of());
      if (outgoing.isEmpty()) {
        return new SimulationResult(false, trace, "Dead-end at node: " + currentNode.getLabel());
      }

      WorkflowValidator.WorkflowEdge nextEdge = selectNextTransition(currentNode, outgoing,
          currentData);
      currentNodeId = nextEdge.getTarget();
    }

    return new SimulationResult(false, trace, "Simulation exceeded max steps (" + maxSteps + ")");
  }

  private WorkflowValidator.WorkflowEdge selectNextTransition(WorkflowValidator.WorkflowNode node,
      List<WorkflowValidator.WorkflowEdge> outgoing, Map<String, Object> data) {
    if ("DECISION".equalsIgnoreCase(node.getType())) {
      int index = (int) (Math.random() * outgoing.size());
      return outgoing.get(index);
    }
    return outgoing.get(0);
  }

  private Map<String, List<WorkflowValidator.WorkflowEdge>> buildAdjacencyMap(
      List<WorkflowValidator.WorkflowEdge> edges) {
    Map<String, List<WorkflowValidator.WorkflowEdge>> adjacency = new HashMap<>();
    for (WorkflowValidator.WorkflowEdge edge : edges) {
      adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
    }
    return adjacency;
  }

  public record SimulationResult(boolean success, List<ExecutionStep> trace, String message) {
  }

  public record ExecutionStep(int stepNumber, String nodeId, String nodeType, String nodeLabel,
      Map<String, Object> dataSnapshot) {
  }
}
