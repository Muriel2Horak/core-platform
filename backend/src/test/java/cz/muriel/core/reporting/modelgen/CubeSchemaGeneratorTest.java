package cz.muriel.core.reporting.modelgen;

import cz.muriel.core.metamodel.schema.EntitySchema;
import cz.muriel.core.metamodel.schema.FieldSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CubeSchemaGenerator.
 */
class CubeSchemaGeneratorTest {

  private CubeSchemaGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new CubeSchemaGenerator();
  }

  @Test
  void shouldGenerateBasicCubeSchema() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("User");
    schema.setTable("users");

    FieldSchema idField = new FieldSchema();
    idField.setName("id");
    idField.setType("uuid");
    idField.setPk(true);

    FieldSchema nameField = new FieldSchema();
    nameField.setName("name");
    nameField.setType("string");

    FieldSchema createdAtField = new FieldSchema();
    createdAtField.setName("created_at");
    createdAtField.setType("timestamp");

    schema.setFields(List.of(idField, nameField, createdAtField));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains("cube(`User`, {").contains("sql: `SELECT * FROM users`")
        .contains("dimensions: {").contains("id: {").contains("type: `string`")
        .contains("primaryKey: true").contains("name: {").contains("createdAt: {")
        .contains("type: `time`").contains("measures: {").contains("count: {")
        .contains("type: `count`").contains("preAggregations: {").contains("dailyRollup: {")
        .contains("@generated DO NOT EDIT MANUALLY");
  }

  @Test
  void shouldGeneratePreAggregationsForTimestampFields() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("Order");
    schema.setTable("orders");

    FieldSchema createdAt = new FieldSchema();
    createdAt.setName("created_at");
    createdAt.setType("timestamp");

    schema.setFields(List.of(createdAt));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains("preAggregations: {").contains("dailyRollup: {")
        .contains("timeDimension: createdAt").contains("granularity: `day`")
        .contains("refreshKey: {").contains("every: `1 hour`");
  }

  @Test
  void shouldGenerateNumericMeasures() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("Product");
    schema.setTable("products");

    FieldSchema priceField = new FieldSchema();
    priceField.setName("price");
    priceField.setType("long");

    schema.setFields(List.of(priceField));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains("priceSum: {").contains("type: `sum`").contains("priceAvg: {")
        .contains("type: `avg`");
  }

  @Test
  void shouldHandleTenantIsolation() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("User");
    schema.setTable("users");
    schema.setTenantField("tenant_id");

    FieldSchema idField = new FieldSchema();
    idField.setName("id");
    idField.setType("uuid");

    schema.setFields(List.of(idField));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains(
        "preAggregationsSchema: `users_preagg_${SECURITY_CONTEXT.tenantId.unsafeValue()}`");
  }

  @Test
  void shouldConvertSnakeCaseToCamelCase() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("User");
    schema.setTable("users");

    FieldSchema field = new FieldSchema();
    field.setName("first_name");
    field.setType("string");

    schema.setFields(List.of(field));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains("firstName: {").contains("sql: `first_name`");
  }

  @Test
  void shouldIncludeDrillMembersInCountMeasure() {
    // Given
    EntitySchema schema = new EntitySchema();
    schema.setEntity("User");
    schema.setTable("users");

    FieldSchema idField = new FieldSchema();
    idField.setName("id");
    idField.setType("uuid");
    idField.setPk(true);

    FieldSchema emailField = new FieldSchema();
    emailField.setName("email");
    emailField.setType("email");

    FieldSchema nameField = new FieldSchema();
    nameField.setName("name");
    nameField.setType("string");

    schema.setFields(List.of(idField, emailField, nameField));

    // When
    String jsCode = generator.generate(schema);

    // Then
    assertThat(jsCode).contains("count: {").contains("drillMembers: [id, email, name]");
  }
}
