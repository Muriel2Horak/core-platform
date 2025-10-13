# 🔄 Workflow Implementation Analysis & Metamodel Gap Analysis

## 📅 Datum analýzy: 13. října 2025

---

## 🎯 Executive Summary

**Problém:** Workflow metamodel YAML soubory byly navrženy v Kubernetes-style formátu, ale backend očekává flat EntitySchema formát. Soubory obsahují nepodporované fields a nikdy nebyly validované proti Java schématům.

**Dopad:** Backend crashuje při startu s validation errors.

**Řešení:** 
1. ✅ Opravit všechny 4 workflow YAML soubory podle správného formátu
2. ✅ Rozšířit metamodel o chybějící workflow features
3. ✅ Vytvořit build-time validator

---

## 📊 Backend Workflow Implementation Analysis

### 1️⃣ WorkflowExecutionService (W7)

**Účel:** Execution engine pro workflow graphs (nodes + edges)

**Co potřebuje:**
```java
// Načítá z workflow_versions table přes MetamodelCrudService
Map<String, Object> workflow = loadActiveWorkflow(entity, auth);
// Očekává strukturu:
{
  "entity": "PurchaseOrder",
  "status": "ACTIVE",
  "data": {
    "nodes": [
      {"id": "n1", "type": "start", "data": {"label": "Start"}},
      {"id": "n2", "type": "task", "data": {"label": "Validate"}},
      {"id": "n3", "type": "decision", "data": {"label": "Amount > 1000?", "condition": "amount > 1000"}},
      {"id": "n4", "type": "end", "data": {"label": "End"}}
    ],
    "edges": [
      {"source": "n1", "target": "n2"},
      {"source": "n2", "target": "n3"},
      {"source": "n3", "target": "n4", "label": "true"},
      {"source": "n3", "target": "n4", "label": "false"}
    ]
  }
}
```

**Ukládá do:**
- `workflow_executions` table přes MetamodelCrudService
- Fields: entity, status, steps, durationMs, error, executedAt

**Supported Node Types:**
- `start` - entry point
- `task` - execute action (zatím no-op, TODO integrace)
- `decision` - conditional branch (simple expression: `amount > 1000`, `status == "APPROVED"`)
- `end` - terminal node

**Limitace:**
- ⚠️ Simple expression parser (bez SpEL/JEXL)
- ⚠️ Max 100 steps (infinite loop protection)
- ⚠️ Task nodes jsou no-op (placeholder pro budoucí integrace)

---

### 2️⃣ WorkflowService (State Machine)

**Účel:** State management & transitions pro entity lifecycle

**Co potřebuje:**
```sql
-- entity_state table
CREATE TABLE entity_state (
  entity_type VARCHAR,
  entity_id UUID,
  tenant_id UUID,
  state_code VARCHAR,
  since TIMESTAMP
);

-- state_transition table
CREATE TABLE state_transition (
  code VARCHAR PRIMARY KEY,
  entity_type VARCHAR,
  from_code VARCHAR,
  to_code VARCHAR,
  guard JSONB,
  sla_minutes INTEGER
);

-- entity_state_log table
CREATE TABLE entity_state_log (
  id SERIAL PRIMARY KEY,
  entity_type VARCHAR,
  entity_id UUID,
  tenant_id UUID,
  from_code VARCHAR,
  to_code VARCHAR,
  changed_by VARCHAR,
  changed_at TIMESTAMP,
  metadata JSONB
);
```

**Features:**
- ✅ State transitions s validací
- ✅ Guard conditions (simple role check: `hasRole('ROLE_NAME')`)
- ✅ SLA tracking (NONE, OK, WARN, BREACH)
- ✅ Transition history log
- ⚠️ Complex CEL expressions not supported yet

**Model:**
```java
EntityState {
  entityType, entityId, tenantId, stateCode, since
}

StateTransition {
  entityType, fromCode, toCode, code, guard, slaMinutes
}

StateLog {
  id, entityType, entityId, tenantId, fromCode, toCode, changedBy, changedAt, metadata
}
```

---

### 3️⃣ Workflow Draft/Proposal/Version Services

**Účel:** Workflow lifecycle management (draft → proposal → version)

