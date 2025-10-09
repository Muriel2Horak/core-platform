# Metamodel Schema Generator - Capability Analysis

## Současný stav generátoru (v1)

### ✅ CO UMÍ:
1. **CREATE TABLE** - vytvoří novou tabulku z YAML
2. **ALTER TABLE ADD COLUMN** - přidá chybějící sloupce
3. **CREATE INDEX** - vytvoří indexy na tenant_id, version, foreign keys
4. **CREATE TRIGGER** - auto-increment pro version field
5. **Detekce existence** - kontroluje, jestli tabulka/sloupec existuje

### ❌ CO NEUMÍ:
1. **ALTER COLUMN TYPE** - změna typu sloupce (VARCHAR → TEXT, INT → BIGINT, atd.)
2. **ALTER COLUMN** - změna nullable, default value, constraints
3. **DROP COLUMN** - odstranění nepotřebných sloupců
4. **RENAME COLUMN** - přejmenování sloupce
5. **Data migration** - transformace dat při změně typu
6. **Complex constraints** - CHECK, UNIQUE, EXCLUSION
7. **M:N relationships** - junction tables
8. **Hot reload** - změny bez restartu
9. **Rollback** - vrácení změn při chybě
10. **Version tracking** - historie změn schématu

## PostgreSQL Type Conversion Support

### ✅ Safe Automatic Conversions (bez data loss)
```sql
-- Numbers (narrower → wider)
SMALLINT → INTEGER → BIGINT → NUMERIC
REAL → DOUBLE PRECISION

-- Strings (shorter → longer)
VARCHAR(50) → VARCHAR(100) → TEXT
CHAR → VARCHAR → TEXT

-- Time
DATE → TIMESTAMP → TIMESTAMPTZ

-- JSON
TEXT → JSON → JSONB
VARCHAR → JSON

-- Boolean
BOOLEAN ↔ INTEGER (0/1)
```

### ⚠️ Risky Conversions (possible data loss)
```sql
-- Numbers (wider → narrower) - FAIL if out of range
BIGINT → INTEGER → SMALLINT
NUMERIC(10,2) → INTEGER  -- loses decimals

-- Strings (longer → shorter) - TRUNCATE or FAIL
TEXT → VARCHAR(100)
VARCHAR(100) → VARCHAR(50)

-- JSON → TEXT - OK but loses structure
JSONB → JSON → TEXT

-- Timestamp → Date - loses time
TIMESTAMPTZ → TIMESTAMP → DATE
```

### ❌ Impossible Direct Conversions (need USING)
```sql
-- String → Number
VARCHAR → INTEGER  -- USING column::INTEGER
TEXT → NUMERIC

-- String → Boolean
VARCHAR → BOOLEAN  -- USING column::BOOLEAN

-- Number → String  
INTEGER → VARCHAR  -- automatic cast works

-- UUID ↔ anything
UUID → VARCHAR  -- USING column::TEXT
VARCHAR → UUID  -- USING column::UUID
```

## Implementace Safe Type Changes

### Strategy Matrix

| From        | To          | Strategy                          | Risk  | Data Loss |
|-------------|-------------|-----------------------------------|-------|-----------|
| VARCHAR(N)  | VARCHAR(M)  | ALTER TYPE (M>N), else FAIL       | LOW   | Possible  |
| VARCHAR     | TEXT        | ALTER TYPE                        | NONE  | No        |
| TEXT        | VARCHAR(N)  | ALTER TYPE + USING substring      | HIGH  | Yes       |
| INTEGER     | BIGINT      | ALTER TYPE                        | NONE  | No        |
| BIGINT      | INTEGER     | ALTER TYPE + CHECK                | HIGH  | Possible  |
| DATE        | TIMESTAMP   | ALTER TYPE                        | NONE  | No        |
| TIMESTAMP   | DATE        | ALTER TYPE + USING cast           | MED   | Yes       |
| TEXT        | JSON        | ALTER TYPE + USING cast           | MED   | Possible  |
| JSON        | JSONB       | ALTER TYPE                        | LOW   | No        |
| BOOLEAN     | INTEGER     | ALTER TYPE + USING CASE           | NONE  | No        |
| VARCHAR     | UUID        | ALTER TYPE + USING cast + VALIDATE| HIGH  | Possible  |

