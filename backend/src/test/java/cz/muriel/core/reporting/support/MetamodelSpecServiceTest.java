package cz.muriel.core.reporting.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muriel.core.metamodel.MetamodelRegistry;
import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetamodelSpecService.
 * 
 * PHASE 2: Tests for full entity spec generation.
 */
@ExtendWith(MockitoExtension.class)
class MetamodelSpecServiceTest {

  @Mock
  private MetamodelRegistry metamodelRegistry;

  private ObjectMapper objectMapper;
  private MetamodelSpecService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new MetamodelSpecService(metamodelRegistry, objectMapper);
  }

  @Test
  void shouldExportCompleteSpec() {
    // Given: Entity schema with various field types
    EntitySchema schema = new EntitySchema();
    schema.setTable("users_directory");

    FieldSchema idField = new FieldSchema();
    idField.setName("id");
    idField.setType("uuid");
    idField.setPk(true);
    idField.setGenerated(true);

    FieldSchema emailField = new FieldSchema();
    emailField.setName("email");
    emailField.setType("email");
    emailField.setRequired(true);
    emailField.setMaxLength(255);

    FieldSchema statusField = new FieldSchema();
    statusField.setName("status");
    statusField.setType("string");
    statusField.setRequired(false);

    FieldSchema versionField = new FieldSchema();
    versionField.setName("version");
    versionField.setType("long");

    FieldSchema createdAtField = new FieldSchema();
    createdAtField.setName("created_at");
    createdAtField.setType("timestamp");

    FieldSchema tenantIdField = new FieldSchema();
    tenantIdField.setName("tenant_id");
    tenantIdField.setType("uuid");
    tenantIdField.setRefEntity("Tenant");

    schema.setFields(Arrays.asList(
        idField, emailField, statusField, versionField, createdAtField, tenantIdField
    ));

    when(metamodelRegistry.getSchemaOrThrow(eq("User"))).thenReturn(schema);

    // When: Export full spec
    EntitySpec spec = service.getFullEntitySpec("User");

    // Then: Spec contains expected metadata
    assertThat(spec).isNotNull();
    assertThat(spec.getEntityName()).isEqualTo("User");
    assertThat(spec.getSpecVersion()).isNotNull().isNotEmpty();

    // Dimensions: id, email, status, tenant_id (UUIDs and strings)
    assertThat(spec.getAllowedDimensions())
        .contains("id", "email", "status", "tenant_id");

    // Measures: version (long)
    assertThat(spec.getAllowedMeasures())
        .contains("version");

    // Editable fields: NOT id (pk), NOT version (read-only), BUT email, status
    assertThat(spec.getEditableFields())
        .contains("email", "status")
        .doesNotContain("id", "version");

    // Validations: email is required with maxLength
    assertThat(spec.getValidations()).containsKey("email");
    assertThat(spec.getValidations().get("email").isRequired()).isTrue();
    assertThat(spec.getValidations().get("email").getMaxLength()).isEqualTo(255);

    // Relations: tenant_id -> Tenant
    assertThat(spec.getRelations()).hasSize(1);
    assertThat(spec.getRelations().get(0).getName()).isEqualTo("tenant_id");
    assertThat(spec.getRelations().get(0).getTargetEntity()).isEqualTo("Tenant");
    assertThat(spec.getRelations().get(0).getRelationType()).isEqualTo("manyToOne");

    // Default time dimension: created_at
    assertThat(spec.getDefaultTimeDimension()).isEqualTo("created_at");
    assertThat(spec.isRequiresTimeRange()).isTrue();

    // Default view: non-sensitive fields
    assertThat(spec.getDefaultView()).isNotNull();
    assertThat(spec.getDefaultView().getSortBy()).isEqualTo("created_at");
    assertThat(spec.getDefaultView().getSortOrder()).isEqualTo("desc");

    // Drilldowns: tenant relation
    assertThat(spec.getDrilldowns()).hasSize(1);
    assertThat(spec.getDrilldowns().get(0).getTargetEntity()).isEqualTo("Tenant");
  }

  @Test
  void shouldExcludeSensitiveFieldsFromEditable() {
    // Given: Schema with password field
    EntitySchema schema = new EntitySchema();
    schema.setTable("users");

    FieldSchema passwordField = new FieldSchema();
    passwordField.setName("password_hash");
    passwordField.setType("string");

    schema.setFields(List.of(passwordField));

    when(metamodelRegistry.getSchemaOrThrow(eq("User"))).thenReturn(schema);

    // When: Export spec
    EntitySpec spec = service.getFullEntitySpec("User");

    // Then: Password not filterable
    assertThat(spec.getAllowedFilters()).doesNotContain("password_hash");
  }

  @Test
  void shouldComputeConsistentChecksum() {
    // Given: Same schema
    EntitySchema schema = new EntitySchema();
    schema.setTable("users");

    FieldSchema field = new FieldSchema();
    field.setName("id");
    field.setType("uuid");
    schema.setFields(List.of(field));

    when(metamodelRegistry.getSchemaOrThrow(eq("User"))).thenReturn(schema);

    // When: Export spec twice
    EntitySpec spec1 = service.getFullEntitySpec("User");
    EntitySpec spec2 = service.getFullEntitySpec("User");

    // Then: Same checksum
    assertThat(spec1.getSpecVersion()).isEqualTo(spec2.getSpecVersion());
  }

  @Test
  void shouldFormatLabelsCorrectly() {
    // Given: Schema with various field name formats
    EntitySchema schema = new EntitySchema();
    schema.setTable("users");

    FieldSchema userIdField = new FieldSchema();
    userIdField.setName("user_id");
    userIdField.setType("uuid");

    FieldSchema firstNameField = new FieldSchema();
    firstNameField.setName("firstName");
    firstNameField.setType("string");

    schema.setFields(Arrays.asList(userIdField, firstNameField));

    when(metamodelRegistry.getSchemaOrThrow(eq("User"))).thenReturn(schema);

    // When: Export spec
    EntitySpec spec = service.getFullEntitySpec("User");

    // Then: Labels are formatted
    assertThat(spec.getFields())
        .extracting(EntitySpec.FieldSpec::getName, EntitySpec.FieldSpec::getLabel)
        .contains(
            tuple("user_id", "User Id"),
            tuple("firstName", "First Name")
        );
  }

  @Test
  void shouldIdentifyMeasureFields() {
    // Given: Schema with numeric fields
    EntitySchema schema = new EntitySchema();
    schema.setTable("products");

    FieldSchema priceField = new FieldSchema();
    priceField.setName("price");
    priceField.setType("long");

    FieldSchema stockField = new FieldSchema();
    stockField.setName("stock");
    stockField.setType("long");

    schema.setFields(Arrays.asList(priceField, stockField));

    when(metamodelRegistry.getSchemaOrThrow(eq("Product"))).thenReturn(schema);

    // When: Export spec
    EntitySpec spec = service.getFullEntitySpec("Product");

    // Then: Numeric fields are measures
    assertThat(spec.getAllowedMeasures()).contains("price", "stock");
  }

  @Test
  void shouldProvideAllowedOperatorsPerFieldType() {
    // Given: Schema with different field types
    EntitySchema schema = new EntitySchema();
    schema.setTable("test");

    FieldSchema stringField = new FieldSchema();
    stringField.setName("name");
    stringField.setType("string");

    FieldSchema longField = new FieldSchema();
    longField.setName("count");
    longField.setType("long");

    FieldSchema boolField = new FieldSchema();
    boolField.setName("active");
    boolField.setType("boolean");

    schema.setFields(Arrays.asList(stringField, longField, boolField));

    when(metamodelRegistry.getSchemaOrThrow(eq("Test"))).thenReturn(schema);

    // When: Export spec
    EntitySpec spec = service.getFullEntitySpec("Test");

    // Then: Operators match field types
    EntitySpec.FieldSpec nameSpec = spec.getFields().stream()
        .filter(f -> f.getName().equals("name"))
        .findFirst().orElseThrow();
    assertThat(nameSpec.getAllowedOperators())
        .contains("eq", "ne", "contains", "startsWith", "endsWith");

    EntitySpec.FieldSpec countSpec = spec.getFields().stream()
        .filter(f -> f.getName().equals("count"))
        .findFirst().orElseThrow();
    assertThat(countSpec.getAllowedOperators())
        .contains("eq", "ne", "gt", "gte", "lt", "lte", "between");

    EntitySpec.FieldSpec activeSpec = spec.getFields().stream()
        .filter(f -> f.getName().equals("active"))
        .findFirst().orElseThrow();
    assertThat(activeSpec.getAllowedOperators())
        .containsExactly("eq", "ne");
  }
}
