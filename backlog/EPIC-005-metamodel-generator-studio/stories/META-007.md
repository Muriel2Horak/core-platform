# META-007: Metamodel Validation & Business Rules

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED** (Design phase)  
**Priorita:** P2 (Quality assurance)  
**Estimated LOC:** ~400 řádků  

---

## 📋 Story Description

Jako **platform developer**, chci **validovat YAML metamodel před aplikací**, abych **zabránil chybným definicím, které způsobí DB migration failures nebo runtime errors**.

---

## 🎯 Acceptance Criteria

### AC1: YAML Schema Validation
- **GIVEN** YAML soubor s metamodel definicí
- **WHEN** provádím reload (META-002)
- **THEN** validator zkontroluje:
  - Required fields (`entity`, `table`, `fields`)
  - Valid field types (string, integer, decimal, boolean, date, text)
  - Reserved keywords (id, tenant_id, created_at, updated_at)
  - Circular relationships detection

### AC2: Business Rule Validation
- **GIVEN** entity s `unique: true` fieldem
- **WHEN** tento field je `nullable: true`
- **THEN** VAROVÁNÍ: "UNIQUE nullable columns problematic - consider NOT NULL"

### AC3: Naming Convention Checks
- **GIVEN** entity name "user-profile"
- **WHEN** validuji
- **THEN** ERROR: "Entity name must be PascalCase (UserProfile)"
- **AND** table name "user_profile" je OK (snake_case)

### AC4: Relationship Validation
- **GIVEN** relationship `User.tenant → NonExistentEntity`
- **WHEN** validuji
- **THEN** ERROR: "Target entity 'NonExistentEntity' does not exist"

---

## 🏗️ Planned Implementation

### MetamodelValidator

```java
@Component
public class MetamodelValidator {
    
    private final MetamodelYamlLoader yamlLoader;
    
    public ValidationReport validate(EntitySchema schema) {
        ValidationReport report = new ValidationReport();
        
        // 1. Schema validation
        validateRequiredFields(schema, report);
        validateFieldTypes(schema, report);
        validateReservedKeywords(schema, report);
        
        // 2. Naming convention checks
        validateNamingConventions(schema, report);
        
        // 3. Business rules
        validateBusinessRules(schema, report);
        
        // 4. Relationship validation
        validateRelationships(schema, report);
        
        // 5. Circular dependency detection
        detectCircularRelationships(schema, report);
        
        return report;
    }
    
    private void validateRequiredFields(EntitySchema schema, ValidationReport report) {
        if (schema.getEntity() == null || schema.getEntity().isBlank()) {
            report.addError("Missing required field: 'entity'");
        }
        if (schema.getTable() == null || schema.getTable().isBlank()) {
            report.addError("Missing required field: 'table'");
        }
        if (schema.getFields() == null || schema.getFields().isEmpty()) {
            report.addError("Entity must have at least one field");
        }
    }
    
    private void validateFieldTypes(EntitySchema schema, ValidationReport report) {
        Set<String> validTypes = Set.of("string", "integer", "long", "decimal", "boolean", "date", "datetime", "text");
        
        for (FieldDefinition field : schema.getFields()) {
            if (!validTypes.contains(field.getType())) {
                report.addError(String.format(
                    "Invalid field type '%s' for field '%s'. Valid types: %s",
                    field.getType(), field.getName(), validTypes
                ));
            }
        }
    }
    
    private void validateReservedKeywords(EntitySchema schema, ValidationReport report) {
        Set<String> reserved = Set.of("id", "tenant_id", "created_at", "updated_at", "deleted_at");
        
        for (FieldDefinition field : schema.getFields()) {
            if (reserved.contains(field.getName())) {
                report.addError(String.format(
                    "Field name '%s' is reserved and auto-generated",
                    field.getName()
                ));
            }
        }
    }
    
    private void validateNamingConventions(EntitySchema schema, ValidationReport report) {
        // Entity name: PascalCase
        if (!schema.getEntity().matches("^[A-Z][a-zA-Z0-9]*$")) {
            report.addError(String.format(
                "Entity name '%s' must be PascalCase (e.g., UserProfile)",
                schema.getEntity()
            ));
        }
        
        // Table name: snake_case
        if (!schema.getTable().matches("^[a-z][a-z0-9_]*$")) {
            report.addError(String.format(
                "Table name '%s' must be snake_case (e.g., user_profiles)",
                schema.getTable()
            ));
        }
        
        // Field names: snake_case
        for (FieldDefinition field : schema.getFields()) {
            if (!field.getName().matches("^[a-z][a-z0-9_]*$")) {
                report.addError(String.format(
                    "Field name '%s' must be snake_case (e.g., profile_picture)",
                    field.getName()
                ));
            }
        }
    }
    
    private void validateBusinessRules(EntitySchema schema, ValidationReport report) {
        for (FieldDefinition field : schema.getFields()) {
            // Warning: UNIQUE + nullable
            if (field.isUnique() && field.isNullable()) {
                report.addWarning(String.format(
                    "Field '%s' is UNIQUE and nullable - consider making it NOT NULL",
                    field.getName()
                ));
            }
            
            // Warning: Text field with unique
            if ("text".equals(field.getType()) && field.isUnique()) {
                report.addWarning(String.format(
                    "Field '%s' is TEXT with UNIQUE - consider using VARCHAR instead (TEXT UNIQUE is slow)",
                    field.getName()
                ));
            }
            
            // Error: Default value with NOT NULL
            if (!field.isNullable() && field.getDefaultValue() == null) {
                report.addWarning(String.format(
                    "Field '%s' is NOT NULL without default value - migration may fail if table has data",
                    field.getName()
                ));
            }
        }
    }
    
    private void validateRelationships(EntitySchema schema, ValidationReport report) {
        if (schema.getRelationships() == null) return;
        
        List<EntitySchema> allEntities = yamlLoader.loadAll();
        Set<String> entityNames = allEntities.stream()
            .map(EntitySchema::getEntity)
            .collect(Collectors.toSet());
        
        for (Relationship rel : schema.getRelationships()) {
            // Check target entity exists
            if (!entityNames.contains(rel.getTarget())) {
                report.addError(String.format(
                    "Relationship '%s' references non-existent entity '%s'",
                    rel.getName(), rel.getTarget()
                ));
            }
            
            // Check relationship type
            if (!Set.of("many-to-one", "one-to-many", "many-to-many").contains(rel.getType())) {
                report.addError(String.format(
                    "Invalid relationship type '%s' for '%s'. Valid: many-to-one, one-to-many, many-to-many",
                    rel.getType(), rel.getName()
                ));
            }
        }
    }
    
    private void detectCircularRelationships(EntitySchema schema, ValidationReport report) {
        // Graph traversal to detect cycles
        Set<String> visited = new HashSet<>();
        Set<String> stack = new HashSet<>();
        
        if (hasCycle(schema.getEntity(), visited, stack)) {
            report.addWarning("Circular relationship detected - may cause lazy loading issues");
        }
    }
    
    private boolean hasCycle(String entityName, Set<String> visited, Set<String> stack) {
        if (stack.contains(entityName)) {
            return true;  // Cycle detected
        }
        if (visited.contains(entityName)) {
            return false;
        }
        
        visited.add(entityName);
        stack.add(entityName);
        
        EntitySchema schema = yamlLoader.load(entityName);
        if (schema.getRelationships() != null) {
            for (Relationship rel : schema.getRelationships()) {
                if (hasCycle(rel.getTarget(), visited, stack)) {
                    return true;
                }
            }
        }
        
        stack.remove(entityName);
        return false;
    }
}

@Data
public class ValidationReport {
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
```