### Allowed Type Conversions Config

```yaml
# metamodel-type-conversions.yaml
conversions:
  safe:
    - from: "VARCHAR"
      to: "TEXT"
      sql: "ALTER TABLE {table} ALTER COLUMN {column} TYPE TEXT"
      
    - from: "INTEGER"
      to: "BIGINT"
      sql: "ALTER TABLE {table} ALTER COLUMN {column} TYPE BIGINT"
      
    - from: "DATE"
      to: "TIMESTAMP"
      sql: "ALTER TABLE {table} ALTER COLUMN {column} TYPE TIMESTAMPTZ"
      
    - from: "TEXT"
      to: "JSON"
      sql: |
        ALTER TABLE {table} ALTER COLUMN {column} TYPE JSON 
        USING CASE 
          WHEN {column} ~ '^[\[\{]' THEN {column}::JSON 
          ELSE NULL 
        END
      warning: "Invalid JSON values will become NULL"
      
  risky:
    - from: "TEXT"
      to: "VARCHAR"
      sql: "ALTER TABLE {table} ALTER COLUMN {column} TYPE VARCHAR({maxLength}) USING LEFT({column}, {maxLength})"
      warning: "Data will be truncated"
      requiresConfirmation: true
      
    - from: "BIGINT"
      to: "INTEGER"
      sql: |
        ALTER TABLE {table} ALTER COLUMN {column} TYPE INTEGER 
        USING CASE 
          WHEN {column} BETWEEN -2147483648 AND 2147483647 THEN {column}::INTEGER
          ELSE NULL
        END
      warning: "Values out of range will become NULL"
      requiresConfirmation: true
      
    - from: "VARCHAR"
      to: "UUID"
      sql: |
        ALTER TABLE {table} ALTER COLUMN {column} TYPE UUID 
        USING CASE 
          WHEN {column} ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' 
          THEN {column}::UUID
          ELSE NULL
        END
      warning: "Invalid UUIDs will become NULL"
      requiresConfirmation: true
      
  forbidden:
    - from: "*"
      to: "*"
      message: "Type conversion not allowed. Create migration manually."
```

## Vylepšený Generator v2

### Features:

#### 1. **Schema Diff Detection**
```java
public SchemaDiff detectChanges(EntitySchema schema) {
  SchemaDiff diff = new SchemaDiff();
  
  // Detect:
  // - Missing columns (ADD)
  // - Type changes (ALTER TYPE)
  // - Nullable changes (ALTER ... SET/DROP NOT NULL)
  // - Default changes (ALTER ... SET/DROP DEFAULT)
  // - Missing indexes (CREATE INDEX)
  // - Missing constraints (ADD CONSTRAINT)
  // - Orphaned columns (DROP - optional)
  
  return diff;
}
```

#### 2. **Safe Type Conversion**
```java
public void applyTypeChange(String table, String column, 
    String fromType, String toType, FieldSchema field) {
  
  TypeConversion conversion = conversionRegistry.find(fromType, toType);
  
  if (conversion == null) {
    throw new UnsupportedOperationException(
      "Conversion from " + fromType + " to " + toType + " not supported");
  }
  
  if (conversion.isRisky() && !isConfirmed()) {
    throw new ManualMigrationRequiredException(
      "Risky conversion requires manual approval");
  }
  
  String sql = conversion.generateSql(table, column, field);
  jdbcTemplate.execute(sql);
}
```

