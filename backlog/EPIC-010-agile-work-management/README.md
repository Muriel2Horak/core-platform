# EPIC-010: Agile Work Management Module (AWM)

**Status:** 🔵 **PLANNED**  
**Priority:** P1 (Strategic)  
**Effort:** ~180 hodin  
**LOC:** ~12,000 řádků

---

## 🎯 Cíle EPICU

**První referenční aplikační modul nad core-platform**, který funkčně pokrývá běžné potřeby agilního řízení práce (issues, backlog, boardy, sprints, workflow), ale je:

- ✅ **100% postavený na Metamodelu + Workflow enginu** (žádná speciální magie mimo core)
- ✅ **Multi-tenant** (tenant = subdoména = realm, tvrdá izolace)
- ✅ **Řízený přes RBAC** core-platform (role-based permissions na projekty, operace, pole)
- ✅ **Provázaný s DMS, monitoringem a AI/MCP** (využití existující infrastruktury)

### Referenční Baseline: Jira Software (čistá, bez apps)

**Co standardně umí:**
- Projekty jako kontejnery
- Issue typy (Story, Bug, Task, Epic, Sub-task)
- Custom fields
- Boardy: Scrum/Kanban (swimlanes, columns, WIP limits)
- Backlog: prioritizace, drag&drop, epics, sprints
- Sprints: start/stop, capacity, burndown
- Workflow: stavový automat, přechody, podmínky, validace
- Assignment, watchers, comments
- Základní permissions a roles
- Filter/JQL + základní reporty

**To bereme jako minimální UX očekávání.**

---

## 🏗️ Principy Implementace

### 1. Žádná Speciální Magie Mimo Core

- **Vše je definované jako:**
  - Metamodel entities (Work Item, Project, Sprint, Board)
  - Workflow definitions (Kanban, Scrum, Bugflow)
  - View definitions (Backlog, Board, Reports)
- **Frontend používá:**
  - Existující EntityView SDK
  - Workflow API
  - Log/Metrics API
- **Modul = konfigurace + tenká UI vrstva**, ne vlastní "mini Jira uvnitř core"

### 2. Multi-tenant Model

- **Každý tenant má vlastní AWM konfiguraci:**
  - Projects, workflows, boards
  - Work items (issues) jsou tenant-specific
- **Tenant izolace zůstává:** `tenant = subdoména = realm`
- **Žádné sdílené issues přes tenancy boundary** (pokud nebude explicitní cross-tenant feature)

### 3. RBAC

**Přístup přes core roles:**
- `AWM_ADMIN`: Správa workflow, boardů, polí, projektů
- `AWM_USER`: Práce s issues (create/edit/transition)
- `AWM_VIEWER`: Read-only přístup

**Oprávnění na:**
- **Projekty**: které entity/boardy vidím
- **Operace**: create/edit/transition/delete
- **Pole**: read/write maskování (sensitive fields)
- **Boardy/View**: kdo může konfigurovat layout, filtry

### 4. UX Očekávání

**Musí "působit jako Jira", ale být naše:**
- Backlog view (filtrování, drag&drop prioritizace)
- Kanban/Scrum board (swimlanes, WIP limits, drag&drop transitions)
- Detail issue (fields, comments, files, timeline, workflow viz)
- Sprinty (planning, active sprint, completion)
- Jednoduché reporty (burndown, velocity, lead time)

---

## 📊 Funkční Rozsah (Themes & Stories)

### Theme 1: Core Entity Model (Metamodel-based)

**AWM1: Definuj generický "Work Item" model v metamodelu**

**As a** platform admin  
**I want** metamodel definici Work Item entity  
**So that** můžu vytvářet issues bez DB migrace

**Acceptance Criteria:**

✅ **Work Item typy:**
- Epic (velký iniciativa, kontejner pro Features/Stories)
- Feature (mid-level, obsahuje Stories)
- Story (user story, deliverable)
- Task (technický úkol)
- Bug (defekt)
- Subtask (sub-item pro dekomposici)

