# Metamodel & Database Synchronization Strategy

## Problém: Version Conflict v Metamodelu

**Root Cause:** Tabulka `users_directory` neměla sloupec `version` pro optimistické zamykání, což vedlo k version conflict chybám v CDC processing.

## Současný Stav (Hybridní Přístup)

### 1. Definice Schémat

**Metamodel YAML** (`backend/src/main/resources/metamodel/*.yaml`):
- Definuje strukturu entit
- Access policies (ABAC)
- UI configuration
- Lifecycle hooks
- Relationships
- **Pole `versionField` pro optimistické zamykání**

**Příklad:**
```yaml
entity: User
table: users_directory
idField: id
tenantField: tenant_id
versionField: version  # ✅ Přidáno pro optimistic locking

fields:
  - name: version
    type: long
    required: true
    default: 0
```

### 2. Vytváření DB Struktur

#### A) Inicializace (V1__init.sql)
- **Ruční DDL** pro základní tabulky:
  - `tenants`
  - `users_directory`
  - `roles`
  - `groups`
  - `user_roles` (junction table)
  - `user_groups` (junction table)
  - `role_composites`
  - `role_hierarchy`

#### B) Migrace (V1.x__*.sql)
- **Flyway migrations** pro změny schématu
- Přidávání sloupců, indexů, constraints
- **Příklad:** `V1.6__add_version_to_users_directory.sql`

```sql
ALTER TABLE users_directory 
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_users_directory_version ON users_directory(version);

CREATE OR REPLACE FUNCTION increment_user_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_user_version
    BEFORE UPDATE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION increment_user_version();
```

### 3. Jak Metamodel Pracuje s DB

#### Runtime Validace (NE DDL)
```java
@Component
public class MetamodelRegistry {
  @PostConstruct
  public void init() {
    reload(); // Načte YAML schemas
  }
}
```

**Metamodel NEAPLIKUJE změny do DB!** Pouze:
- ✅ Načítá YAML definice při startu
- ✅ Validuje data při CRUD operacích
- ✅ Aplikuje access policies
- ✅ Provádí lifecycle hooks
- ✅ Kontroluje verzování při UPDATE

#### Optimistické Zamykání

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Map<String, Object> update(String entityType, String id, long expectedVersion,
    Map<String, Object> data, Authentication auth) {
  
  // Load current entity
  Object entity = findEntityById(schema, id);
  
  // Check version
  Long currentVersion = extractVersion(entity, schema);
  if (currentVersion == null || currentVersion != expectedVersion) {
    throw new VersionMismatchException("Version mismatch", currentVersion, ...);
  }
  
  // Build UPDATE with version check
  String updateSql = String.format(
    "UPDATE %s SET %s WHERE %s = '%s' AND %s = %d",
    schema.getTable(), sets, schema.getIdField(), id, 
    schema.getVersionField(), expectedVersion
  );
  
  // Trigger automatically increments version
  int affected = entityManager.createNativeQuery(updateSql).executeUpdate();
  
  if (affected == 0) {
    throw new VersionMismatchException("Update failed - version mismatch", ...);
  }
}
```

## Současné Role, Groups a Vazby

### Tabulky
```sql
roles (id, tenant_id, name, description, version, ...)
groups (id, tenant_id, name, description, version, ...)
user_roles (user_id, role_id)  -- M:N junction
user_groups (user_id, group_id) -- M:N junction
role_composites (parent_role_id, child_role_id)
role_hierarchy (parent_role_id, child_role_id, depth)
```

### Validace Metamodelem

**Role.yaml:**
```yaml
entity: Role
table: roles
versionField: version

relationships:
  - name: users
    type: manyToMany
    targetEntity: User
    joinTable: user_roles
    joinColumn: role_id
    inverseJoinColumn: user_id
```

**Group.yaml:**
```yaml
entity: Group
table: groups
versionField: version

relationships:
  - name: users
    type: manyToMany
    targetEntity: User
    joinTable: user_groups
    joinColumn: group_id
    inverseJoinColumn: user_id
