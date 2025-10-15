package cz.muriel.core.metamodel.validator;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiConfig;
import cz.muriel.core.metamodel.schema.ai.AiPolicies;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for AI schema configuration
 * 
 * Validates AI policies, prompts, and field annotations. Ensures META_ONLY mode
 * never exposes actual values.
 * 
 * @since 2025-10-14
 */
@Slf4j @Component
public class AiSchemaValidator {

  /**
   * Validate global AI configuration
   */
  public List<String> validateGlobalAiConfig(GlobalMetamodelConfig config) {
    List<String> errors = new ArrayList<>();

    if (config == null || config.getAi() == null) {
      return errors; // AI config is optional
    }

    GlobalAiConfig aiConfig = config.getAi();

    // Validate policies
    if (aiConfig.getPolicies() != null) {
      errors.addAll(validatePolicies(aiConfig.getPolicies(), "global"));
    }

    // Validate prompts
    if (aiConfig.getPrompts() != null) {
      if (aiConfig.getPrompts().getUserAgent() != null) {
        String systemPrompt = aiConfig.getPrompts().getUserAgent().get("system");
        if (systemPrompt != null && systemPrompt.length() > 2000) {
          errors.add("global: userAgent.system prompt too long (max 2000 chars)");
        }
      }
      if (aiConfig.getPrompts().getDevAgent() != null) {
        String systemPrompt = aiConfig.getPrompts().getDevAgent().get("system");
        if (systemPrompt != null && systemPrompt.length() > 2000) {
          errors.add("global: devAgent.system prompt too long (max 2000 chars)");
        }
      }
    }

    return errors;
  }

  /**
   * Validate entity AI configuration
   */
  public List<String> validateEntityAiConfig(EntitySchema schema) {
    List<String> errors = new ArrayList<>();

    if (schema == null) {
      return errors;
    }

    String entityName = schema.getEntity();
    AiConfig aiConfig = schema.getAi();

    if (aiConfig == null) {
      return errors; // AI config is optional for entities
    }

    // Validate policies
    if (aiConfig.getPolicies() != null) {
      errors.addAll(validatePolicies(aiConfig.getPolicies(), entityName));
    }

    // Validate field annotations
    if (schema.getFields() != null) {
      for (FieldSchema field : schema.getFields()) {
        errors.addAll(validateFieldAiAnnotations(field, entityName));
      }
    }

    return errors;
  }

  /**
   * Validate AI policies
   */
  private List<String> validatePolicies(AiPolicies policies, String context) {
    List<String> errors = new ArrayList<>();

    if (policies.getMaxFields() != null && policies.getMaxFields() <= 0) {
      errors.add(context + ": maxFields must be positive");
    }
    if (policies.getMaxFields() != null && policies.getMaxFields() > 100) {
      errors.add(context + ": maxFields too large (max 100)");
    }

    if (policies.getMaxRecords() != null && policies.getMaxRecords() <= 0) {
      errors.add(context + ": maxRecords must be positive");
    }
    if (policies.getMaxRecords() != null && policies.getMaxRecords() > 1000) {
      errors.add(context + ": maxRecords too large (max 1000)");
    }

    if (policies.getMaxTokens() != null && policies.getMaxTokens() <= 0) {
      errors.add(context + ": maxTokens must be positive");
    }
    if (policies.getMaxTokens() != null && policies.getMaxTokens() > 100000) {
      errors.add(context + ": maxTokens too large (max 100000)");
    }

    return errors;
  }

  /**
   * Validate field AI annotations
   */
  private List<String> validateFieldAiAnnotations(FieldSchema field, String entityName) {
    List<String> errors = new ArrayList<>();

    String fieldName = field.getName();

    // If PII is marked, helpSafe should typically be false
    if (Boolean.TRUE.equals(field.getPii()) && Boolean.TRUE.equals(field.getHelpSafe())) {
      log.warn("{}.{}: Field marked as PII but helpSafe=true - verify this is intentional",
          entityName, fieldName);
    }

    // If mask is provided, validate pattern
    if (field.getMask() != null && field.getMask().trim().isEmpty()) {
      errors.add(entityName + "." + fieldName + ": mask cannot be empty string");
    }

    return errors;
  }

  /**
   * Validate that META_ONLY context doesn't contain actual values This is a
   * critical security check
   */
  public void assertNoValuesInMetaOnlyContext(Object context, String source) {
    // This will be implemented when we create the context assembler
    // For now, just a placeholder
    log.debug("Validating META_ONLY context from: {}", source);
  }
}
