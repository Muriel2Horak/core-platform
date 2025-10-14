package cz.muriel.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.entities.MetamodelCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 🎯 W7: Workflow Execution Engine
 * 
 * <p>
 * Executes workflows by traversing nodes and edges based on decision logic.
 * 
 * <p>
 * <b>Features:</b>
 * <ul>
 * <li>Start from Start node, follow edges to End node</li>
 * <li>Execute Task nodes (placeholder for future integrations)</li>
 * <li>Evaluate Decision nodes (condition logic)</li>
 * <li>Track execution history (steps, decisions, timing)</li>
 * <li>Store execution results in workflow_executions table via metamodel</li>
 * </ul>
 * 
 * <p>
 * <b>Node Types:</b>
 * <ul>
 * <li><b>start:</b> Entry point (single start node required)</li>
 * <li><b>task:</b> Execute task (e.g., API call, data transformation) -
 * currently no-op</li>
 * <li><b>decision:</b> Conditional branching based on condition field</li>
 * <li><b>end:</b> Terminal node (execution stops)</li>
 * </ul>
 * 
 * <p>
 * <b>Decision Logic:</b>
 * 
 * <pre>
 * {
 *   "id": "d1",
 *   "type": "decision",
 *   "data": {
 *     "label": "Amount > 1000?",
 *     "condition": "amount > 1000"  // Simple expression
 *   }
 * }
 * 
 * Edges from decision:
 * - edge with label "true" → taken if condition is true
 * - edge with label "false" → taken if condition is false
 * </pre>
 * 
 * @since W7
 */
@Service @Slf4j @RequiredArgsConstructor
public class WorkflowExecutionService {

  private final MetamodelCrudService crudService;
  private final ObjectMapper objectMapper;

