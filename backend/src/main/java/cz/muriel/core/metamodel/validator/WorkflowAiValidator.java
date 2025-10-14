package cz.muriel.core.metamodel.validator;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.StateConfig;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for workflow configuration
 * 
 * Ensures workflow states and transitions are properly configured
 * for AI assistance.
 * 
 * @since 2025-10-14
 */
@Slf4j
@Component
public class WorkflowAiValidator {
  
  /**
   * Validate workflow configuration for AI compatibility
   */
  public List<String> validateWorkflow(EntitySchema schema) {
    List<String> errors = new ArrayList<>();
    
    if (schema == null) {
      return errors;
    }
    
    String entityName = schema.getEntity();
    
    // If no workflow defined, that's OK
    if ((schema.getStates() == null || schema.getStates().isEmpty()) &&
        (schema.getTransitions() == null || schema.getTransitions().isEmpty())) {
      return errors;
    }
    
    // Validate states
    if (schema.getStates() != null) {
      Set<String> stateIds = new HashSet<>();
      for (StateConfig state : schema.getStates()) {
        // Unique IDs
        if (stateIds.contains(state.getCode())) {
          errors.add(entityName + ": duplicate state ID: " + state.getCode());
        }
        stateIds.add(state.getCode());
        
        // Required fields
        if (state.getCode() == null || state.getCode().isBlank()) {
          errors.add(entityName + ": state must have code/id");
        }
        if (state.getLabel() == null || state.getLabel().isBlank()) {
          errors.add(entityName + ": state " + state.getCode() + " must have label");
        }
      }
    }
    
    // Validate transitions/actions
    if (schema.getTransitions() != null) {
      Set<String> actionIds = new HashSet<>();
      for (TransitionConfig transition : schema.getTransitions()) {
        // Unique IDs
        if (actionIds.contains(transition.getCode())) {
          errors.add(entityName + ": duplicate action ID: " + transition.getCode());
        }
        actionIds.add(transition.getCode());
        
        // Required fields
        if (transition.getCode() == null || transition.getCode().isBlank()) {
          errors.add(entityName + ": action must have code/id");
        }
        if (transition.getLabel() == null || transition.getLabel().isBlank()) {
          errors.add(entityName + ": action " + transition.getCode() + " must have label");
        }
        
        // How-to validation
        if (transition.getHowto() != null) {
          if (transition.getHowto().size() < 1) {
            errors.add(entityName + "." + transition.getCode() + ": howto must have at least 1 step");
          }
          if (transition.getHowto().size() > 10) {
            errors.add(entityName + "." + transition.getCode() + ": howto should have max 10 steps (recommended 3-7)");
          }
        }
      }
    }
    
    return errors;
  }
}
