# W9: Workflow Versioning - Implementation Complete ✅

**Datum:** 2025-01-14  
**Status:** ✅ Core Implementation Complete

## 📋 Přehled

W9 zavádí **versioning systém pro workflow schémata**, který umožňuje:

- ✅ Verzování workflow definic
- ✅ Aktivaci/deaktivaci verzí
- ✅ Migraci instancí mezi verzemi
- ✅ Historii migrace
- ✅ Strategii migrace (IMMEDIATE, LAZY, MANUAL)

---

## 🗄️ Database Schema (V4 Migration)

### Tabulky

#### `workflow_versions`
Ukládá verze workflow schémat:

```sql
CREATE TABLE workflow_versions (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,
    schema_definition JSONB NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT false,
    migration_notes TEXT,
    
    CONSTRAINT uq_workflow_version UNIQUE (entity_type, version)
);
```

**Účel:**
- `entity_type` - typ entity (ORDER, INVOICE, ...)
- `version` - číslo verze (auto-increment per entity_type)
- `schema_definition` - JSONB definice workflow (states, transitions, guards, actions)
- `is_active` - jenom jedna verze per entity_type může být aktivní

#### `workflow_instance_versions`
Mapování instancí na verze:

```sql
CREATE TABLE workflow_instance_versions (
    workflow_instance_id BIGINT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    migrated_from_version_id BIGINT,
    migrated_at TIMESTAMP,
    
    FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id),
    FOREIGN KEY (version_id) REFERENCES workflow_versions(id),
    FOREIGN KEY (migrated_from_version_id) REFERENCES workflow_versions(id)
);
```

**Účel:**
- Každá instance má přiřazenu verzi
- Historie migrace (odkud se instance přesunula)

#### `workflow_version_migrations`
Audit log bulk migrací:

```sql
CREATE TABLE workflow_version_migrations (
    id BIGSERIAL PRIMARY KEY,
    from_version_id BIGINT NOT NULL,
    to_version_id BIGINT NOT NULL,
    migration_strategy VARCHAR(50) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    instances_migrated INTEGER DEFAULT 0,
    instances_failed INTEGER DEFAULT 0,
    
    FOREIGN KEY (from_version_id) REFERENCES workflow_versions(id),
    FOREIGN KEY (to_version_id) REFERENCES workflow_versions(id)
);
```

---

## 🔧 Backend Services

### `WorkflowVersionService`

Hlavní service pro správu verzí.

#### Metody

**1. Vytvoření verze**
```java
Long createVersion(String entityType, JsonNode schemaDefinition, String createdBy, String notes)
```
- Auto-increment čísla verze per entity_type
- Uložení JSONB schématu
- Vratí ID nové verze

**2. Aktivace verze**
```java
void activateVersion(Long versionId)
```
- Deaktivuje všechny ostatní verze pro daný entity_type
- Aktivuje vybranou verzi
- Nové instance budou používat tuto verzi

**3. Získání aktivní verze**
```java
Optional<WorkflowVersion> getActiveVersion(String entityType)
```

**4. Migrace instance**
```java
void migrateInstance(Long instanceId, Long toVersionId, MigrationStrategy strategy)
```
- Přesune instanci na novou verzi
- Uloží historii (migrated_from_version_id)
- Podporuje strategie:
  - `IMMEDIATE` - hned provést
  - `LAZY` - až při příštím přístupu
  - `MANUAL` - vyžaduje manuální zásah

**5. Bulk migrace**
```java
Long startMigration(Long fromVersionId, Long toVersionId, MigrationStrategy strategy, String createdBy)
```
- Inicializuje hromadnou migraci
- Vrací migration ID pro tracking

### `WorkflowVersionController`

REST API endpointy (všechny vyžadují `ROLE_WORKFLOW_ADMIN`):

```
POST   /api/v1/workflows/versions                    - Vytvoří verzi
POST   /api/v1/workflows/versions/{id}/activate      - Aktivuje verzi
GET    /api/v1/workflows/versions/active/{entityType}- Aktivní verze
GET    /api/v1/workflows/versions/{id}               - Konkrétní verze
GET    /api/v1/workflows/versions/entity/{entityType}- Všechny verze
POST   /api/v1/workflows/versions/migrate            - Migruj instanci
POST   /api/v1/workflows/versions/migrate/bulk       - Bulk migrace
GET    /api/v1/workflows/versions/migrations/instance/{id} - Historie migrace
```

#### Příklad: Vytvoření verze

**Request:**
```json
POST /api/v1/workflows/versions
{
  "entityType": "ORDER",
  "schemaDefinition": {
    "states": ["DRAFT", "SUBMITTED", "APPROVED", "CANCELLED"],
    "transitions": [
      {"from": "DRAFT", "to": "SUBMITTED", "event": "submit"},
      {"from": "SUBMITTED", "to": "APPROVED", "event": "approve"},
      {"from": "SUBMITTED", "to": "CANCELLED", "event": "cancel"}
    ]
  },
  "createdBy": "admin",
  "notes": "Added CANCELLED state"
}
```

