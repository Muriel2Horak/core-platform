# EPIC-005: GAP Analysis - Metamodel Complete Vision vs Implementation

**Date:** 8. listopadu 2025  
**Purpose:** Validace HIGH-LEVEL požadavků (1️⃣-1️⃣1️⃣) proti implementaci a stávajícím stories

---

## 📊 EXECUTIVE SUMMARY

### ✅ Implementováno (Částečně)
- **Schema Diff Detection** (META-001) - ✅ DONE
- **Hot Reload API** (META-002) - ✅ DONE  
- **UNIQUE Constraints** (META-003) - ✅ DONE
- **Streaming Config** - ⚠️ Schema existuje, ale NENÍ runtime implementace

### 📋 Planned ale Nedetailní
- **Code Generation** (META-006) - pouze template návrh
- **Validation** (META-007) - pouze concept
- **Visual Studio UI** (META-005) - pouze vize
- **Advanced Constraints** (META-004) - žádný detail

### ❌ CHYBÍ (Klíčové z HIGH-LEVEL požadavků)
1. **Streaming & Priority Queue** - schema existuje, runtime NE
2. **Workflow Integration** - žádná story
3. **Reporting Integration** - žádná story
4. **DMS Integration** - žádná story
5. **Loki Integration** - žádná story
6. **RBAC & Security** - žádná story
7. **AI/MCP Integration** - žádná story
8. **API Generation** - žádná story
9. **UI Generation** - žádná story
10. **Naming Conventions** - žádná story

---

## 🔍 DETAILNÍ ANALÝZA PO POŽADAVCÍCH

### 1️⃣ Základní Koncept: "Metamodel = Single Source of Truth"

**HIGH-LEVEL Požadavek:**
- Metamodel řídí: doménový model, validace, práva, API, UI, storage backends

**✅ Implementováno:**
- ✅ Doménový model: `EntitySchema`, `FieldSchema`, `RelationshipSchema`
- ✅ Storage types: Schema má `StorageType` (relational/log/external) - NENÍ runtime
- ✅ Validace: Schema pro constraints - META-007 PLANNED
- ✅ Schema jako konfigurace

**❌ CHYBÍ:**
- ❌ API generation (META-006 jen template návrh)
- ❌ UI generation (META-005 vize, žádný kód)
- ❌ Práva/RBAC (žádná story)
- ❌ Runtime storage routing (relational vs log vs external)

**→ POTŘEBUJEME:**
- **META-008**: API Generation Runtime
- **META-009**: UI Generation Runtime
- **META-013**: RBAC & Access Control

---

### 2️⃣ Entita & Schéma: Fields, Relations, Storage Types

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  name, label, description
  storageType: relational | log | external
  fields:
    - constraints, visibility, searchable, pii
  relations:
    - 1:1, 1:N, M:N, onDelete