---

## 🧪 Planned Tests

```java
@Test
void shouldFailOnMissingRequiredFields() {
    // Given: Invalid YAML (missing 'table')
    EntitySchema schema = EntitySchema.builder()
        .entity("User")
        // .table("users")  ← MISSING!
        .fields(List.of())
        .build();
    
    // When: Validate
    ValidationReport report = validator.validate(schema);
    
    // Then: Error
    assertThat(report.hasErrors()).isTrue();
    assertThat(report.getErrors()).contains("Missing required field: 'table'");
}

@Test
void shouldWarnOnUniqueNullableField() {
    // Given: Unique nullable field
    EntitySchema schema = EntitySchema.builder()
        .entity("User")
        .table("users")
        .fields(List.of(
            FieldDefinition.builder()
                .name("email")
                .type("string")
                .unique(true)
                .nullable(true)  // ← Problematic!
                .build()
        ))
        .build();
    
    // When: Validate
    ValidationReport report = validator.validate(schema);
    
    // Then: Warning
    assertThat(report.hasWarnings()).isTrue();
    assertThat(report.getWarnings()).anyMatch(w -> w.contains("UNIQUE and nullable"));
}

@Test
void shouldRejectInvalidEntityName() {
    // Given: snake_case entity name (should be PascalCase)
    EntitySchema schema = EntitySchema.builder()
        .entity("user_profile")  // ← Should be UserProfile
        .table("user_profiles")
        .fields(List.of())
        .build();
    
    // When: Validate
    ValidationReport report = validator.validate(schema);
    
    // Then: Error
    assertThat(report.hasErrors()).isTrue();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("must be PascalCase"));
}

@Test
void shouldDetectNonExistentTargetEntity() {
    // Given: Relationship to non-existent entity
    EntitySchema schema = EntitySchema.builder()
        .entity("User")
        .table("users")
        .relationships(List.of(
            Relationship.builder()
                .name("organization")
                .target("NonExistentEntity")  // ← Does not exist!
                .type("many-to-one")
                .build()
        ))
        .build();
    
    // When: Validate
    ValidationReport report = validator.validate(schema);
    
    // Then: Error
    assertThat(report.hasErrors()).isTrue();
    assertThat(report.getErrors()).anyMatch(e -> e.contains("non-existent entity"));
}
```

---

## 💡 Expected Value

### Benefits
- **Pre-Migration Safety**: Catch errors before DB changes
- **Consistency**: Enforce naming standards
- **Developer Experience**: Clear error messages
- **Production Stability**: Prevent runtime failures

### Error Prevention Examples
- ❌ Missing required fields → DB migration failure
- ❌ Invalid field types → JPA mapping errors
- ❌ Reserved keywords → Column name conflicts
- ❌ Circular relationships → LazyInitializationException

---

## 🔗 Related

- **Depends On:** [META-001 (Schema Diff)](META-001.md)
- **Blocks:** [META-002 (Hot Reload)](META-002.md) - validation runs before apply
- **Used By:** [META-005 (Visual Studio)](META-005.md) - real-time validation

---

## 📚 References

- **JSON Schema Validation:** https://json-schema.org/
- **YAML Validation Libraries:** SnakeYAML, Jackson YAML