✅ **Core fields:**
- `summary` (string, required, max 255 chars)
- `description` (rich-text, Markdown supported)
- `assignee` (user reference, optional)
- `reporter` (user reference, auto-set on create)
- `status` (workflow state, managed by WF engine)
- `priority` (enum: Blocker, Critical, Major, Minor, Trivial)
- `labels` (array of strings, filterable)
- `due_date` (date, optional)
- `story_points` (integer, optional, for estimation)
- `created_at`, `updated_at` (auto-managed)

✅ **Validace:**
- Summary required, max length
- Priority required for Bug type
- Story points only for Story/Feature types

**BE Impact:**
- Metamodel definition JSON (stored in `metamodel_definitions` table)
- Entity CRUD API reuse (generic entity controller)
- Validation rules as metamodel constraints

**FE Impact:**
- Entity form generator (reuse existing EntityView SDK)
- Rich-text editor for description field
- User picker component (assignee/reporter)

**Security/Tenancy:**
- Work Items jsou tenant-scoped (tenant_id foreign key)
- RBAC: `AWM_USER` role required for create/edit

**Test Coverage:**
- Unit: Metamodel validation rules
- Integration: Work Item CRUD via API
- E2E: Create Epic → Story → Subtask hierarchy

**LOC:** ~400 (metamodel JSON: ~150, validation: ~100, tests: ~150)  
**Effort:** 8h  
**Priority:** MUST HAVE (Phase 1)

---

**AWM2: Relace a hierarchie**

**As a** user  
**I want** provázat work items hierarchií a odkazy  
**So that** můžu organizovat práci (Epic → Story → Subtask)

**Acceptance Criteria:**

✅ **Hierarchie (parent/child):**
- Epic může obsahovat Features a Stories
- Feature může obsahovat Stories
- Story může obsahovat Subtasks
- Task může obsahovat Subtasks
- Bug samostatný (no children)

✅ **Cross-links:**
- `blocks` / `is blocked by` (dependency)
- `relates to` (generic link)
- `duplicates` / `is duplicated by`
- `clones` / `is cloned by`

✅ **Vazby na core data:**
- `linked_customer` (Customer entity reference)
- `linked_asset` (Asset entity reference)
- `linked_documents` (DMS document references)

✅ **API:**
- GET `/api/v1/work-items/{id}/children` - seznam child items
- GET `/api/v1/work-items/{id}/links` - všechny linky
- POST `/api/v1/work-items/{id}/links` - vytvoř link

**BE Impact:**
- `work_item_relationships` table (parent_id, child_id, relationship_type)
- Cascade delete rules (pokud parent smazán → co s children?)
- Circular dependency validation

**FE Impact:**
- Hierarchie tree view v detailu work item
- Link picker dialog (search work items, select type)
- Visual indicator pro blocked items

**Security/Tenancy:**
- Links pouze v rámci tenant boundary
- RBAC check: user musí mít read na obě strany linku

**Test Coverage:**
- Unit: Circular dependency detection
- Integration: Link creation, cascade delete scenarios
- E2E: Create Epic → add Stories → verify hierarchy

**LOC:** ~500 (BE: ~300, FE: ~150, tests: ~50)  
**Effort:** 10h  
**Priority:** MUST HAVE (Phase 1)

---

**AWM3: Validace a business rules**

**As a** admin  
**I want** konfigurovatelné validační pravidla  
**So that** můžu vynucovat data quality (např. Bug musí mít priority)

**Acceptance Criteria:**

✅ **Required fields:**
- Summary always required
- Priority required for Bug
- Assignee required before transition to "In Progress"

✅ **Pattern validace:**
- Summary regex (např. no leading/trailing spaces)
- Labels format (alphanumeric + dash only)

✅ **Business rules:**
- Bug cannot have story points
- Subtask must have parent
- Epic cannot be child of anything

✅ **Custom validace per tenant:**
- Tenant admin může přidat vlastní rules přes UI

**BE Impact:**
- Validation engine extension (metamodel constraint framework)
- Custom validation rules storage (JSON schema)
- Validation execution on entity save

