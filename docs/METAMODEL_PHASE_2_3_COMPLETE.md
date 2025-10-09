# 🚀 Metamodel Phase 2 & 3: Hot Reload API + Constraints - COMPLETE

**Status:** ✅ IMPLEMENTED & COMPILED  
**Date:** 2025-10-09  
**Features:** REST API for metamodel management, UNIQUE constraints

---

## 📋 Overview

Phase 2 a 3 přidávají:

1. **REST API pro správu Metamodelu** (Phase 2)
   - Hot reload YAML definic bez restartu
   - Detekce změn schématu přes API
   - Aplikace bezpečných změn

2. **UNIQUE Constraint Management** (Phase 3)
   - Automatické vytváření UNIQUE constraints z YAML
   - Detekce existujících constraints
   - Safe application (skipuje pokud už existuje)

---

## 🔌 REST API Endpoints

### 1. GET `/api/admin/metamodel/reload`
Reload YAML metamodel definitions a detekuj změny schématu.

**Response:**
```json
{
  "status": "success",
  "message": "Metamodel reloaded successfully",
  "entitiesCount": 3,
  "changesDetected": 1,
  "changes": {
    "User": {
      "tableName": "users_directory",
      "totalChanges": 2,
      "hasRiskyChanges": false,
      "safeChanges": 2,
      "riskyChanges": 0,
      "details": [
        {
          "type": "ADD",
          "column": "new_field",
          "risky": "false",
          "newType": "VARCHAR(255)"
        }
      ]
    }
  }
}
```

**Use Case:**  
Po úpravě YAML souboru volejte tento endpoint pro ověření, jaké změny by byly aplikovány.

---

### 2. POST `/api/admin/metamodel/apply-safe-changes`
Aplikuje všechny bezpečné změny detekované z YAML.

**Response:**
```json
{
  "status": "success",
  "message": "Safe schema changes applied successfully"
}
```

**Behavior:**
- ✅ Přidá nové sloupce (ADD COLUMN)
- ✅ Vytvoří nové indexy
- ✅ Vytvoří UNIQUE constraints
- ⚠️ Skipne rizikovéoperace (type conversions, NOT NULL)

**Use Case:**  
Po `/reload` a verifikaci změn použijte tento endpoint k aplikaci bezpečných změn.

---

### 3. GET `/api/admin/metamodel/status`
Zobrazí aktuální stav metamodelu a pending změny.

**Response:**
```json
{
  "status": "success",
  "entitiesCount": 3,
  "entities": ["User", "Role", "Group"],
  "pendingChanges": 0,
  "changes": {}
}
```

**Use Case:**  
Health check - zjistěte, zda jsou YAML definice synchronizované s DB schématem.

---

## 🔒 UNIQUE Constraints

### Deklarace v YAML

```yaml
# user.yaml
fields:
  - name: username
    type: string
    maxLength: 100
    required: true
    unique: true  # ← UNIQUE constraint
  
  - name: email
    type: email
    maxLength: 255
    unique: true  # ← UNIQUE constraint
```

### Automatické Vytvoření

Při startu backendu nebo při `/apply-safe-changes`:

```sql
ALTER TABLE users_directory 
ADD CONSTRAINT uk_users_directory_username UNIQUE (username);

ALTER TABLE users_directory 
ADD CONSTRAINT uk_users_directory_email UNIQUE (email);
```

### Naming Convention
- Pattern: `uk_{table_name}_{column_name}`
- Příklad: `uk_users_directory_username`

### Safety
- ✅ Zkontroluje existenci před vytvořením
- ✅ Skipne pokud constraint už existuje
- ⚠️ Loguje warning pokud vytvoření selže

---

## 💡 Usage Examples

### Scenario 1: Přidání nového pole

**1. Upravte YAML:**
```yaml
# user.yaml
fields:
  - name: phone_number  # ← NOVÝ FIELD
    type: string
    maxLength: 20
```

**2. Reload metamodel:**
```bash
curl http://localhost:8080/api/admin/metamodel/reload
```

