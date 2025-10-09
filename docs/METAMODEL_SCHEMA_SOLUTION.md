# Metamodel Schema Management - Complete Solution

## Současný Problém

### Dual System Conflict
1. **JPA Entities** (`UserDirectoryEntity`, `RoleEntity`, `GroupEntity`) - Hibernate managed
2. **Metamodel YAML** (`user.yaml`, `role.yaml`, `group.yaml`) - Native SQL queries
3. **❌ ŽÁDNÁ SYNCHRONIZACE** mezi JPA a Metamodel
4. **❌ JPA entities NEMAJÍ `@Version`** anotaci
5. **❌ DB triggers pro version NEJSOU** auto-generované

### Co se děje nyní?

```
V1__init.sql
  ↓ (vytvoří tabulky + version sloupec)
DB Schema
  ↓ (používají různé systémy)
JPA Entities ← Hibernate CRUD (BEZ @Version!)
Metamodel ← Native SQL (očekává triggers!)
```

**Důsledek:** Version conflict v CDC processing, protože:
- Metamodel očekává DB trigger pro auto-increment version
- JPA entity nemá `@Version`, takže Hibernate trigger nevytvoří
- V1__init.sql má sloupec `version`, ale ŽÁDNÝ TRIGGER!

## Řešení: Dual-Track with Auto-Sync

### Fáze 1: Opravit JPA Entities (✅ DONE)

Přidat `@Version` do všech entit s verzováním:

```java
@Entity
@Table(name = "users_directory")
public class UserDirectoryEntity extends MultiTenantEntity {
  
  @Version  // ← ✅ KLÍČOVÉ pro Hibernate optimistic locking
  @Column(name = "version", nullable = false)
  private Long version;
  
  // ... rest
}
```

### Fáze 2: Vytvořit DB Triggers v V1__init.sql

**Aktuální stav:**
```sql
-- V1__init.sql má pouze sloupec
CREATE TABLE users_directory (
  version BIGINT DEFAULT 0 NOT NULL,
  -- ...
);
```

**✅ OPRAVA - Přidat triggery:**
```sql
-- Auto-increment version trigger for users_directory
CREATE OR REPLACE FUNCTION increment_user_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = COALESCE(OLD.version, 0) + 1;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_user_version
    BEFORE UPDATE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION increment_user_version();

-- Similarly for roles
CREATE OR REPLACE FUNCTION increment_role_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = COALESCE(OLD.version, 0) + 1;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_role_version
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION increment_role_version();

-- Similarly for groups
CREATE OR REPLACE FUNCTION increment_group_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = COALESCE(OLD.version, 0) + 1;
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_group_version
    BEFORE UPDATE ON groups
    FOR EACH ROW
    EXECUTE FUNCTION increment_group_version();
```

### Fáze 3: Schema Validation Tool

Vytvořit utility pro kontrolu YAML vs DB:

```java
@Component
public class MetamodelSchemaValidator {
  
  @Autowired
  private MetamodelRegistry registry;
  
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  @PostConstruct
  public void validateSchemas() {
    for (EntitySchema schema : registry.getAllSchemas().values()) {
      validateSchema(schema);
    }
  }
  
  private void validateSchema(EntitySchema schema) {
    // 1. Check table exists
    // 2. Check all fields exist
    // 3. Check version field exists if versionField is set
    // 4. Check indexes
    // 5. Check triggers for version field
    
    List<String> issues = new ArrayList<>();
    
    // Check version trigger
    if (schema.getVersionField() != null) {
      String triggerName = "trigger_increment_" + schema.getTable().replace("_", "");
      if (!triggerExists(triggerName)) {
        issues.add("Missing trigger: " + triggerName);
      }
    }
    
    if (!issues.isEmpty()) {
      log.error("❌ Schema validation failed for {}: {}", 
          schema.getEntity(), issues);
      throw new IllegalStateException("Schema mismatch detected!");
    }
  }
}
```

### Fáze 4: Schema Migration Generator (Budoucnost)

```java
@Component
public class MetamodelSchemaGenerator {
  
  public void generateMigration(String version) {
    StringBuilder sql = new StringBuilder();
    sql.append("-- V").append(version).append("__metamodel_sync.sql\n\n");
    
    for (EntitySchema schema : registry.getAllSchemas().values()) {
      // Generate CREATE TABLE if not exists
      sql.append(generateCreateTable(schema));
      
      // Generate triggers
      if (schema.getVersionField() != null) {
        sql.append(generateVersionTrigger(schema));
      }
      
      // Generate indexes
      sql.append(generateIndexes(schema));
    }
    
    // Write to file
    Path migrationFile = Paths.get("src/main/resources/db/migration/V" 
        + version + "__metamodel_sync.sql");
    Files.writeString(migrationFile, sql.toString());
  }
  
  private String generateVersionTrigger(EntitySchema schema) {
    String tableName = schema.getTable();
    String functionName = "increment_" + tableName.replace("_", "") + "_version";
    String triggerName = "trigger_" + functionName;
    
    return String.format("""
        CREATE OR REPLACE FUNCTION %s()
        RETURNS TRIGGER AS $$
        BEGIN
            NEW.%s = COALESCE(OLD.%s, 0) + 1;
            NEW.updated_at = NOW();
            RETURN NEW;
        END;
        $$ LANGUAGE plpgsql;
        
        DROP TRIGGER IF EXISTS %s ON %s;
        CREATE TRIGGER %s
            BEFORE UPDATE ON %s
            FOR EACH ROW
            EXECUTE FUNCTION %s();
        
        """, 
        functionName, 
        schema.getVersionField(), schema.getVersionField(),
        triggerName, tableName,
        triggerName, tableName,
        functionName
    );
  }
}
```