**FE Impact:**
- Validation error display (field-level + form-level)
- Custom rules editor (admin UI)
- Real-time validation feedback

**Security/Tenancy:**
- Custom rules jsou tenant-scoped
- Only `AWM_ADMIN` může editovat rules

**Test Coverage:**
- Unit: Validation rule evaluation
- Integration: API returns 400 with validation errors
- E2E: Submit invalid work item → see error messages

**LOC:** ~300 (validation engine: ~150, UI: ~100, tests: ~50)  
**Effort:** 6h  
**Priority:** SHOULD HAVE (Phase 1)

---

### Theme 2: Workflow

**AWM4: Předpřipravené workflow šablony**

**As a** admin  
**I want** ready-to-use workflow templates  
**So that** můžu rychle nastavit team process (Kanban/Scrum/Bugflow)

**Acceptance Criteria:**

✅ **Šablona 1: Simple Kanban**
- States: `To Do` → `In Progress` → `Done`
- Transitions: 
  - Start Work (To Do → In Progress)
  - Complete (In Progress → Done)
  - Reopen (Done → To Do)

✅ **Šablona 2: Software Bugflow**
- States: `Open` → `In Progress` → `In Review` → `Resolved` → `Closed`
- Transitions:
  - Start Fix (Open → In Progress)
  - Submit for Review (In Progress → In Review)
  - Approve Fix (In Review → Resolved)
  - Close Bug (Resolved → Closed)
  - Reject (In Review → In Progress)
  - Reopen (Resolved/Closed → Open)

✅ **Šablona 3: Scrum**
- States: `To Do` → `In Progress` → `In Test` → `Done`
- Transitions:
  - Start Work (To Do → In Progress)
  - Submit for Test (In Progress → In Test)
  - Pass Test (In Test → Done)
  - Fail Test (In Test → In Progress)

✅ **Import šablony:**
- Admin UI: "Create Workflow" → select template → customize

**BE Impact:**
- Workflow template definitions (JSON files)
- Workflow import API (POST `/api/v1/workflows/import`)
- Template catalog endpoint (GET `/api/v1/workflows/templates`)

**FE Impact:**
- Workflow template picker (modal dialog)
- Preview workflow diagram before import
- Customize states/transitions before save

**Security/Tenancy:**
- Workflows jsou tenant-scoped
- Only `AWM_ADMIN` může importovat

**Test Coverage:**
- Unit: Template JSON parsing
- Integration: Import workflow, verify states/transitions created
- E2E: Select template → customize → use in work item

**LOC:** ~400 (templates: ~150, import logic: ~150, UI: ~100)  
**Effort:** 8h  
**Priority:** MUST HAVE (Phase 1)

---

**AWM5: Workflow engine integrace**

**As a** system  
**I want** použít náš core Workflow engine  
**So that** AWM workflow je konzistentní s ostatními moduly

**Acceptance Criteria:**

✅ **Definice v metamodelu:**
- Workflow je entity v core (reuse EPIC-006 Workflow Engine)
- States, transitions, guards definované v `workflow_definitions` table

✅ **Guard rules:**
- "Cannot start work without assignee" (guard condition)
- "Cannot close without resolution" (required field check)
- SLA enforcement (max time in state)

✅ **Eventy pro metriky:**
- Workflow transition → event logged to Loki
- Metrics: `awm_workflow_transitions_total`, `awm_time_in_state_seconds`

✅ **API:**
- GET `/api/v1/work-items/{id}/workflow` - current state + available transitions
- POST `/api/v1/work-items/{id}/transitions/{transitionId}` - execute transition

**BE Impact:**
- Reuse existing Workflow Engine (EPIC-006)
- AWM-specific guard implementations
- Event logging integration (Loki)

**FE Impact:**
- Workflow status badge (colored by state)
- Transition button bar (only available transitions)
- Guard failure explanation (why transition disabled)

**Security/Tenancy:**
- Workflow definitions jsou tenant-scoped
- RBAC: user must have `transition` permission

**Test Coverage:**
- Unit: Guard evaluation logic
- Integration: Transition execution, guard blocking
- E2E: Move work item through workflow, verify guards

