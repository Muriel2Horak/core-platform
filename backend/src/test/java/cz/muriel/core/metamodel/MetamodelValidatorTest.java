package cz.muriel.core.metamodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import cz.muriel.core.metamodel.schema.StateConfig;
import cz.muriel.core.metamodel.schema.TransitionConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🔍 Metamodel YAML Validator Test
 * 
 * Validates all metamodel YAML files against Java schema classes at build time.
 * Prevents invalid YAML from reaching production.
 * 
 * @since 2025-10-13
 */
public class MetamodelValidatorTest {

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Test
  public void validateAllMetamodelYamls() throws Exception {
    Path metamodelDir = Paths.get("src/main/resources/metamodel");

    assertTrue(Files.exists(metamodelDir), "Metamodel directory must exist");

    try (Stream<Path> paths = Files.walk(metamodelDir)) {
      List<Path> yamlFiles = paths.filter(p -> p.toString().endsWith(".yaml"))
          .filter(p -> !p.toString().endsWith(".disabled"))
          .filter(p -> !p.toString().endsWith("global-config.yaml")) // Skip global config - not an
                                                                     // entity schema
          .toList();

      assertFalse(yamlFiles.isEmpty(), "At least one YAML file must exist");

      for (Path yamlFile : yamlFiles) {
        System.out.println("Validating: " + yamlFile.getFileName());
        validateYamlFile(yamlFile);
      }
    }
  }

  private void validateYamlFile(Path yamlFile) throws Exception {
    try {
      EntitySchema schema = yamlMapper.readValue(yamlFile.toFile(), EntitySchema.class);

      // Basic validation
      assertNotNull(schema.getEntity(), yamlFile + ": entity must not be null");
      assertNotNull(schema.getTable(), yamlFile + ": table must not be null");
      assertNotNull(schema.getFields(), yamlFile + ": fields must not be null");
      assertFalse(schema.getFields().isEmpty(), yamlFile + ": fields must not be empty");

      // Validate fields
      for (FieldSchema field : schema.getFields()) {
        assertNotNull(field.getName(), yamlFile + ": field name must not be null");
        assertNotNull(field.getType(),
            yamlFile + ": field type must not be null for field: " + field.getName());

        // Validate supported types
        List<String> supportedTypes = List.of("uuid", "string", "email", "text", "long",
            "timestamp", "boolean", "ref", "collection", "manyToMany", "oneToMany", "manyToOne");
        assertTrue(supportedTypes.contains(field.getType()), yamlFile + ": unsupported field type '"
            + field.getType() + "' for field: " + field.getName());
      }

      // Validate states (if present)
      if (schema.getStates() != null) {
        for (StateConfig state : schema.getStates()) {
          assertNotNull(state.getCode(), yamlFile + ": state code must not be null");
          assertNotNull(state.getLabel(),
              yamlFile + ": state label must not be null for state: " + state.getCode());
        }
      }

      // Validate transitions (if present)
      if (schema.getTransitions() != null) {
        for (TransitionConfig transition : schema.getTransitions()) {
          assertNotNull(transition.getCode(), yamlFile + ": transition code must not be null");
          assertNotNull(transition.getTo(),
              yamlFile + ": transition 'to' must not be null for: " + transition.getCode());
          assertNotNull(transition.getLabel(),
              yamlFile + ": transition label must not be null for: " + transition.getCode());
        }
      }

      // Validate access policy
      assertNotNull(schema.getAccessPolicy(), yamlFile + ": accessPolicy must not be null");
      assertNotNull(schema.getAccessPolicy().getRead(),
          yamlFile + ": accessPolicy.read must not be null");

      // Validate UI config
      if (schema.getUi() != null && schema.getUi().getList() != null) {
        assertNotNull(schema.getUi().getList().getColumns(),
            yamlFile + ": ui.list.columns must not be null");

        // Validate filters are List<String>, not List<Object>
        if (schema.getUi().getList().getFilters() != null) {
          assertTrue(schema.getUi().getList().getFilters() instanceof List,
              yamlFile + ": ui.list.filters must be List<String>");
        }

        // Validate sort config
        if (schema.getUi().getList().getSort() != null) {
          assertNotNull(schema.getUi().getList().getSort().getDefaultField(),
              yamlFile + ": ui.list.sort.defaultField must not be null");
          assertNotNull(schema.getUi().getList().getSort().getFields(),
              yamlFile + ": ui.list.sort.fields must not be null");
        }
      }

      System.out.println("✅ Valid: " + yamlFile.getFileName());

    } catch (Exception e) {
      System.err.println("❌ Invalid: " + yamlFile.getFileName());
      System.err.println("Error: " + e.getMessage());
      throw new AssertionError("Invalid YAML: " + yamlFile + " - " + e.getMessage(), e);
    }
  }