  /**
   * Execute a workflow version
   *
   * @param entity Entity name
   * @param context Execution context (variables for decision evaluation)
   * @param auth Authentication
   * @return Execution result
   */
  public ExecutionResult executeWorkflow(String entity, Map<String, Object> context,
      Authentication auth) {
    log.info("Executing workflow for entity: {}", entity);

    try {
      // Load active workflow version
      Map<String, Object> workflow = loadActiveWorkflow(entity, auth);
      if (workflow == null) {
        return ExecutionResult.error("No active workflow found for entity: " + entity);
      }

      // Parse workflow data
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) workflow.get("data");
      String dataJson = objectMapper.writeValueAsString(data);
      @SuppressWarnings("unchecked")
      Map<String, Object> workflowData = objectMapper.readValue(dataJson, Map.class);

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflowData.get("nodes");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> edges = (List<Map<String, Object>>) workflowData.get("edges");

      // Execute workflow
      List<ExecutionStep> steps = new ArrayList<>();
      Instant startTime = Instant.now();

      String currentNodeId = findStartNode(nodes);
      if (currentNodeId == null) {
        return ExecutionResult.error("No start node found in workflow");
      }

      int maxSteps = 100; // Prevent infinite loops
      int stepCount = 0;

      while (currentNodeId != null && stepCount < maxSteps) {
        stepCount++;

        Map<String, Object> currentNode = findNodeById(nodes, currentNodeId);
        if (currentNode == null) {
          return ExecutionResult.error("Node not found: " + currentNodeId);
        }

        String nodeType = (String) currentNode.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> nodeData = (Map<String, Object>) currentNode.get("data");

        ExecutionStep step = new ExecutionStep(currentNodeId, nodeType,
            (String) nodeData.get("label"));
        steps.add(step);

        log.debug("Executing node: id={}, type={}", currentNodeId, nodeType);

        switch (nodeType) {
        case "start":
          currentNodeId = findNextNode(edges, currentNodeId, null);
          break;

        case "task":
          // Execute task: API call, data transformation, notification, etc.
          log.info("Task node executed: {}", nodeData.get("label"));
          step.setResult("Task completed");
          currentNodeId = findNextNode(edges, currentNodeId, null);
          break;

        case "decision":
          String condition = (String) nodeData.get("condition");
          boolean conditionResult = evaluateCondition(condition, context);
          step.setDecision(condition, conditionResult);
          log.info("Decision node evaluated: condition={}, result={}", condition, conditionResult);
          currentNodeId = findNextNode(edges, currentNodeId, conditionResult ? "true" : "false");
          break;

        case "end":
          log.info("Workflow execution completed at end node");
          currentNodeId = null; // Stop execution
          break;

        default:
          log.warn("Unknown node type: {}", nodeType);
          return ExecutionResult.error("Unknown node type: " + nodeType);
        }
      }

      if (stepCount >= maxSteps) {
        return ExecutionResult
            .error("Workflow execution exceeded maximum steps (possible infinite loop)");
      }

      Instant endTime = Instant.now();
      long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

      ExecutionResult result = ExecutionResult.success(steps, durationMs);

      // Store execution result in workflow_executions table
      storeExecutionResult(entity, result, auth);

      return result;
    } catch (Exception e) {
      log.error("Workflow execution failed", e);
      return ExecutionResult.error("Execution failed: " + e.getMessage());
    }
  }

  // ========== Helper Methods ==========

  private Map<String, Object> loadActiveWorkflow(String entity, Authentication auth) {
    try {
      Map<String, String> filter = Map.of("entity", entity, "status", "ACTIVE");
      List<Map<String, Object>> results = crudService.list("WorkflowVersion", filter, null, 0, 1,
          auth);
      return results.isEmpty() ? null : results.get(0);
    } catch (Exception e) {
      log.error("Failed to load active workflow", e);
      return null;
    }
  }

  private String findStartNode(List<Map<String, Object>> nodes) {
    return nodes.stream().filter(node -> "start".equals(node.get("type")))
        .map(node -> (String) node.get("id")).findFirst().orElse(null);
  }

  private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, String nodeId) {
    return nodes.stream().filter(node -> nodeId.equals(node.get("id"))).findFirst().orElse(null);
  }

  private String findNextNode(List<Map<String, Object>> edges, String currentNodeId, String label) {
    return edges.stream().filter(edge -> currentNodeId.equals(edge.get("source")))
        .filter(edge -> label == null || label.equals(edge.get("label")))
        .map(edge -> (String) edge.get("target")).findFirst().orElse(null);
  }

  /**
   * Evaluate simple condition expression
   * 
   * Supports: - amount > 1000 - status == "APPROVED" - count < 5
   * 
   * @param condition Condition expression
   * @param context Execution context
   * @return true if condition is met
   */
  private boolean evaluateCondition(String condition, Map<String, Object> context) {
    if (condition == null || condition.isEmpty()) {
      return true;
    }

    try {
      // Simple expression parser (for demo purposes)
      // In production, use a proper expression engine like SpEL or JEXL

      if (condition.contains(">")) {
        String[] parts = condition.split(">");
        String var = parts[0].trim();
        double threshold = Double.parseDouble(parts[1].trim());
        Object value = context.get(var);
        return value instanceof Number && ((Number) value).doubleValue() > threshold;
      }

      if (condition.contains("<")) {
        String[] parts = condition.split("<");
        String var = parts[0].trim();
        double threshold = Double.parseDouble(parts[1].trim());
        Object value = context.get(var);
        return value instanceof Number && ((Number) value).doubleValue() < threshold;
      }

      if (condition.contains("==")) {
        String[] parts = condition.split("==");
        String var = parts[0].trim();
        String expected = parts[1].trim().replace("\"", "");
        Object value = context.get(var);
        return Objects.equals(String.valueOf(value), expected);
      }

      log.warn("Unsupported condition syntax: {}", condition);
      return false;
    } catch (Exception e) {
      log.error("Failed to evaluate condition: {}", condition, e);
      return false;
    }
  }

  private void storeExecutionResult(String entity, ExecutionResult result, Authentication auth) {
    try {
      Map<String, Object> executionData = Map.of("entity", entity, "status", result.status, "steps",
          result.steps, "durationMs", result.durationMs, "error",
          result.error != null ? result.error : "", "executedAt", Instant.now().toString());

      crudService.create("WorkflowExecution", executionData, auth);
      log.info("Execution result stored for entity: {}", entity);
    } catch (Exception e) {
      log.error("Failed to store execution result", e);
    }
  }

  // ========== DTOs ==========

  public record ExecutionResult(String status, List<ExecutionStep> steps, long durationMs,
      String error) {
    public static ExecutionResult success(List<ExecutionStep> steps, long durationMs) {
      return new ExecutionResult("SUCCESS", steps, durationMs, null);
    }

    public static ExecutionResult error(String error) {
      return new ExecutionResult("ERROR", List.of(), 0, error);
    }
  }

  public static class ExecutionStep {
    private final String nodeId;
    private final String nodeType;
    private final String label;
    private String result;
    private String condition;
    private Boolean conditionResult;

    public ExecutionStep(String nodeId, String nodeType, String label) {
      this.nodeId = nodeId;
      this.nodeType = nodeType;
      this.label = label;
    }

    public void setResult(String result) {
      this.result = result;
    }

    public void setDecision(String condition, boolean conditionResult) {
      this.condition = condition;
      this.conditionResult = conditionResult;
    }

    // Getters for serialization
    public String getNodeId() {
      return nodeId;
    }

    public String getNodeType() {
      return nodeType;
    }

    public String getLabel() {
      return label;
    }

    public String getResult() {
      return result;
    }

    public String getCondition() {
      return condition;
    }

    public Boolean getConditionResult() {
      return conditionResult;
    }
  }
}