**LOC:** ~500 (guards: ~200, API: ~150, FE: ~100, tests: ~50)  
**Effort:** 12h  
**Priority:** MUST HAVE (Phase 1)

---

**AWM6: Workflow visualization v detailu issue**

**As a** user  
**I want** vidět workflow stav a historii  
**So that** rozumím co se děje a co můžu dělat

**Acceptance Criteria:**

✅ **Zobrazení:**
- **Aktuální stav** (badge s barvou)
- **Dostupné přechody** (buttons nebo dropdown)
- **Guard explanations** (proč něco nejde: "Assignee required", "Missing resolution")
- **Timeline/historie** (kdy přešlo do jakého stavu, kdo to udělal)

✅ **Workflow diagram:**
- Visual representation (nodes = states, edges = transitions)
- Current state highlighted
- Optional: show full diagram on hover/expand

✅ **SLA warning:**
- Pokud work item v stavu > SLA limit → warning badge
- "In Progress for 5 days (SLA: 3 days)"

**BE Impact:**
- Timeline API (GET `/api/v1/work-items/{id}/timeline`)
- SLA calculation logic

**FE Impact:**
- Workflow widget component
- Timeline component (vertical list, timestamps)
- Diagram renderer (D3.js nebo React Flow)

**Security/Tenancy:**
- Timeline viditelnost podle RBAC (hide sensitive transitions)

**Test Coverage:**
- Unit: SLA calculation
- Integration: Timeline API returns correct events
- E2E: View workflow history, verify SLA warning

**LOC:** ~400 (BE: ~100, FE: ~250, tests: ~50)  
**Effort:** 10h  
**Priority:** SHOULD HAVE (Phase 1)

---

## 📦 Story Overview (První část - Themes 1-2)

| Story | Name | Theme | LOC | Effort | Phase | Priority |
|-------|------|-------|-----|--------|-------|----------|
| AWM1 | Work Item Model | 1 | ~400 | 8h | 1 | MUST |
| AWM2 | Relace & Hierarchie | 1 | ~500 | 10h | 1 | MUST |
| AWM3 | Validace & Rules | 1 | ~300 | 6h | 1 | SHOULD |
| AWM4 | Workflow Šablony | 2 | ~400 | 8h | 1 | MUST |
| AWM5 | Workflow Engine | 2 | ~500 | 12h | 1 | MUST |
| AWM6 | Workflow Viz | 2 | ~400 | 10h | 1 | SHOULD |
| **SUBTOTAL** | **Themes 1-2** | | **~2,500** | **54h** | | |

---

**Pokračování následuje v dalším commitu (AWM7-AWM17):**
- Theme 3: Boards & Backlog (AWM7-AWM9)
- Theme 4: DMS & Komentáře (AWM10-AWM11)
- Theme 5: Reporting & Monitoring (AWM12-AWM13)
- Theme 6: Tenancy, RBAC, Licensing (AWM14-AWM15)
- Theme 7: AI/MCP Hooks (AWM16-AWM17)

---

## 🔗 Závislosti

**Vyžaduje (MUST):**
- ✅ EPIC-005: Metamodel Generator Studio (entity definitions)
- ✅ EPIC-006: Workflow Engine (workflow execution)
- ✅ EPIC-000: Security & RBAC (role-based permissions)
- ✅ EPIC-007: Multi-tenant Infrastructure (tenant isolation)

**Volitelné (NICE TO HAVE):**
- EPIC-008: DMS (document attachments - AWM10)
- EPIC-003: Monitoring (metrics dashboards - AWM13)
- EPIC-009: AI/MCP (workflow suggestions - AWM16-AWM17)
- EPIC-011: n8n (orchestration hooks)

**Žádné změny základní architektury:**
- ✅ Multi-tenant model zůstává: `tenant = subdoména = realm`
- ✅ Žádný impact na licensing core-platform (AWM15 jen připraví hooks)

---

**Status:** Část 1/3 (Themes 1-2) - Další části v samostatných commitech