**3. Zkontrolujte response:**
```json
{
  "changesDetected": 1,
  "changes": {
    "User": {
      "safeChanges": 1,
      "details": [{"type": "ADD", "column": "phone_number"}]
    }
  }
}
```

**4. Aplikujte změny:**
```bash
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes
```

**Result:** Column `phone_number` přidán bez restartu! ✅

---

### Scenario 2: Přidání UNIQUE constraint

**1. Upravte YAML:**
```yaml
# role.yaml
fields:
  - name: name
    type: string
    maxLength: 100
    unique: true  # ← PŘIDÁNO
```

**2. Reload + Apply:**
```bash
curl http://localhost:8080/api/admin/metamodel/reload
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes
```

**Result:** UNIQUE constraint vytvořen ✅

---

### Scenario 3: Monitoring změn

**Periodický health check:**
```bash
# Každých 5 minut zkontroluj pending změny
*/5 * * * * curl http://localhost:8080/api/admin/metamodel/status | jq '.pendingChanges'
```

**Alert pokud pendingChanges > 0:**
```
⚠️ Schema drift detected! 
   YAML definitions don't match DB schema.
   Run /reload to review changes.
```

---

## 🏗️ Architecture

### Component Diagram

```
┌─────────────────────────────────────┐
│  MetamodelAdminController           │
│  (REST API Layer)                   │
└───────────┬─────────────────────────┘
            │
            ├──> MetamodelRegistry.reload()
            │    └─> MetamodelLoader.loadSchemas()
            │
            ├──> MetamodelSchemaGenerator.detectChanges()
            │    └─> getCurrentColumns() from information_schema
            │
            └──> MetamodelSchemaGenerator.generateSchema()
                 ├─> createTable()
                 ├─> applyChanges()
                 ├─> createIndexes()
                 ├─> createUniqueConstraints()  ← NEW!
                 └─> createVersionTrigger()
```

### Call Flow: Hot Reload

```
1. User edits user.yaml
2. POST /api/admin/metamodel/reload
3. MetamodelRegistry.reload()
   └─> Clears cache, reloads all YAML files
4. MetamodelSchemaGenerator.detectChanges()
   └─> Queries information_schema
   └─> Compares YAML fields vs DB columns
   └─> Returns SchemaDiff
5. Response built with change summary
6. User reviews changes
7. POST /api/admin/metamodel/apply-safe-changes
8. MetamodelSchemaGenerator.generateSchema()
   └─> Applies ADD COLUMN, CREATE INDEX, CREATE CONSTRAINT
9. Success response
```

---

## 🔍 Implementation Details

### MetamodelAdminController.java

**Key Methods:**

```java
@GetMapping("/reload")
public ResponseEntity<Map<String, Object>> reloadMetamodel() {
  // 1. Reload YAML
  registry.reload();
  
  // 2. Detect changes
  Map<String, SchemaDiff> diffs = new HashMap<>();
  for (var entry : registry.getAllSchemas().entrySet()) {
    SchemaDiff diff = schemaGenerator.detectChanges(entry.getValue());
    if (!diff.getColumnChanges().isEmpty()) {
      diffs.put(entry.getKey(), diff);
    }
  }
  
  // 3. Build response
  return ResponseEntity.ok(buildResponse(diffs));
}
```

**Response Building:**

```java
private Map<String, Object> buildChangeSummary(Map<String, SchemaDiff> diffs) {
  for (var entry : diffs.entrySet()) {
    // Categorize safe vs risky changes
    long safeChanges = diff.getColumnChanges().stream()
        .filter(c -> !c.isRisky())
        .count();
    
    // Build detailed change list
    for (var change : diff.getColumnChanges()) {
      changeInfo.put("type", change.getType().toString());
      changeInfo.put("column", change.getColumnName());
      changeInfo.put("risky", String.valueOf(change.isRisky()));
    }
  }
}
```

---

### UNIQUE Constraint Implementation

**createUniqueConstraints():**

```java
private void createUniqueConstraints(EntitySchema schema) {
  log.debug("🔒 Creating UNIQUE constraints for: {}", schema.getTable());
  
  for (FieldSchema field : schema.getFields()) {
    if (Boolean.TRUE.equals(field.getUnique())) {
      createUniqueConstraint(schema.getTable(), field.getName());
    }
  }
}
```