#### 3. **Data Migration Hooks**
```yaml
# user.yaml
fields:
  - name: phone
    type: string
    maxLength: 50
    migration:
      from: phone_number  # Rename old column
      transform: "REGEXP_REPLACE(phone_number, '[^0-9+]', '', 'g')"
      
  - name: settings_json
    type: json
    migration:
      from: settings_text
      transform: |
        CASE 
          WHEN settings_text IS NULL THEN '{}'::JSON
          WHEN settings_text ~ '^[\[\{]' THEN settings_text::JSON
          ELSE json_build_object('legacy', settings_text)
        END
```

#### 4. **Constraint Management**
```yaml
# user.yaml
fields:
  - name: email
    type: email
    unique: true  # → CREATE UNIQUE INDEX
    
  - name: age
    type: integer
    check: "age >= 0 AND age <= 150"  # → ALTER TABLE ADD CHECK
    
  - name: status
    type: string
    enum: ["ACTIVE", "INACTIVE", "DELETED"]  # → CHECK constraint
```

#### 5. **Relationship Auto-Generation**
```yaml
# user.yaml
relationships:
  - name: roles
    type: manyToMany
    targetEntity: Role
    autoGenerateJunctionTable: true  # ← Generator vytvoří user_roles
```

Generator vytvoří:
```sql
CREATE TABLE IF NOT EXISTS user_roles (
  user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
```

#### 6. **Hot Reload (bez restartu)**
```java
@RestController
@RequestMapping("/admin/metamodel")
public class MetamodelAdminController {
  
  @PostMapping("/reload")
  public ResponseEntity<?> reloadSchema() {
    // 1. Reload YAML files
    registry.reload();
    
    // 2. Detect changes
    List<SchemaDiff> diffs = generator.detectAllChanges();
    
    // 3. Apply safe changes
    generator.applySafeChanges(diffs);
    
    // 4. Report risky changes
    return ResponseEntity.ok(Map.of(
      "applied", diffs.stream().filter(SchemaDiff::wasApplied).count(),
      "pending", diffs.stream().filter(SchemaDiff::requiresApproval).toList()
    ));
  }
  
  @PostMapping("/apply-pending")
  public ResponseEntity<?> applyPendingChanges(
      @RequestBody ApprovalRequest approval) {
    // Apply risky changes with confirmation
    generator.applyRiskyChanges(approval.getChanges());
    return ResponseEntity.ok("Changes applied");
  }
}
```

#### 7. **Version Tracking**
```sql
CREATE TABLE metamodel_schema_history (
  id SERIAL PRIMARY KEY,
  entity_type VARCHAR(100),
  table_name VARCHAR(100),
  change_type VARCHAR(50),  -- CREATE_TABLE, ADD_COLUMN, ALTER_TYPE, etc.
  old_definition TEXT,
  new_definition TEXT,
  applied_at TIMESTAMPTZ DEFAULT NOW(),
  applied_by VARCHAR(100),
  status VARCHAR(20)  -- SUCCESS, FAILED, ROLLED_BACK
);
```

#### 8. **Rollback Support**
```java
@Transactional
public void applyChangesWithRollback(List<SchemaChange> changes) {
  Savepoint savepoint = connection.setSavepoint("schema_change");
  
  try {
    for (SchemaChange change : changes) {
      applyChange(change);
      recordHistory(change);
    }
  } catch (Exception e) {
    log.error("Schema change failed, rolling back: {}", e.getMessage());
    connection.rollback(savepoint);
    throw e;
  }
}
```

## Implementační Plán

### Phase 1: Enhanced Type Detection ✅
```java
class ColumnInfo {
  String name;
  String dataType;
  Integer characterMaximumLength;
  Boolean isNullable;
  String columnDefault;
}

ColumnInfo getCurrentColumnInfo(String table, String column) {
  String sql = """
      SELECT 
        column_name,
        data_type,
        character_maximum_length,
        is_nullable,
        column_default
      FROM information_schema.columns
      WHERE table_name = ? AND column_name = ?
      """;
  return jdbcTemplate.queryForObject(sql, columnInfoMapper, table, column);
}
```

