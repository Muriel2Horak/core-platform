# Pure Metamodel Schema Generation - Implementation Summary

## Co jsme implementovali

### 1. MetamodelSchemaGenerator ✅

**Umístění:** `backend/src/main/java/cz/muriel/core/metamodel/schema/MetamodelSchemaGenerator.java`

**Funkce:**
- ✅ `CREATE TABLE IF NOT EXISTS` z YAML definice
- ✅ `ALTER TABLE ADD COLUMN` pro chybějící sloupce
- ✅ Automatická detekce DB typů (uuid, string, text, timestamp, boolean, long, integer)
- ✅ PRIMARY KEY, NOT NULL, DEFAULT constraints
- ✅ FOREIGN KEY pro manyToOne relationships
- ✅ CREATE INDEX (tenant_id, version, foreign keys)
- ✅ CREATE TRIGGER pro auto-increment version field
- ✅ Conditional activation přes `metamodel.schema.auto-generate`

**Trigger Example:**
```sql
CREATE OR REPLACE FUNCTION increment_users_directory_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version = COALESCE(OLD.version, 0) + 1;
    IF NEW.updated_at IS NOT NULL THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_increment_users_directory_version
    BEFORE UPDATE ON users_directory
    FOR EACH ROW
    EXECUTE FUNCTION increment_users_directory_version();
```

### 2. FieldSchema Enhancement ✅

**Přidáno:**
```java
private Object defaultValue;  // Default value for the field
```

**Podporované default values:**
- `0`, `false`, `true` - pro primitive types
- `'ACTIVE'` - pro stringy
- `NOW()` - pro timestamps
- `gen_random_uuid()` - pro UUID

### 3. Configuration ✅

**application.properties:**
```properties
metamodel.schema.auto-generate=${METAMODEL_SCHEMA_AUTO_GENERATE:false}
```

**application-development.properties:**
```properties
metamodel.schema.auto-generate=true
```

**Docker Environment:**
```yaml
# docker-compose.devcontainer.yml
environment:
  - METAMODEL_SCHEMA_AUTO_GENERATE=true
```

## Testovací Plán

### Test 1: Fresh Database (Clean Start)

```bash
# 1. Smazat databázi
docker compose down -v

# 2. Start s metamodel schema generation
docker compose up -d

# 3. Zkontrolovat logy
docker logs core-backend | grep "Metamodel schema"

# Expected output:
# 🔨 Starting Metamodel schema generation...
# 📋 Processing entity: User
# 🔨 Creating table: users_directory
# ✅ Table created: users_directory
# 📑 Creating indexes for: users_directory
# ⚡ Creating version trigger for: users_directory
# ✅ Version trigger created: trigger_increment_users_directory_version
# ... (stejné pro Role, Group, atd.)
# ✅ Metamodel schema generation completed successfully
```

### Test 2: Existing Database (Schema Evolution)

```bash
# 1. Databáze už existuje s users_directory (bez version)
# 2. Přidat do user.yaml:
#    versionField: version
#    fields:
#      - name: version
#        type: long
#        required: true
#        default: 0

# 3. Restart backendu
docker restart core-backend

# 4. Zkontrolovat logy
# Expected:
# 🔍 Checking for missing columns in: users_directory
# ➕ Adding column version to table users_directory
# ✅ Column added: users_directory.version
# ⚡ Creating version trigger for: users_directory
```

### Test 3: Version Conflict Fix

```bash
# 1. Udělat změnu v Keycloaku (user update)
# 2. CDC event trigger
# 3. Zkontrolovat logy

# Expected (před opravou):
# ❌ Version conflict after 5 retries for user: test

# Expected (po opravě):
# 🔄 Processing CDC event: type=USER_UPDATED
# Updating user test with version 2  ← ✅ Správná verze!
# ✅ CDC event processed successfully
```

### Test 4: Trigger Functionality

```sql
-- Přímý test v databázi
SELECT username, version FROM users_directory WHERE username='test';
-- version=0

UPDATE users_directory SET display_name='Test User' WHERE username='test';

SELECT username, version FROM users_directory WHERE username='test';
-- version=1  ← ✅ Auto-incremented!

UPDATE users_directory SET display_name='Test User 2' WHERE username='test';

SELECT username, version FROM users_directory WHERE username='test';
-- version=2  ← ✅ Works!
```

## Migrace Strategy

### Fáze 1: ✅ Pure Metamodel Test (DEV)

**Cíl:** Ověřit, že Metamodel dokáže spravovat celé schéma