**createUniqueConstraint():**

```java
private void createUniqueConstraint(String tableName, String columnName) {
  String constraintName = "uk_" + tableName + "_" + columnName;
  
  // Check existence
  Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, 
      tableName, constraintName);
  
  if (Boolean.TRUE.equals(exists)) {
    log.debug("✅ UNIQUE constraint already exists: {}", constraintName);
    return;
  }
  
  // Create if not exists
  String sql = String.format("ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)", 
      tableName, constraintName, columnName);
  
  jdbcTemplate.execute(sql);
  log.debug("✅ UNIQUE constraint created: {}", constraintName);
}
```

---

## ✅ Testing

### Manual Testing

**1. Test hot reload:**
```bash
# Edit user.yaml - add new field
# Then:
curl http://localhost:8080/api/admin/metamodel/reload

# Expected: changesDetected: 1
```

**2. Test constraint creation:**
```bash
# Add unique: true to username field in user.yaml
# Then:
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes

# Check DB:
psql -d core -c "\d users_directory"
# Expected: uk_users_directory_username constraint listed
```

**3. Test status endpoint:**
```bash
curl http://localhost:8080/api/admin/metamodel/status | jq

# Expected: 
# {
#   "entitiesCount": 3,
#   "entities": ["User", "Role", "Group"],
#   "pendingChanges": 0
# }
```

### Integration Testing

```java
@Test
void reloadMetamodel_withNewField_detectsChange() {
  // Given: user.yaml has new field
  
  // When
  var response = adminController.reloadMetamodel();
  
  // Then
  assertThat(response.getBody().get("changesDetected")).isEqualTo(1);
}

@Test
void applyChanges_withUniqueField_createsConstraint() {
  // Given: username has unique: true
  
  // When
  adminController.applySafeChanges();
  
  // Then
  var constraints = jdbcTemplate.queryForList(
      "SELECT constraint_name FROM information_schema.table_constraints " +
      "WHERE table_name = 'users_directory' AND constraint_type = 'UNIQUE'"
  );
  
  assertThat(constraints).contains("uk_users_directory_username");
}
```

---

## 🎯 Benefits

### For Development
- ✅ **No restarts needed** - změny aplikovány za běhu
- ✅ **Fast iteration** - úprava YAML → reload → test
- ✅ **Safe changes** - automatic detection of risky operations

### For Operations
- ✅ **Schema monitoring** - `/status` endpoint pro health checks
- ✅ **Audit trail** - všechny změny logované
- ✅ **Gradual rollout** - reload detekuje, apply aplikuje

### For Data Integrity
- ✅ **UNIQUE constraints** - automaticky z YAML
- ✅ **No duplicates** - DB enforced uniqueness
- ✅ **Consistent** - same constraints across environments

---

## 📝 Next Steps (Phase 4)

### Advanced Constraint Management
1. **CHECK constraints** from YAML validation rules
2. **Foreign key constraints** from relationships
3. **Junction tables** for M:N relationships
4. **Composite UNIQUE** constraints

### Example YAML:
```yaml
fields:
  - name: age
    type: integer
    validation:
      min: 0
      max: 150
    # → CHECK (age >= 0 AND age <= 150)

  - name: status
    type: string
    validation:
      enum: [ACTIVE, INACTIVE, PENDING]
    # → CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING'))

constraints:
  - type: unique
    columns: [tenant_id, username]
    # → UNIQUE (tenant_id, username)
```

---

## 🎉 Summary

**Phase 2 & 3 COMPLETE!**

**Implemented:**
- ✅ REST API: `/reload`, `/apply-safe-changes`, `/status`
- ✅ Hot reload without restart
- ✅ UNIQUE constraint auto-creation
- ✅ Safe change detection
- ✅ Detailed change reporting

**Files Changed:**
- `MetamodelAdminController.java` (NEW) - 200 lines
- `MetamodelSchemaGenerator.java` (UPDATED) - added createUniqueConstraints()
- Compilation: ✅ SUCCESS (164 files)

**Ready for production testing!** 🚀