```

**✅ Implementováno:**
- ✅ `EntitySchema.java` má: name, table, fields, relationships
- ✅ `FieldSchema.java` má: type, constraints, unique
- ✅ `StorageType` v kódu jako enum (relational/log/external)
- ✅ Relationships: many_to_one, one_to_many podporováno

**⚠️ Částečně:**
- ⚠️ `visibility` (read/write/hidden/adminOnly) - NENÍ v schema
- ⚠️ `searchable`, `filterable`, `sortable` - NENÍ explicitní
- ⚠️ `pii` / `sensitive` flag - NENÍ v schema
- ⚠️ `onDelete` cascade rules - NENÍ detailní

**❌ CHYBÍ:**
- ❌ Runtime pro `storageType` routing (Loki queries, external REST)
- ❌ Field-level security (visibility, PII masking)

**→ POTŘEBUJEME:**
- **Expandovat META-001**: Přidat field-level metadata (visibility, searchable, pii)
- **META-012**: Loki Integration (storageType: log runtime)
- **META-013**: RBAC & Field-Level Security

---

### 3️⃣ Naming & Konvence: DB tables, Kafka topics, REST, FE routes

**HIGH-LEVEL Požadavek:**
```
DB:    core_{context}_{entity}
Kafka: core.{context}.{entity}.{event}
REST:  /api/{context}/{entity}
FE:    /app/{context}/{entity}
```

**✅ Implementováno:**
- ✅ DB tables: `EntitySchema.table` field
- ✅ YAML má naming (ale bez konvencí enforcement)

**❌ CHYBÍ:**
- ❌ Naming convention validation (META-007 jen PLANNED)
- ❌ Auto-generation názvu Kafka topics z entity
- ❌ Auto-generation REST path z entity
- ❌ Auto-generation FE route z entity

**→ POTŘEBUJEME:**
- **Expandovat META-007**: Naming Convention Validation
- **META-008**: Kafka Topic Naming Convention
- **META-009**: REST API Path Convention

---

### 4️⃣ Generování API & UI: CRUD, filters, search, export

**HIGH-LEVEL Požadavek:**
- API: CRUD, filtry, stránkování, search DSL, RLS, export
- UI: list view, detail view, inline edit, metadata-driven

**✅ Implementováno:**
- ✅ META-006 PLANNED: Code Generation (JPA, Repository, Controller)
- ✅ Template návrh existuje (Velocity engine)

**❌ CHYBÍ:**
- ❌ Runtime API generation (žádný kód)
- ❌ Runtime UI generation (žádný kód)
- ❌ Search DSL builder
- ❌ Filter builder
- ❌ Export service (CSV/Excel/JSON)
- ❌ RLS injection do queries

**→ POTŘEBUJEME:**
- **META-008**: API Generation Runtime (CRUD endpoints)
- **META-009**: UI Generation Runtime (list/detail components)
- **META-010**: Search & Filter DSL
- **META-011**: Export Service

---

### 5️⃣ Workflow Integrace: States, Transitions, Guards, Actions

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  workflowDefinition:
    states, transitions, guards, actions
  workflow step types: REST, Kafka, Timer, Approval
```

**✅ Implementováno:**
- ✅ `StateConfig.java` existuje
- ✅ `TransitionConfig.java` existuje
- ✅ `LifecycleAction.java` existuje
- ✅ `TransitionConfig` má `streamingPriority` field

**❌ CHYBÍ:**
- ❌ Workflow engine runtime (žádný kód)
- ❌ Workflow napojení na UI (buttons, state display)
- ❌ Workflow step execution (REST/Kafka/Timer/Approval)
- ❌ Guards/ACL evaluation

**→ POTŘEBUJEME:**
- **META-014**: Workflow Engine Integration
- **META-015**: Workflow UI Components

---

### 6️⃣ Streaming & Events: Kafka, CDC, versioning, presence

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  streaming:
    enabled, topics, payload, versioning, priority queue
```

**✅ Implementováno:**
- ✅ `StreamingEntityConfig.java` - kompletní schema!
  - `enabled`, `retentionHours`, `eventPayloadMode` (full/diff/minimal)
  - `snapshotEnabled`, `topicPrefix`
  - **`priorityWeights`** - CRITICAL/HIGH/NORMAL/BULK weights!
- ✅ `StreamingGlobalConfig.java` - global defaults
- ✅ `EntitySchema` má `streaming` field
- ✅ `TransitionConfig` má `streamingPriority` field

**❌ CHYBÍ:**
- ❌ **Runtime Kafka Producer** (žádný KafkaTemplate)
- ❌ **Priority Queue implementace** (jen schema, žádný executor)
- ❌ CDC event publisher
- ❌ Presence tracking (online lock)
- ❌ FE refresh po event
- ❌ Pre-agg/reporting CDC stream

**→ POTŘEBUJEME:**
- **META-016**: Streaming Runtime & Priority Queue (⚡ CRITICAL!)
  - Kafka producer z entity changes
  - Priority queue executor (CRITICAL → HIGH → NORMAL → BULK)
  - CDC event format (tenantId, entityType, entityId, changeType, version, timestamp, user)
  - Retry/DLQ handling

---

### 7️⃣ Reporting & Analytika: Dimensions, measures, Cube.js

**HIGH-LEVEL Požadavek:**
- Metamodel popisuje: aggregatable fields, dimensions, measures
- Generování: Cube schemas, views, UI dashboards
- RLS/RBAC platí i pro reporting

**✅ Implementováno:**
- ❌ **Žádná story!**

**❌ CHYBÍ:**
- ❌ Field metadata: `aggregatable`, `dimension`, `measure`
- ❌ Cube.js schema generation z metamodel
- ❌ Reporting view generation
- ❌ Dashboard UI generation

**→ POTŘEBUJEME:**
- **META-017**: Reporting & Analytics Integration
  - Field annotations (dimension/measure)
  - Cube.js schema generator
  - RLS injection do Cube queries

---

### 8️⃣ DMS & Dokumenty: Attachments, versioning, external storage

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  attachments:
    enabled, types, visibility, versioning
  externalStorage: MinIO | M365 | Google Drive
```