**Co potřebují:**
```yaml
# workflow-draft.yaml
entity: WorkflowDraft
table: workflow_drafts
fields:
  - name: id (uuid, pk)
  - name: tenant_id (string)
  - name: entity (string, unique) # Entity name
  - name: data (text) # JSON workflow graph
  - name: created_at, updated_at, created_by, updated_by
  - name: version (long) # Optimistic locking

# workflow-proposal.yaml
entity: WorkflowProposal
table: workflow_proposals
states: [PENDING, APPROVED, REJECTED]
transitions:
  - code: submit
    from: null
    to: PENDING
  - code: approve
    from: PENDING
    to: APPROVED
  - code: reject
    from: PENDING
    to: REJECTED

# workflow-version.yaml
entity: WorkflowVersion
table: workflow_versions
fields:
  - name: status (string) # ACTIVE, ARCHIVED
  - name: version_number (long)
  - name: activated_at, activated_by
```

---

## 🔍 Metamodel Schema Analysis

### ✅ Podporované v EntitySchema:

```java
EntitySchema {
  entity: String           // ✅ Entity name
  table: String            // ✅ Table name
  idField: String          // ✅ PK field name
  tenantField: String      // ✅ Tenant isolation field
  
  fields: List<FieldSchema>        // ✅ Field definitions
  accessPolicy: AccessPolicy       // ✅ RBAC/ABAC rules
  ui: UiConfig                     // ✅ UI metadata
  
  states: List<StateConfig>        // ✅ Workflow states
  transitions: List<TransitionConfig> // ✅ Workflow transitions
  
  idGeneration: IdGenerationConfig // ✅ ID strategy
  lifecycle: LifecycleConfig       // ✅ Hooks (beforeCreate, etc.)
  streaming: StreamingEntityConfig // ✅ Kafka streaming
}
```

### ✅ FieldSchema - Podporované properties:

```java
FieldSchema {
  name: String             // ✅ Field name
  type: String             // ✅ uuid, string, email, text, long, timestamp, boolean, ref, collection, manyToMany, oneToMany, manyToOne
  pk: Boolean              // ✅ Primary key
  required: Boolean        // ✅ NOT NULL
  generated: Boolean       // ✅ Auto-generated
  unique: Boolean          // ✅ UNIQUE constraint
  maxLength: Integer       // ✅ VARCHAR(n)
  defaultValue: Object     // ✅ DEFAULT value
  
  // Relationships
  refEntity: String        // ✅ Foreign key target
  refField: String         // ✅ FK field name
  itemType: String         // ✅ Collection item type
  targetEntity: String     // ✅ M:N target
  joinTable: String        // ✅ Junction table
  joinColumn: String       // ✅ This side FK
  inverseJoinColumn: String // ✅ Other side FK
  bidirectional: Boolean   // ✅ Bidirectional rel
  inverseName: String      // ✅ Inverse field name
  mappedBy: String         // ✅ JPA mappedBy
  cascade: List<String>    // ✅ ALL, PERSIST, MERGE, REMOVE, etc.
}
```

### ❌ NEPODPOROVANÉ v FieldSchema (použité v workflow YAMLs):

```yaml
❌ description: "Field description"  # Není v FieldSchema
❌ indexed: true                     # Není v FieldSchema
❌ generated: uuid_v7                # Boolean, ne enum
❌ primaryKey: true                  # Používej pk: true
```

### ✅ StateConfig - Podporované:

```java
StateConfig {
  code: String        // ✅ State code (NOT name!)
  label: String       // ✅ Display label
  description: String // ✅ Description
}
```

### ❌ NEPODPOROVANÉ v StateConfig:

```yaml
❌ name: PENDING    # Používej code: PENDING
❌ color: warning   # Není v StateConfig
```

### ✅ TransitionConfig - Podporované:

```java
TransitionConfig {
  code: String                // ✅ Transition code
  from: String                // ✅ From state
  to: String                  // ✅ To state
  label: String               // ✅ Display label
  guard: Map<String, Object>  // ✅ Guard condition
  slaMinutes: Integer         // ✅ SLA threshold
}
```

### ❌ NEPODPOROVANÉ v TransitionConfig:

```yaml
❌ action: executeTask           # Není v TransitionConfig
❌ permissions: [ROLE_ADMIN]     # Není v TransitionConfig (používej guard)
```

---

## 📋 Workflow YAML Soubory - Co je potřeba opravit

### 1. workflow-draft.yaml

**Současný stav:** ✅ Většinou OK, drobné opravy
- ✅ Má správný flat format
- ✅ Fields jsou OK
- ✅ AccessPolicy je OK
- ⚠️ Odstranit `action` z transitions (pokud existuje)

### 2. workflow-proposal.yaml

**Současný stav:** ⚠️ Potřebuje opravy
- ❌ States mají `name:` → změnit na `code:`
- ❌ States mají `color:` → odstranit
- ❌ Transitions mají `action:` → odstranit
- ❌ Transitions mají `permissions:` → přesunout do `guard`