**Response:**
```json
123  // version ID
```

#### Příklad: Aktivace verze

```
POST /api/v1/workflows/versions/123/activate
```

---

## 🧪 Testy

### Unit Tests: `WorkflowVersionServiceTest`

**Testované scénáře:**

1. ✅ **Auto-increment verzí** - version 1, 2, 3, ...
2. ✅ **Aktivace deaktivuje ostatní** - jen jedna aktivní per entity_type
3. ✅ **Prázdný aktivní** - když neexistuje žádná verze
4. ✅ **Řazení verzí** - descendingorder
5. ✅ **Migrace instance** - update version_id, uložení historie
6. ✅ **Start bulk migrace** - vytvoření záznamu v audit logu
7. ✅ **Izolace entity typů** - ORDER vs INVOICE mají nezávislé verzování
8. ✅ **JSONB storage** - komplexní schémata se ukládají správně

**Spuštění:**
```bash
cd backend
./mvnw test -Dtest=WorkflowVersionServiceTest
```

---

## 📊 Use Cases

### UC1: Přidání nového stavu

**Scénář:** Do ORDER workflow potřebujeme přidat stav `REFUNDED`.

1. Vytvoř novou verzi:
```json
POST /api/v1/workflows/versions
{
  "entityType": "ORDER",
  "schemaDefinition": {
    "states": ["DRAFT", "SUBMITTED", "APPROVED", "CANCELLED", "REFUNDED"],
    "transitions": [...]
  },
  "createdBy": "admin",
  "notes": "Added REFUNDED state for refund process"
}
```

2. Otestuj novou verzi (manuálně migruj pár instancí)

3. Aktivuj verzi:
```
POST /api/v1/workflows/versions/124/activate
```

4. Nové instance budou používat novou verzi

### UC2: Oprava chyby v workflow

**Scénář:** Ve verzi 2 je chyba v guardu, potřebujeme rollback.

1. Aktivuj starší verzi:
```
POST /api/v1/workflows/versions/122/activate  # verze 1
```

2. Migruj zpět problematické instance:
```json
POST /api/v1/workflows/versions/migrate/bulk
{
  "fromVersionId": 123,  // verze 2 (buggy)
  "toVersionId": 122,    // verze 1 (stable)
  "strategy": "IMMEDIATE",
  "initiatedBy": "admin"
}
```

### UC3: Postupná migrace

**Scénář:** Máme 10000 ORDER instancí, chceme opatrně migrovat.

1. Vytvoř novou verzi (ale NEaktivuj)

2. Lazy migrace:
```json
POST /api/v1/workflows/versions/migrate/bulk
{
  "fromVersionId": 122,
  "toVersionId": 124,
  "strategy": "LAZY",      // migruje až při příštím přístupu
  "initiatedBy": "admin"
}
```

3. Sleduj metriky a chybovost

4. Když je vše OK, aktivuj:
```
POST /api/v1/workflows/versions/124/activate
```

---

## 🎯 Klíčové vlastnosti

### ✅ Co máme

1. **Version Storage** - JSONB definice workflow schémat
2. **Auto-increment** - automatické číslování verzí per entity_type
3. **Activation** - pouze jedna aktivní verze per entity_type
4. **Instance Mapping** - každá instance má přiřazenu verzi
5. **Migration History** - audit trail kdo, kdy, odkud, kam migroval
6. **Migration Strategies** - IMMEDIATE, LAZY, MANUAL
7. **REST API** - kompletní CRUD + migrace endpointy
8. **Unit Tests** - 8 test cases pokrývající hlavní scénáře

### 🔜 Co zbývá (pro finalizaci)

- [ ] Integration test (WorkflowVersionControllerIT)
- [ ] E2E test (create → activate → migrate)
- [ ] Dokumentace migračních strategií (kdy použít jakou)
- [ ] Metriky (`workflow.version.active_versions`, `workflow.version.migrations_total`)

---

## 📈 Metriky

Service má `@Timed` anotace na klíčových operacích:

- `workflow.version.create` - čas vytvoření verze
- `workflow.version.activate` - čas aktivace
- `workflow.version.get_active` - čas získání aktivní verze
- `workflow.version.list` - čas listování verzí
- `workflow.version.migrate` - čas migrace instance
- `workflow.version.migrate_bulk` - čas bulk migrace

---

## 🔐 Bezpečnost

Všechny mutace (POST/PUT) vyžadují **`ROLE_WORKFLOW_ADMIN`**.

Čtení (GET) je dostupné všem autentizovaným uživatelům.

---

## 📝 Závěr

**W9 Versioning je funkční** a připravený pro:
- Verzování workflow schémat
- Aktivaci/deaktivaci verzí
- Migraci instancí (single i bulk)
- Audit historii

Zbývá doplnit IT/E2E testy a metriky pro production-ready stav.

**Next:** W10 Studio UI (drag-and-drop editor)