**✅ Implementováno:**
- ❌ **Žádná story!**

**❌ CHYBÍ:**
- ❌ Attachments field v EntitySchema
- ❌ DMS integration (upload/download/version)
- ❌ External storage routing (MinIO vs M365)
- ❌ UI tab "Dokumenty"

**→ POTŘEBUJEME:**
- **META-018**: DMS Integration
  - Attachments schema config
  - MinIO/M365 connector
  - Document UI components

---

### 9️⃣ Loki / Log Data: Read-only entity, log queries

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  storageType: log
  logSource: loki
  fields: mapped to labels/JSON
  filters, timeRanges
```

**✅ Implementováno:**
- ✅ `StorageType` enum má `log` hodnotu
- ❌ **Žádný runtime pro Loki queries**

**❌ CHYBÍ:**
- ❌ Loki query builder z metamodel filters
- ❌ Loki field mapping (labels → fields)
- ❌ UI komponenty pro log views (tabulka, time-series grafy)
- ❌ RBAC/tenant filtering pro Loki

**→ POTŘEBUJEME:**
- **META-019**: Loki Integration
  - LogQL query builder
  - Field → Label mapping
  - UI log viewer components
  - Tenant/RBAC filtering

---

### 🔟 RBAC, Bezpečnost, Audit: Role, permissions, sensitive data

**HIGH-LEVEL Požadavek:**
```yaml
entity:
  permissions:
    roles, adminOnly, actions, workflow transitions
  fields:
    sensitive, pii, anonymization
  audit: CRUD, workflow, export, AI
