# Metamodel Schema Generator v2 - Implementation Summary

## Co jsme implementovali

### 1. **SchemaDiff.java** ✅
Detekce změn mezi YAML a DB:
- `ColumnChange` - přidání, změna typu, nullable, default
- `IndexChange` - vytvoření/odstranění indexů
- `ConstraintChange` - unique, check, foreign keys
- `TriggerChange` - version auto-increment triggers
- Detekce **risky changes** (možná ztráta dat)

### 2. **TypeConversionRegistry.java** ✅
Registry podporovaných konverzí typu:

#### Safe Conversions (bez ztráty dat):
- VARCHAR → TEXT
- INTEGER → BIGINT
- SMALLINT → INTEGER  
- DATE → TIMESTAMP
- JSON → JSONB
- VARCHAR(50) → VARCHAR(100)

#### Risky Conversions (možná ztráta dat):
- TEXT → VARCHAR(N) - truncate na N znaků
- BIGINT → INTEGER - NULL pokud out of range
- TEXT/VARCHAR → JSON - NULL pokud invalid JSON
- TEXT/VARCHAR → UUID - NULL pokud invalid UUID
- TEXT/VARCHAR → INTEGER - NULL pokud non-numeric
- TEXT/VARCHAR → BOOLEAN - NULL pokud unrecognized
- TIMESTAMP → DATE - ztráta času

### 3. **MetamodelSchemaGenerator.java** (existující)
- ✅ CREATE TABLE z YAML
- ✅ ADD COLUMN pro chybějící fieldy
- ✅ CREATE INDEX
- ✅ CREATE TRIGGER pro version
- ⏳ Plánované: ALTER COLUMN TYPE (s TypeConversionRegistry)

## Co generátor UMÍ

### Aktuální capabilities (v1):

```java
// 1. Vytvoření tabulky z YAML
@PostConstruct
public void generateSchema() {
  for (EntitySchema schema : registry.getAllSchemas().values()) {
    if (!tableExists(schema.getTable())) {
      createTable(schema);  // ✅
    }
  }
}

// 2. Přidání chybějících sloupců
alterTableAddMissingColumns(schema);  // ✅

// 3. Vytvoření indexů
createIndexes(schema);  // ✅
// - tenant_id
// - version
// - foreign keys (_id columns)

// 4. Vytvoření version triggeru
createVersionTrigger(schema);  // ✅
// - AUTO INCREMENT version při UPDATE
// - AUTO UPDATE updated_at timestamp
```

### Type Mapping YAML → PostgreSQL:

| YAML Type  | PostgreSQL Type | Notes                  |
|------------|-----------------|------------------------|
| uuid       | UUID            | Primary key compatible |
| string     | VARCHAR(N)      | Default 255            |
| email      | VARCHAR(N)      | Same as string         |
| text       | TEXT            | Unlimited length       |
| boolean    | BOOLEAN         | TRUE/FALSE             |
| integer    | INTEGER         | 32-bit                 |
| long       | BIGINT          | 64-bit                 |
| timestamp  | TIMESTAMPTZ     | With timezone          |
| date       | DATE            | Date only              |
| manyToOne  | UUID            | Foreign key            |

### Co generátor zatím NEUMÍ (plánované v2):

```java
// ❌ ALTER COLUMN TYPE
// Potřeba integrace s TypeConversionRegistry

// ❌ ALTER COLUMN SET/DROP NOT NULL
// ❌ ALTER COLUMN SET/DROP DEFAULT

// ❌ DROP COLUMN
// Risky operation - manual only?

// ❌ RENAME COLUMN
// Needs mapping: old_name → new_name

// ❌ Complex constraints
// - UNIQUE (per column or multi-column)
// - CHECK (validation rules)
// - EXCLUDE (advanced)

// ❌ Junction table auto-generation
// Pro many-to-many relationships

// ❌ Data migration hooks
// Transform data during schema change

// ❌ Hot reload
// Apply changes without restart

// ❌ Rollback
// Undo failed changes
```

## Použití

### Development Mode (auto-generate):

```yaml
# application.yml
metamodel:
  schema:
    auto-generate: true  # ← Zapne generátor při startu
```

```bash
# 1. Clean start
docker compose down -v

# 2. Start services
make start-dev

# Backend při startu:
# 📋 Processing entity: User
# 🔨 Creating table: users_directory
# ✅ Table created: users_directory
# 📑 Creating indexes for: users_directory
# ⚡ Creating version trigger for: users_directory
# ✅ Version trigger created: trigger_increment_users_directory_version
```

### Manual Schema Operations:

```java
@Autowired
private MetamodelSchemaGenerator generator;

// Regenerate all tables
generator.generateSchema();

// Drop all (DEV only!)
generator.dropAllTables();
```

## Testing

### Test Scenarios:

#### 1. **Clean Database → Generate Schema**
```bash
docker compose down -v
make start-dev
# Očekáváme: Všechny tabulky vytvořené z YAML
```

#### 2. **Add New Field → Auto-detect & Add Column**
```yaml
# user.yaml
fields:
  - name: new_field  # ← NOVÝ
    type: string
```