### Phase 2: Type Conversion Registry ✅
```java
@Component
class TypeConversionRegistry {
  Map<TypePair, TypeConversion> conversions = new HashMap<>();
  
  void register(String from, String to, TypeConversion conversion) {
    conversions.put(new TypePair(from, to), conversion);
  }
  
  TypeConversion find(String from, String to) {
    return conversions.get(new TypePair(from, to));
  }
}
```

### Phase 3: Migration DSL ✅
```yaml
# migration in YAML
fields:
  - name: phone
    type: string
    migration:
      strategy: rename_and_transform
      from: phone_number
      transform: "REGEXP_REPLACE({old}, '[^0-9+]', '', 'g')"
```

### Phase 4: Constraint Support ✅
- Unique indexes
- Check constraints
- Enum validation
- Foreign key cascade rules

### Phase 5: Hot Reload ✅
- Watch YAML files
- Detect changes
- Apply safe changes automatically
- Queue risky changes for approval

### Phase 6: Audit & Rollback ✅
- Schema history table
- Change tracking
- Rollback commands
- Diff visualization

## Testing Strategy

### Unit Tests
```java
@Test
void testSafeTypeConversion_VarcharToText() {
  generator.alterColumnType("users", "bio", "VARCHAR(500)", "TEXT");
  
  String actualType = getColumnType("users", "bio");
  assertEquals("text", actualType);
}

@Test
void testRiskyConversion_RequiresConfirmation() {
  assertThrows(ManualMigrationRequiredException.class, () -> {
    generator.alterColumnType("users", "age_text", "TEXT", "INTEGER");
  });
}
```

### Integration Tests
```java
@Test
void testHotReload_AddColumn() {
  // 1. Update YAML file
  updateYaml("user.yaml", """
      - name: new_field
        type: string
      """);
  
  // 2. Trigger reload
  metam odelController.reloadSchema();
  
  // 3. Verify column exists
  assertTrue(columnExists("users_directory", "new_field"));
}
```

## Configuration

```yaml
# application.yml
metamodel:
  schema:
    auto-generate: true
    auto-apply-safe-changes: true
    auto-apply-risky-changes: false  # Requires approval
    hot-reload:
      enabled: true
      watch-interval: 5s
    conversions:
      allow-data-loss: false
      require-confirmation-for-risky: true
    audit:
      enabled: true
      table: metamodel_schema_history
```

## CLI Commands

```bash
# Generate diff
./mvnw exec:java -Dexec.mainClass="MetamodelCLI" -Dexec.args="diff"

# Apply safe changes
./mvnw exec:java -Dexec.mainClass="MetamodelCLI" -Dexec.args="apply --safe-only"

# Apply all changes (with confirmation)
./mvnw exec:java -Dexec.mainClass="MetamodelCLI" -Dexec.args="apply --all --confirm"

# Rollback last change
./mvnw exec:java -Dexec.mainClass="MetamodelCLI" -Dexec.args="rollback --last"

# Show history
./mvnw exec:java -Dexec.mainClass="MetamodelCLI" -Dexec.args="history --table users_directory"
```

## Summary

### Co generátor v1 UMÍ:
- ✅ CREATE TABLE
- ✅ ADD COLUMN
- ✅ CREATE INDEX
- ✅ CREATE TRIGGER (version)

### Co potřebujeme přidat (v2):
- 🔨 ALTER COLUMN TYPE (se safe conversion matrix)
- 🔨 ALTER COLUMN constraints (nullable, default)
- 🔨 DROP COLUMN (optional, risky)
- 🔨 RENAME COLUMN
- 🔨 Data migration hooks
- 🔨 Complex constraints (unique, check)
- 🔨 Junction table auto-generation
- 🔨 Hot reload without restart
- 🔨 Rollback support
- 🔨 Version tracking

### Priorita:
1. **P0:** ALTER COLUMN TYPE (safe conversions only)
2. **P0:** Junction table generation
3. **P1:** Constraint management
4. **P1:** Hot reload
5. **P2:** Data migration hooks
6. **P2:** Rollback support

---
**Next Step:** Implementovat v2 s podporou safe type conversions a hot reload