  @Test
  public void validateWorkflowFiles() throws Exception {
    Path metamodelDir = Paths.get("src/main/resources/metamodel");

    // Workflow files must exist
    String[] workflowFiles = { "workflow-draft.yaml", "workflow-proposal.yaml",
        "workflow-version.yaml", "workflow-execution.yaml" };

    for (String filename : workflowFiles) {
      Path file = metamodelDir.resolve(filename);
      assertTrue(Files.exists(file), "Workflow file must exist: " + filename);

      EntitySchema schema = yamlMapper.readValue(file.toFile(), EntitySchema.class);
      assertNotNull(schema, filename + " must be valid");
    }
  }

  @Test
  public void validateWorkflowProposalStates() throws Exception {
    Path file = Paths.get("src/main/resources/metamodel/workflow-proposal.yaml");
    EntitySchema schema = yamlMapper.readValue(file.toFile(), EntitySchema.class);

    assertNotNull(schema.getStates(), "workflow-proposal must have states");
    assertEquals(3, schema.getStates().size(), "workflow-proposal must have 3 states");

    List<String> stateCodes = schema.getStates().stream().map(StateConfig::getCode).toList();

    assertTrue(stateCodes.contains("PENDING"), "Must have PENDING state");
    assertTrue(stateCodes.contains("APPROVED"), "Must have APPROVED state");
    assertTrue(stateCodes.contains("REJECTED"), "Must have REJECTED state");
  }

  @Test
  public void validateWorkflowVersionStates() throws Exception {
    Path file = Paths.get("src/main/resources/metamodel/workflow-version.yaml");
    EntitySchema schema = yamlMapper.readValue(file.toFile(), EntitySchema.class);

    assertNotNull(schema.getStates(), "workflow-version must have states");
    assertEquals(3, schema.getStates().size(), "workflow-version must have 3 states");

    List<String> stateCodes = schema.getStates().stream().map(StateConfig::getCode).toList();

    assertTrue(stateCodes.contains("DRAFT"), "Must have DRAFT state");
    assertTrue(stateCodes.contains("ACTIVE"), "Must have ACTIVE state");
    assertTrue(stateCodes.contains("ARCHIVED"), "Must have ARCHIVED state");
  }

  @Test
  public void validateWorkflowExecutionFields() throws Exception {
    Path file = Paths.get("src/main/resources/metamodel/workflow-execution.yaml");
    EntitySchema schema = yamlMapper.readValue(file.toFile(), EntitySchema.class);

    List<String> fieldNames = schema.getFields().stream().map(FieldSchema::getName).toList();

    // Required fields for WorkflowExecutionService
    assertTrue(fieldNames.contains("entity"), "Must have 'entity' field");
    assertTrue(fieldNames.contains("status"), "Must have 'status' field");
    assertTrue(fieldNames.contains("steps"), "Must have 'steps' field");
    assertTrue(fieldNames.contains("duration_ms"), "Must have 'duration_ms' field");
    assertTrue(fieldNames.contains("executed_at"), "Must have 'executed_at' field");
    assertTrue(fieldNames.contains("executed_by"), "Must have 'executed_by' field");
  }
}