```

**✅ Implementováno:**
- ✅ `AccessPolicy.java` existuje
- ✅ `PolicyRule.java` existuje
- ✅ `ColumnPolicy.java` existuje
- ⚠️ Žádný `pii` / `sensitive` flag v FieldSchema

**❌ CHYBÍ:**
- ❌ Runtime RBAC enforcement (role checks)
- ❌ Field-level permissions (read/write per role)
- ❌ PII masking/anonymization
- ❌ Audit logging pro metamodel operations
- ❌ Export permission checks

**→ POTŘEBUJEME:**
- **META-020**: RBAC & Field-Level Security
  - Role-based access checks
  - PII masking engine
  - Audit trail
  - Export permission validation

---

### 1️⃣1️⃣ MCP / AI Integrace: Safe views, tools, PII masking

**HIGH-LEVEL Požadavek:**
- AI může číst safe views (ne raw tables)
- MCP tools generované z metamodel
- PII masking, tenant scope, limity

**✅ Implementováno:**
- ✅ `AiConfig.java` existuje!
- ✅ `AiPolicies.java` existuje
- ✅ `AiPrompts.java` existuje
- ✅ `AiRouteHelp.java` existuje
- ✅ `AiTool.java` existuje
- ✅ `GlobalAiConfig.java` existuje
- ✅ `AiSchemaValidator.java` existuje

**❌ CHYBÍ:**
- ❌ Runtime MCP server generation z metamodel
- ❌ AI tool execution
- ❌ PII masking v AI responses
- ❌ Tenant scoping v AI queries
- ❌ Rate limiting/quota pro AI

**→ POTŘEBUJEME:**
- **META-021**: MCP/AI Integration Runtime
  - MCP server generator
  - AI-safe view builder (PII masked)
  - Tool executor
  - Tenant/rate limiting

---

## 📋 NAVRHOVANÉ USER STORIES (Kompletní Seznam)

### ✅ Existující Stories (META-001 až META-007)

1. **META-001**: Schema Diff Detection ✅ DONE
2. **META-002**: Hot Reload REST API ✅ DONE
3. **META-003**: UNIQUE Constraint Management ✅ DONE
4. **META-004**: Advanced Constraints 📋 PLANNED (expandovat)
5. **META-005**: Visual Metamodel Studio UI 📋 PLANNED (expandovat)
6. **META-006**: Code Generation 📋 PLANNED (expandovat)
7. **META-007**: Validation & Business Rules 📋 PLANNED (expandovat)

### ⚡ NOVÉ Stories (META-008 až META-021) - CHYBÍ!

**Core Engine:**

8. **META-008**: API Generation Runtime
   - CRUD endpoints generation
   - Filter/search DSL
   - RLS injection
   - Export service

9. **META-009**: UI Generation Runtime
   - List view components
   - Detail view components
   - Form generation
   - Metadata-driven rendering

**Naming & Conventions:**

10. **META-010**: Naming Convention System
    - DB table naming enforcement
    - Kafka topic naming
    - REST path convention
    - FE route convention

**Integrations:**

11. **META-011**: Streaming & Priority Queue Runtime ⚡ **KRITICKÉ!**
    - Kafka producer from entity changes
    - Priority queue executor (CRITICAL/HIGH/NORMAL/BULK)
    - CDC event format
    - Retry/DLQ handling
    - Presence tracking

12. **META-012**: Workflow Engine Integration
    - State machine runtime
    - Transition guards/actions
    - Workflow step execution (REST/Kafka/Timer/Approval)
    - UI workflow components (buttons, state display)

13. **META-013**: Reporting & Analytics Integration
    - Field metadata (dimension/measure/aggregatable)
    - Cube.js schema generator
    - RLS in Cube queries
    - Dashboard UI generation

14. **META-014**: DMS Integration
    - Attachments schema config
    - MinIO/M365 connector
    - Document versioning
    - Document UI components

15. **META-015**: Loki Integration
    - LogQL query builder
    - Field → Label mapping
    - UI log viewer
    - Tenant/RBAC filtering

**Security & Access:**

16. **META-016**: RBAC & Field-Level Security
    - Role-based access runtime
    - Field visibility enforcement
    - PII masking engine
    - Audit trail
    - Export permission checks

17. **META-017**: MCP/AI Integration Runtime
    - MCP server generator
    - AI-safe view builder (PII masked)
    - Tool executor
    - Tenant scoping
    - Rate limiting/quota

**Advanced Features:**

18. **META-018**: Multi-Tenancy Enhancements
    - Tenant-specific schema variations
    - Column-level tenant isolation
    - Shared vs dedicated tables

19. **META-019**: Versioning & Migrations
    - Entity version history
    - Automatic Flyway generation
    - Schema rollback support

20. **META-020**: Search & Filter DSL
    - Advanced query builder
    - Full-text search
    - Faceted search
    - Filter UI components

21. **META-021**: External Storage Routing
    - Storage type runtime (relational/log/external)
    - External REST connector
    - API gateway integration
    - n8n workflow integration

---

## 🎯 PRIORITY MATRIX

### P0 - CRITICAL (Start Immediately)

**META-011**: **Streaming & Priority Queue Runtime** ⚡
- **WHY**: Schema existuje, ale ŽÁDNÝ runtime! Frontend fronta s prioritizací byla požadována od začátku.
- **IMPACT**: Bez toho není event-driven architecture, CDC, real-time updates
- **EFFORT**: 3-4 týdny (Kafka producer, priority executor, DLQ)

**META-016**: **RBAC & Field-Level Security**
- **WHY**: Bezpečnost MUSÍ být před API/UI generation
- **IMPACT**: Bez toho je systém unsafe (žádné permissions checks)
- **EFFORT**: 2-3 týdny

### P1 - HIGH (Next Sprint)

**META-008**: **API Generation Runtime**
- **WHY**: CRUD API generování je core funkcionalita
- **EFFORT**: 2-3 týdny

**META-009**: **UI Generation Runtime**
- **WHY**: Metadata-driven UI je hlavní value proposition
- **EFFORT**: 3-4 týdny

**META-010**: **Naming Convention System**
- **WHY**: Konzistence v celém stacku (DB/Kafka/REST/FE)
- **EFFORT**: 1 týden

**META-012**: **Workflow Engine Integration**
- **WHY**: Workflow + metamodel integrace je klíčová pro procesy
- **EFFORT**: 3-4 týdny

### P2 - MEDIUM (Later)

**META-013**: Reporting & Analytics Integration
**META-014**: DMS Integration
**META-015**: Loki Integration
**META-017**: MCP/AI Integration Runtime
**META-020**: Search & Filter DSL

### P3 - NICE TO HAVE

**META-018**: Multi-Tenancy Enhancements
**META-019**: Versioning & Migrations
**META-021**: External Storage Routing

---

## 📊 EFFORT ESTIMATE

### Implementované (META-001 až META-003)
- **Schema Diff Detection**: ~600 LOC ✅
- **Hot Reload API**: ~200 LOC ✅
- **UNIQUE Constraints**: ~50 LOC ✅
- **Total**: ~850 LOC (3 týdny)

### Planned ale Nedetailní (META-004 až META-007)
- **Advanced Constraints**: ~300 LOC (1 týden)
- **Visual Studio UI**: ~2000 LOC (3 týdny)
- **Code Generation**: ~600 LOC (2 týdny)
- **Validation**: ~400 LOC (1 týden)
- **Total**: ~3300 LOC (7 týdnů)

### NOVÉ Stories (META-008 až META-021)
- **API Generation Runtime**: ~1500 LOC (3 týdny)
- **UI Generation Runtime**: ~2500 LOC (4 týdny)
- **Naming Convention**: ~300 LOC (1 týden)
- **Streaming & Priority Queue**: ~2000 LOC (4 týdny) ⚡ CRITICAL
- **Workflow Engine**: ~2000 LOC (4 týdny)
- **Reporting Integration**: ~1000 LOC (2 týdny)
- **DMS Integration**: ~1200 LOC (2 týdny)
- **Loki Integration**: ~800 LOC (2 týdny)
- **RBAC & Security**: ~1800 LOC (3 týdny)
- **MCP/AI Runtime**: ~1500 LOC (3 týdny)
- **Search & Filter DSL**: ~1000 LOC (2 týdny)
- **Multi-Tenancy**: ~600 LOC (1 týden)
- **Versioning**: ~800 LOC (2 týdny)
- **External Storage**: ~1000 LOC (2 týdny)
- **Total**: ~17,000 LOC (~35-40 týdnů)

### **GRAND TOTAL**: ~21,150 LOC (~50-55 týdnů / ~12 měsíců)

---

## 🚦 NEXT STEPS

1. **✅ Review této analýzy** - schválení priorit a scope
2. **📝 Expandovat existující META-004 až META-007** - přidat detaily
3. **📝 Vytvořit nové META-008 až META-021** - kompletní story soubory
4. **📝 Updatovat EPIC-005 README** - kompletní vize podle HIGH-LEVEL požadavků
5. **🏗️ Vytvořit task strukturu** - S1-S21 directories s T#-*.md files
6. **⚡ START META-011 (Streaming & Priority Queue)** - CRITICAL missing feature!

---

**Analýzu provedl:** GitHub Copilot  
**Validace:** Na základě HIGH-LEVEL požadavků 1️⃣-1️⃣1️⃣  
**Implementace prověřena:** `backend/src/main/java/cz/muriel/core/metamodel/**/*.java`  
**Status:** ⚠️ **KRITICKÉ GAP identifikovány** - Streaming runtime CHYBÍ přes existující schema!