## Doporučený Workflow

### Pro DEV (Clean Start)

```bash
# 1. Drop všechno
docker compose down -v

# 2. V1__init.sql obsahuje:
#    - CREATE TABLE s version sloupcem
#    - CREATE TRIGGER pro auto-increment version
#    - CREATE INDEX
make start-dev

# 3. Backend startup:
#    - Hibernate validuje JPA entities (@Version maping)
#    - MetamodelSchemaValidator kontroluje YAML vs DB
#    - Vše musí sedět nebo FAIL!
```

### Pro PROD (Migrace)

```bash
# 1. Zjisti současný stav
./gradlew generateSchemaDiff

# 2. Vygeneruje migraci
# V1.7__add_version_triggers.sql

# 3. Review + commit

# 4. Deploy spustí Flyway
```

## CLI Tool (Budoucnost)

```bash
# Validate current state
./mvnw exec:java -Dexec.mainClass="cz.muriel.core.MetamodelCLI" -Dexec.args="validate"

# Generate diff between YAML and DB
./mvnw exec:java -Dexec.mainClass="cz.muriel.core.MetamodelCLI" -Dexec.args="diff"

# Generate migration SQL
./mvnw exec:java -Dexec.mainClass="cz.muriel.core.MetamodelCLI" -Dexec.args="generate-migration 1.7"

# Apply (dangerous!)
./mvnw exec:java -Dexec.mainClass="cz.muriel.core.MetamodelCLI" -Dexec.args="apply"
```

## Kdo Vytváří Co?

### V1__init.sql (Source of Truth)
- ✅ CREATE TABLE
- ✅ CREATE INDEX
- ✅ CREATE TRIGGER (version auto-increment)
- ✅ CREATE FUNCTION
- ✅ INSERT seed data

### JPA Entity (@Entity)
- ✅ Hibernate entity mapping
- ✅ @Version pro optimistic locking
- ✅ @ManyToMany relationships
- ⚠️ **Hibernate hbm2ddl.auto = validate** (NE update!)

### Metamodel YAML
- ✅ Runtime validation
- ✅ Access policies
- ✅ UI configuration
- ✅ Lifecycle hooks
- ❌ NEPROVÁDÍ DDL

### Flyway Migrations (V1.x__*.sql)
- ✅ ALTER TABLE
- ✅ ADD COLUMN
- ✅ CREATE TRIGGER (nové)
- ✅ Data transformations

## Konfliktní Scenario

### ❌ ŠPATNĚ (současný stav):
```
V1__init.sql: CREATE TABLE users_directory (version BIGINT)
             (žádný trigger!)

UserDirectoryEntity: (žádná @Version!)

Metamodel user.yaml: versionField: version

MetamodelCrudService: Očekává trigger pro increment
                      ↓ (FAIL!)
                      Version vždy 0, version conflict!
```

### ✅ SPRÁVNĚ:
```
V1__init.sql: CREATE TABLE + CREATE TRIGGER
              ↓
DB Schema: tabulka + trigger

JPA Entity: @Version
           ↓
Hibernate: Generuje správný SQL s version check

Metamodel YAML: versionField: version
                ↓
MetamodelCrudService: Používá trigger, funguje!
```

## Akční Plán

### Krok 1: Opravit JPA Entity ✅
```java
@Version
@Column(name = "version", nullable = false)
private Long version = 0L;
```

### Krok 2: Vytvořit V1.7__add_version_triggers.sql
- Pro users_directory
- Pro roles
- Pro groups

### Krok 3: Změnit Hibernate config
```java
properties.put("hibernate.hbm2ddl.auto", "validate"); // NE update!
```

### Krok 4: Implementovat MetamodelSchemaValidator
- Kontrola při startu
- FAIL pokud nesedí

### Krok 5: Clean restart
```bash
docker compose down -v
make start-dev
# Nyní vše funguje!
```

## Závěr

**Metamodel NEAPLIKUJE diff automaticky!**

✅ Správný přístup:
1. V1__init.sql = Single source of truth pro DDL
2. JPA Entity = Hibernate mapping + @Version
3. Metamodel YAML = Runtime policies + validation
4. Validator při startu = Kontrola konzistence
5. Flyway migrations = Všechny změny schématu

**Budoucnost:**
- Auto-generation migrations z YAML diff
- CLI tool pro schema management
- Zero-downtime migrations

---
**Status:** 🔨 Work in Progress
**Priorita:** P0 - Critical
**Deadline:** ASAP (version conflicts blokují CDC)
