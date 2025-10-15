package cz.muriel.core.metamodel.validator;

import cz.muriel.core.metamodel.schema.GlobalMetamodelConfig;
import cz.muriel.core.metamodel.schema.ai.AiPolicies;
import cz.muriel.core.metamodel.schema.ai.GlobalAiConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for AI Schema Validator
 * 
 * @since 2025-10-14
 */
class AiSchemaValidatorTest {
  
  private final AiSchemaValidator validator = new AiSchemaValidator();
  
  @Test
  void testValidGlobalAiConfig() {
    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    aiConfig.setEnabled(true);
    
    AiPolicies policies = new AiPolicies();
    policies.setMaxFields(30);
    policies.setMaxRecords(20);
    policies.setMaxTokens(8000);
    aiConfig.setPolicies(policies);
    
    config.setAi(aiConfig);
    
    List<String> errors = validator.validateGlobalAiConfig(config);
    assertTrue(errors.isEmpty(), "Valid config should have no errors");
  }
  
  @Test
  void testInvalidMaxFields() {
    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    
    AiPolicies policies = new AiPolicies();
    policies.setMaxFields(-1); // Invalid
    aiConfig.setPolicies(policies);
    
    config.setAi(aiConfig);
    
    List<String> errors = validator.validateGlobalAiConfig(config);
    assertFalse(errors.isEmpty(), "Negative maxFields should cause error");
    assertTrue(errors.get(0).contains("maxFields"), "Error should mention maxFields");
  }
  
  @Test
  void testMaxFieldsTooLarge() {
    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    GlobalAiConfig aiConfig = new GlobalAiConfig();
    
    AiPolicies policies = new AiPolicies();
    policies.setMaxFields(150); // Too large
    aiConfig.setPolicies(policies);
    
    config.setAi(aiConfig);
    
    List<String> errors = validator.validateGlobalAiConfig(config);
    assertFalse(errors.isEmpty(), "maxFields > 100 should cause error");
    assertTrue(errors.get(0).contains("too large"), "Error should mention 'too large'");
  }
  
  @Test
  void testNullAiConfigIsValid() {
    GlobalMetamodelConfig config = new GlobalMetamodelConfig();
    config.setAi(null);
    
    List<String> errors = validator.validateGlobalAiConfig(config);
    assertTrue(errors.isEmpty(), "Null AI config should be valid (AI disabled)");
  }
}