### 3. workflow-version.yaml

**Současný stav:** ⚠️ Potřebuje opravy
- ❌ States mají `name:` → změnit na `code:`
- ❌ States mají `color:` → odstranit
- ❌ Transitions mají `action:` → odstranit
- ❌ Transitions mají `permissions:` → přesunout do `guard`

### 4. workflow-execution.yaml

**Současný stav:** ❌ KRITICKÉ - Kompletně přepsat
- ❌ Používá Kubernetes-style format (`apiVersion`, `kind`, `metadata`, `spec`)
- ❌ Fields mají nepodporované properties (`description`, `indexed`, `generated: uuid_v7`)
- ❌ AccessPolicy má `none: true` (nepodporováno)
- ❌ UI filters jsou objekty, ne stringy

**Potřebuje:**
- ✅ Změnit na flat EntitySchema format
- ✅ Odstranit nepodporované field properties
- ✅ Opravit accessPolicy (použít `role: NEVER_ALLOWED`)
- ✅ Změnit filters na `List<String>`

---

## 🔧 Chybějící Features v Metamodelu

### 1️⃣ Node Type Definitions (pro WorkflowExecutionService)

**Co chybí:** Definice node types pro workflow graphs

**Návrh rozšíření:**
```java
// EntitySchema.java
public class EntitySchema {
  // ... existing fields
  private List<WorkflowNodeTypeConfig> nodeTypes; // ✨ NEW
}

// WorkflowNodeTypeConfig.java
@Data
public class WorkflowNodeTypeConfig {
  private String type;           // "start", "task", "decision", "end"
  private String label;          // Display name
  private String icon;           // Icon name
  private List<String> properties; // Required properties
}
```

**YAML příklad:**
```yaml
nodeTypes:
  - type: start
    label: Start Node
    icon: play-circle
    properties: []
    
  - type: task
    label: Task Node
    icon: check-square
    properties: [taskType, handler]
    
  - type: decision
    label: Decision Node
    icon: git-branch
    properties: [condition]
    
  - type: end
    label: End Node
    icon: stop-circle
    properties: []
```

### 2️⃣ Expression Engine Configuration

**Co chybí:** Konfigurace expression evaluation engine

**Návrh rozšíření:**
```java
// TransitionConfig.java
public class TransitionConfig {
  // ... existing fields
  private String guardExpression;      // ✨ NEW: Simple string expression
  private ExpressionEngineType engine; // ✨ NEW: SIMPLE, SPEL, JEXL
}

public enum ExpressionEngineType {
  SIMPLE,  // Basic comparisons: amount > 1000
  SPEL,    // Spring Expression Language
  JEXL,    // Apache Commons JEXL
  CEL      // Common Expression Language (future)
}
```

### 3️⃣ Task Handler Registry

**Co chybí:** Definice task handlers pro execution

**Návrh rozšíření:**
```java
// EntitySchema.java
public class EntitySchema {
  // ... existing fields
  private List<TaskHandlerConfig> taskHandlers; // ✨ NEW
}

// TaskHandlerConfig.java
@Data
public class TaskHandlerConfig {
  private String type;          // "api", "email", "notification", "script"
  private String handlerClass;  // Fully qualified class name
  private Map<String, Object> config; // Handler-specific config
}
```

**YAML příklad:**
```yaml
taskHandlers:
  - type: api
    handlerClass: cz.muriel.core.workflow.handlers.ApiCallHandler
    config:
      endpoint: https://api.example.com/validate
      method: POST
      timeout: 5000
      
  - type: email
    handlerClass: cz.muriel.core.workflow.handlers.EmailHandler
    config:
      from: noreply@example.com
      template: approval-notification
```

### 4️⃣ Validation Rules

**Co chybí:** YAML schema validation při buildu

**Návrh:**
- ✅ Unit test s Jackson deserializace
- ✅ Maven plugin pro pre-compile validation
- ✅ CI/CD check

---

## 🎯 Akční plán

### Fáze 1: Opravit existující YAML soubory (TEĎ)

**Priorita:** 🔴 KRITICKÁ

1. ✅ workflow-draft.yaml - odstranit `action` z transitions
2. ✅ workflow-proposal.yaml:
   - Změnit `name:` → `code:` ve states
   - Odstranit `color:` ze states
   - Odstranit `action:` z transitions
   - Odstranit `permissions:` z transitions
3. ✅ workflow-version.yaml:
   - Změnit `name:` → `code:` ve states
   - Odstranit `color:` ze states
   - Odstranit `action:` z transitions
   - Odstranit `permissions:` z transitions