Restart backend:
```
🔍 Checking for missing columns in: users_directory
➕ Adding column new_field to table users_directory
✅ Column added: users_directory.new_field
```

#### 3. **Change Type (budoucnost v2)**
```yaml
# user.yaml
fields:
  - name: phone
    type: string  # Změna z INTEGER
```

S v2:
```
⚠️ Detected type change: phone INTEGER → VARCHAR(255)
🔍 Finding conversion: INTEGER → VARCHAR
✅ Safe conversion found
🔨 ALTER TABLE users_directory ALTER COLUMN phone TYPE VARCHAR(255) USING phone::TEXT
```

## Next Steps

### Phase 1: Schema Diff Detection ⏳
```java
public SchemaDiff detectChanges(EntitySchema schema) {
  // 1. Get current DB schema
  Map<String, ColumnInfo> dbColumns = getCurrentColumns(schema.getTable());
  
  // 2. Compare with YAML schema
  for (FieldSchema field : schema.getFields()) {
    ColumnInfo dbColumn = dbColumns.get(field.getName());
    
    if (dbColumn == null) {
      diff.addColumnChange(ColumnChange.ADD, field);
    } else if (!typesMatch(field, dbColumn)) {
      diff.addColumnChange(ColumnChange.ALTER_TYPE, field, dbColumn);
    }
  }
  
  return diff;
}
```

### Phase 2: Apply Type Conversions ⏳
```java
public void applyTypeChange(SchemaDiff.ColumnChange change) {
  TypeConversion conversion = conversionRegistry
      .find(change.getOldType(), change.getNewType())
      .orElseThrow(() -> new UnsupportedOperationException(
          "No conversion available"));
  
  if (conversion.isRisky() && !isConfirmed) {
    throw new ManualApprovalRequiredException(conversion.getWarning());
  }
  
  String sql = conversion.generateSql(
      change.getTableName(), 
      change.getColumnName(), 
      change.getField().getMaxLength()
  );
  
  jdbcTemplate.execute(sql);
}
```

### Phase 3: Hot Reload API ⏳
```java
@PostMapping("/admin/metamodel/reload")
public ResponseEntity<?> reloadSchema() {
  // 1. Reload YAML
  registry.reload();
  
  // 2. Detect diff
  List<SchemaDiff> diffs = generator.detectAllChanges();
  
  // 3. Apply safe changes
  for (SchemaDiff diff : diffs) {
    if (!diff.hasRiskyChanges()) {
      generator.applyDiff(diff);
    }
  }
  
  return ResponseEntity.ok(Map.of(
    "applied", diffs.stream().filter(d -> !d.hasRiskyChanges()).count(),
    "pending", diffs.stream().filter(SchemaDiff::hasRiskyChanges).toList()
  ));
}
```

### Phase 4: Constraint Support ⏳
```yaml
# user.yaml
fields:
  - name: email
    type: email
    unique: true  # ← CREATE UNIQUE INDEX
    
  - name: age
    type: integer
    check: "age >= 0 AND age <= 150"  # ← ADD CHECK CONSTRAINT
    
  - name: status
    type: string
    enum: ["ACTIVE", "INACTIVE"]  # ← CHECK (status IN (...))
```

### Phase 5: Junction Tables ⏳
```yaml
# user.yaml
relationships:
  - name: roles
    type: manyToMany
    targetEntity: Role
    autoGenerateJunctionTable: true  # ← Generator vytvoří user_roles
```

Auto-generated:
```sql
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users_directory(id) ON DELETE CASCADE,
  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);
```

## Porovnání s konkurencí

### Liquibase/Flyway:
- ✅ Version control pro migrace
- ✅ Rollback support
- ❌ Ruční psaní SQL
- ❌ Žádná integrace s runtime validací

### Hibernate hbm2ddl.auto=update:
- ✅ Auto DDL z JPA entities
- ❌ Není spolehlivé v produkci
- ❌ Neumí složité konverze
- ❌ Neumí rollback

### Metamodel Generator v2:
- ✅ Deklarativní YAML definice
- ✅ Runtime + DDL synchronizace
- ✅ Safe/Risky conversion detection
- ✅ Access policies + UI config v jednom
- ⏳ Hot reload (plánované)
- ⏳ Rollback (plánované)

## Závěr

**Současný stav:**
- ✅ Generátor umí vytvořit tabulky z YAML
- ✅ Přidává chybějící sloupce
- ✅ Vytváří indexy a triggery
- ✅ TypeConversionRegistry definuje safe/risky konverze

**Co chybí (priorita P0):**
- ⏳ Detekce změn typu sloupců
- ⏳ Aplikace type conversions (s TypeConversionRegistry)
- ⏳ Junction table generation
- ⏳ Constraint management

**Dlouhodobé cíle:**
- ⏳ Hot reload bez restartu
- ⏳ Data migration hooks v YAML
- ⏳ Rollback support
- ⏳ CLI tool pro schema management

---

**Status:** 🔨 Foundation Complete, v2 Features In Progress  
**Next:** Implementovat schema diff detection + type conversion application