```

Metamodel:
- ✅ Validuje existenci junction tables při načítání
- ✅ Automaticky spravuje M:N vztahy přes `RelationshipResolver`
- ✅ Kontroluje verze při updatech
- ❌ NEVYTVÁŘÍ tabulky

## Kdy Metamodel Aplikuje Changes?

### Nikdy (Současný Stav)
Metamodel **neprovádí DDL operace**. Pouze:
1. **Startup:** Načte všechny `*.yaml` schemas
2. **Runtime:** Validuje CRUD operace proti loaded schemas
3. **Error:** Pokud DB schéma neodpovídá YAML, operace selže

### Jak Zjistit Rozdíly?

```bash
# 1. Kontrola sloupců v DB
docker exec core-db psql -U core -d core -c "\d users_directory"

# 2. Kontrola YAML definice
cat backend/src/main/resources/metamodel/user.yaml

# 3. Manuální porovnání
```

## Řešení Version Conflict

### ✅ Co jsme opravili:

1. **YAML Schema** - přidán `versionField`:
```yaml
entity: User
versionField: version
fields:
  - name: version
    type: long
    required: true
    default: 0
```

2. **DB Migration** - přidán sloupec + trigger:
```sql
-- V1.6__add_version_to_users_directory.sql
ALTER TABLE users_directory ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TRIGGER trigger_increment_user_version ...
```

3. **MetamodelCrudService** - oprava čtení verzí:
```java
private Object findEntityById(EntitySchema schema, String id) {
  // ✅ Explicitně specifikujeme pořadí sloupců
  List<String> columns = schema.getFields().stream()
      .map(FieldSchema::getName)
      .collect(Collectors.toList());
  
  String columnList = String.join(", ", columns);
  String sql = String.format("SELECT %s FROM %s WHERE %s = :id", 
      columnList, schema.getTable(), schema.getIdField());
  
  return entityManager.createNativeQuery(sql)
      .setParameter("id", UUID.fromString(id))
      .getSingleResult();
}
```

### Proč to bylo potřeba?

**Problém:** `SELECT * FROM users_directory` vrací sloupce v **DB pořadí** (často alfabetické), ale `schema.getFields()` vrací pole v **YAML pořadí**.

**Důsledek:** Sloupec `version` se mapoval na špatný index v `Object[]`, vždy se četla hodnota `0`.

**Řešení:** Explicitní SELECT s pořadím sloupců podle YAML schema.

## Budoucí Směr - Auto Schema Migration

### Option 1: Liquibase/Flyway Generator
```java
@Component
public class MetamodelSchemaGenerator {
  
  public void generateMigration() {
    for (EntitySchema schema : registry.getAllSchemas().values()) {
      String ddl = generateDDL(schema);
      writeMigration(ddl);
    }
  }
}
```

### Option 2: JPA Entities + Hibernate DDL
Generovat JPA entity classes z YAML a použít Hibernate's `hbm2ddl.auto=update`.

### Option 3: Schema Diff Tool
```bash
./metamodel diff  # Compare YAML vs DB schema
./metamodel apply # Generate migration SQL
```

## Best Practices

### ✅ DO:
1. **Vždy definuj `versionField`** pro entity, které se mění přes CDC
2. **Používej Flyway migrations** pro všechny DB změny
3. **Synchronizuj YAML + SQL** manuálně (zatím)
4. **Testuj optimistic locking** po každé změně

### ❌ DON'T:
1. **Nespoléhej na `SELECT *`** - vždy specifikuj sloupce
2. **Neprovádíš ruční ALTER** bez migrace
3. **Neočekávej auto-sync** YAML -> DB (zatím není implementováno)

## Testing

```bash
# 1. Restart backend s novou migrací
make rebuild-backend

# 2. Kontrola verze v DB
docker exec core-db psql -U core -d core -c \
  "SELECT username, version FROM users_directory LIMIT 5"

# 3. Trigger CDC event (změň user v Keycloak)

# 4. Kontrola logů
docker logs core-backend | grep -E "(version|Version conflict)"
```

## Summary

- **Metamodel = Runtime Validation Layer**, NE DDL engine
- **Flyway = Source of Truth** pro DB schema
- **YAML + SQL musí být synchronizované** manuálně
- **Version field je kritický** pro CDC + optimistic locking
- **Budoucnost:** Auto-generation DB migrations z YAML

---

**Opraveno:** 2025-10-09  
**Status:** ✅ Version conflict v CDC vyřešen