4. ✅ workflow-execution.yaml:
   - Přepsat z Kubernetes-style na flat format
   - Odstranit `description`, `indexed` z fields
   - Opravit `generated: uuid_v7` → `generated: true`
   - Změnit `none: true` → `role: NEVER_ALLOWED`
   - Změnit filters na `[status, entity]`

### Fáze 2: Build-time Validator (TEĎ)

**Priorita:** 🔴 KRITICKÁ

```java
// MetamodelValidatorTest.java
@Test
public void validateAllMetamodelYamls() {
  Path metamodelDir = Paths.get("src/main/resources/metamodel");
  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  
  Files.walk(metamodelDir)
    .filter(p -> p.toString().endsWith(".yaml"))
    .forEach(yamlFile -> {
      try {
        EntitySchema schema = mapper.readValue(yamlFile.toFile(), EntitySchema.class);
        assertNotNull(schema.getEntity());
        assertNotNull(schema.getTable());
        // Validate fields, states, transitions...
      } catch (Exception e) {
        fail("Invalid YAML: " + yamlFile + " - " + e.getMessage());
      }
    });
}
```

### Fáze 3: Rozšíření metamodelu (DALŠÍ SPRINT)

**Priorita:** 🟡 DŮLEŽITÉ

1. 🔹 Přidat `WorkflowNodeTypeConfig`
2. 🔹 Přidat `ExpressionEngineType` enum
3. 🔹 Přidat `TaskHandlerConfig`
4. 🔹 Dokumentace metamodel extensions

### Fáze 4: Production-ready Execution Engine (BUDOUCNOST)

**Priorita:** 🟢 NICE TO HAVE

1. 🔹 Integrace SpEL/JEXL pro expressions
2. 🔹 Task handler registry + implementations
3. 🔹 Async execution support
4. 🔹 Retry logic, error handling
5. 🔹 Performance optimizations

---

## 📊 Gap Matrix

| Feature | Backend Support | Metamodel Support | YAML Support | Status |
|---------|----------------|-------------------|--------------|--------|
| **States** | ✅ WorkflowService | ✅ StateConfig | ⚠️ Uses `name` not `code` | 🔧 FIX |
| **Transitions** | ✅ WorkflowService | ✅ TransitionConfig | ⚠️ Has unsupported fields | 🔧 FIX |
| **Guard conditions** | ⚠️ Simple only | ✅ Map<String,Object> | ❌ Uses `permissions` | 🔧 FIX |
| **SLA tracking** | ✅ WorkflowService | ✅ slaMinutes | ✅ OK | ✅ OK |
| **Execution engine** | ✅ WorkflowExecutionService | ❌ No schema | ❌ Wrong format | 🔧 FIX |
| **Node types** | ✅ Hardcoded | ❌ No config | ❌ Not defined | 🆕 NEW |
| **Expression engine** | ⚠️ Simple parser | ❌ No config | ❌ Not configured | 🆕 NEW |
| **Task handlers** | ❌ No-op | ❌ No config | ❌ Not defined | 🆕 NEW |
| **Validation** | ❌ Runtime only | ❌ No validator | ❌ No checks | 🆕 NEW |

---

## 📝 Závěr

**Hlavní problémy:**
1. ❌ Workflow YAMLs používají nevalidní formát (Kubernetes-style vs flat)
2. ❌ YAMLs obsahují nepodporované fields (`description`, `action`, `permissions`, `color`)
3. ❌ Chybí build-time validation
4. ⚠️ Backend podporuje pouze simple expressions (bez SpEL/JEXL)
5. ⚠️ Task nodes jsou no-op (chybí handler integrace)

**Doporučené kroky:**
1. ✅ **TEĎ:** Opravit všechny 4 YAML soubory podle správného formátu
2. ✅ **TEĎ:** Vytvořit MetamodelValidatorTest pro CI/CD
3. 🔹 **SPRINT +1:** Rozšířit metamodel o node types, task handlers
4. 🔹 **SPRINT +2:** Integrace SpEL/JEXL expression engine
5. 🔹 **SPRINT +3:** Production-ready execution engine

**Odhad času:**
- Fáze 1 (opravy YAML): **30 minut**
- Fáze 2 (validator test): **20 minut**
- Fáze 3 (metamodel extensions): **4-6 hodin**
- Fáze 4 (production features): **2-3 dny**

---

**Autor:** AI Coding Assistant  
**Datum:** 13. října 2025  
**Verze:** 1.0