```bash
# 1. Clean start s metamodel.schema.auto-generate=true
docker compose down -v
docker compose up -d

# 2. Kontrola tabulek
docker exec core-db psql -U core -d core -c "\dt"

# 3. Kontrola triggers
docker exec core-db psql -U core -d core -c "
  SELECT trigger_name, event_manipulation, event_object_table 
  FROM information_schema.triggers 
  WHERE trigger_schema='public'
"

# 4. Test CDC events
# - Změnit user v Keycloaku
# - Sledovat logy: docker logs core-backend -f
# - Ověřit version increment
```

### Fáze 2: Sjednotit V1__init.sql

**Po úspěšném testu:**

1. ❌ **ODSTRANIT** JPA entities:
   - `UserDirectoryEntity.java` → SMAZAT
   - `RoleEntity.java` → SMAZAT
   - `GroupEntity.java` → SMAZAT

2. ✅ **UPRAVIT** V1__init.sql:
   - Zachovat CREATE TABLE (pro zpětnou kompatibilitu)
   - Přidat komentář: `-- Tables managed by Metamodel Schema Generator`
   - Nebo úplně odstranit a nechat Metamodel vytvořit vše

3. ✅ **PŘEPSAT** services na Metamodel:
   - `UserService` → používat `MetamodelCrudService`
   - `RoleService` → používat `MetamodelCrudService`
   - `GroupService` → používat `MetamodelCrudService`

### Fáze 3: Production Rollout

```bash
# 1. Backup produkční DB
pg_dump -h prod-db -U core core > backup_before_metamodel.sql

# 2. Deploy s metamodel.schema.auto-generate=false (!)
# Metamodel jen validuje, nemění schéma

# 3. Manual migration pokud nutné
# Spustit generated SQL manuálně

# 4. Verify
# Kontrola že vše funguje

# 5. Enable auto-generate pro následující deploye
```

## Výhody Pure Metamodel Approach

✅ **Single Source of Truth:** YAML definice řídí vše  
✅ **Žádná duplicita:** Konec konfliktu JPA vs Metamodel  
✅ **Auto-migration:** Přidání pole = automatické ALTER TABLE  
✅ **Konzistence:** Version triggers vždy synchronní s definicí  
✅ **Testability:** Snadné otestování metamodel functionality  
✅ **Flexibility:** Snadné přidání nových entit bez Java kódu  

## Rizika a Mitigace

### ⚠️ Riziko: Data Loss při špatném DDL

**Mitigace:**
- Metamodel NIKDY nedělá DROP COLUMN
- Pouze ADD COLUMN (non-destructive)
- Produkce: auto-generate=false, manual review migrations

### ⚠️ Riziko: Downtime při ALTER TABLE

**Mitigace:**
- PostgreSQL ADD COLUMN je fast (metadata only)
- ADD INDEX CONCURRENTLY pro velké tabulky
- Blue-green deployment strategy

### ⚠️ Riziko: Trigger performance

**Mitigace:**
- Triggers jsou minimální (jen version++)
- PostgreSQL triggers jsou velmi fast
- Monitoring query performance

## Rollback Plan

Pokud metamodel selže:

```bash
# 1. Vypnout auto-generate
METAMODEL_SCHEMA_AUTO_GENERATE=false

# 2. Restore z backupu
psql -U core core < backup_before_metamodel.sql

# 3. Vrátit se na JPA entities
git revert <commit-hash>

# 4. Deploy starší verze
```

## Monitoring

```bash
# Kontrola schema generation logů
docker logs core-backend | grep -E "(Metamodel schema|Creating table|Adding column|Creating trigger)"

# Kontrola version increments
docker logs core-backend | grep -E "(Updating.*with version|Version conflict)"

# DB monitoring
SELECT 
  schemaname,
  tablename,
  attname as column_name,
  pg_typeof(attname) as data_type
FROM pg_stats 
WHERE tablename IN ('users_directory', 'roles', 'groups');

# Trigger execution stats
SELECT 
  schemaname, tablename, 
  tgname as trigger_name,
  tgenabled as enabled
FROM pg_trigger t
JOIN pg_class c ON t.tgrelid = c.oid
JOIN pg_namespace n ON c.relnamespace = n.oid
WHERE n.nspname = 'public';
```

## Next Steps

1. ✅ Zkompilováno
2. ⏳ Clean restart DB
3. ⏳ Test schema generation
4. ⏳ Test version triggers
5. ⏳ Test CDC events
6. ⏳ Remove JPA entities
7. ⏳ Update documentation

---

**Status:** 🚀 Ready for Testing  
**Risks:** Medium (test thoroughly in DEV)  
**Effort:** Low (infrastructure ready)  
**Impact:** High (eliminates dual-system conflict)
