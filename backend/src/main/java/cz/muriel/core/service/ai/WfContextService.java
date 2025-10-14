package cz.muriel.core.service.ai;

import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StateConfig;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Workflow Context Service for AI agents
 * 
 * Exports workflow metadata (states, actions, how-to) without data.
 * META_ONLY mode - no actual values, only process structure.
 * 
 * @since 2025-10-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfContextService {
  
  private final MetamodelRegistry metamodelRegistry;
  
  /**
   * Get workflow metadata for entity
   * 
   * @param entityName Entity name
   * @return Workflow metadata (states, actions, how-to)
   */
  public Map<String, Object> getWorkflowForEntity(String entityName) {
    EntitySchema schema = metamodelRegistry.getSchema(entityName)
        .orElseThrow(() -> new IllegalArgumentException("Entity not found: " + entityName));
    
    return buildWorkflowContext(schema);
  }
  
  /**
   * Get workflow metadata by route ID
   * 
   * @param routeId Route identifier (e.g., "users.detail", "proposals.review")
   * @return Workflow metadata
   */
  public Map<String, Object> getWorkflowForRoute(String routeId) {
    // Extract entity from routeId (users.detail -> User)
    String entityName = extractEntityFromRoute(routeId);
    return getWorkflowForEntity(entityName);
  }
  
  /**
   * Build workflow context from entity schema
   */
  private Map<String, Object> buildWorkflowContext(EntitySchema schema) {
    Map<String, Object> context = new LinkedHashMap<>();
    
    context.put("entity", schema.getEntity());
    
    // States
    if (schema.getStates() != null && !schema.getStates().isEmpty()) {
      List<Map<String, Object>> states = schema.getStates().stream()
          .map(this::serializeState)
          .collect(Collectors.toList());
      context.put("states", states);
    }
    
    // Actions (transitions)
    if (schema.getTransitions() != null && !schema.getTransitions().isEmpty()) {
      List<Map<String, Object>> actions = schema.getTransitions().stream()
          .map(this::serializeAction)
          .collect(Collectors.toList());
      context.put("actions", actions);
    }
    
    // Extract how-to steps from all actions
    if (schema.getTransitions() != null) {
      Map<String, List<String>> howtos = new LinkedHashMap<>();
      for (TransitionConfig transition : schema.getTransitions()) {
        if (transition.getHowto() != null && !transition.getHowto().isEmpty()) {
          howtos.put(transition.getCode(), transition.getHowto());
        }
      }
      if (!howtos.isEmpty()) {
        context.put("howto", howtos);
      }
    }
    
    return context;
  }
  
  /**
   * Serialize state for AI context
   */
  private Map<String, Object> serializeState(StateConfig state) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", state.getCode());
    result.put("label", state.getLabel());
    if (state.getDescription() != null) {
      result.put("description", state.getDescription());
    }
    if (state.getHelp() != null) {
      result.put("help", state.getHelp());
    }
    return result;
  }
  
  /**
   * Serialize action/transition for AI context
   */
  private Map<String, Object> serializeAction(TransitionConfig transition) {
    Map<String, Object> result = new LinkedHashMap<>();
    
    result.put("id", transition.getCode());
    result.put("label", transition.getLabel());
    result.put("from", transition.getFrom());
    result.put("to", transition.getTo());
    
    if (transition.getHelp() != null) {
      result.put("help", transition.getHelp());
    }
    if (transition.getIcon() != null) {
      result.put("icon", transition.getIcon());
    }
    if (Boolean.TRUE.equals(transition.getDangerous())) {
      result.put("dangerous", true);
    }
    if (transition.getRoutes() != null && !transition.getRoutes().isEmpty()) {
      result.put("routes", transition.getRoutes());
    }
    if (transition.getPreconditions() != null && !transition.getPreconditions().isEmpty()) {
      result.put("preconditions", transition.getPreconditions());
    }
    if (transition.getPostconditions() != null && !transition.getPostconditions().isEmpty()) {
      result.put("postconditions", transition.getPostconditions());
    }
    if (transition.getSideEffects() != null && !transition.getSideEffects().isEmpty()) {
      result.put("sideEffects", transition.getSideEffects());
    }
    if (transition.getErrors() != null && !transition.getErrors().isEmpty()) {
      result.put("possibleErrors", transition.getErrors());
    }
    if (transition.getHowto() != null && !transition.getHowto().isEmpty()) {
      result.put("howto", transition.getHowto());
    }
    if (transition.getStreamingPriority() != null) {
      result.put("streamingPriority", transition.getStreamingPriority());
    }
    
    return result;
  }
  
  /**
   * Extract entity name from route ID
   * 
   * Examples:
   * - users.detail -> User
   * - proposals.review -> Proposal
   * - workflow-draft.edit -> WorkflowDraft
   */
  private String extractEntityFromRoute(String routeId) {
    if (routeId == null || routeId.isEmpty()) {
      throw new IllegalArgumentException("Route ID cannot be empty");
    }
    
    String[] parts = routeId.split("\\.");
    String entityPart = parts[0];
    
    // Convert plural to singular and capitalize
    // Simple heuristic: remove trailing 's' if present
    if (entityPart.endsWith("s")) {
      entityPart = entityPart.substring(0, entityPart.length() - 1);
    }
    
    // Capitalize first letter
    return Character.toUpperCase(entityPart.charAt(0)) + entityPart.substring(1);
  }
}
