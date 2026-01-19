# EPIC-005: Metamodel Generator & Studio

> **Status:** 🟢 **Phase 1-3 IMPLEMENTED (schema diff, hot reload, UNIQUE)** | 🟡 **Phase 4+ PLANNED (Studio UI, AI, Contracts, Streaming UX)**  
> **Implementováno:** Srpen-Září 2025 (Phase 1-3)  
> **LOC:** ~15,000 řádků (generator + templates + UI) + ~8,000 plánováno (Studio, AI, Contracts)
> **Definice:** ✅ **100%** (META-004..META-021 rozpracovano s AC + tasky)

---

## 🎯 Vision

Vytvořit **low-code platformu pro generování, správu a dokumentaci entity modelu** s vizuálním editorem, AI asistentem a automatickou generací API kontraktů.

**Metamodel jako Single Source of Truth:**
- 📐 Definice entit v YAML deklarativním jazyce
- 🤖 Automatická generace Java kódu (Entity, Repository, Service, Controller)
- 🎨 Vizuální ER diagram a Use-case canvas (Miro-like)
- 🧠 AI Copilot pro návrh změn a dokumentaci
- 📜 Automatická generace OpenAPI/AsyncAPI kontraktů
- 🔄 Hot-reload změn bez restartu aplikace
- 🗄️ Správa databázového schématu (Flyway migrations)
- 🔌 Read-only Metamodel API pro n8n, MCP, ETL, reporting

**Value Proposition:**
- 🚀 **10x rychlejší vývoj** nových entit (YAML → Code → API → Docs)
- 🎨 **Konzistentní architektura** napříč projektem
- 🔄 **Jednoduché refaktoring** - změna YAML → regenerace kódu + kontraktů
- 🧪 **Testovatelnost** - generovaný kód je standardizovaný
- 📚 **Dokumentace zdarma** - YAML + AI → Markdown/OpenAPI
- 🤝 **Integrace ready** - Metamodel API pro n8n, MCP tools, třetí strany
- 🔒 **Governance** - Approval workflow, audit log, versioning

---

## 👥 Studio Users & Role Model

- **Platform / Admin tenant (globální realm):** definuje sdílené core modely, systémové entity, povoluje cross-tenant moduly a schvaluje AI návrhy. Má plný přístup k platform + tenant definicím kvůli governance.
- **Tenant admin (realm = subdoména):** může rozšiřovat model pro svůj tenant – přidávat vlastní entity, pole a relace, zapínat DMS/workflow/streaming per entita a nastavovat access matrix. Guardrails: vidí pouze svůj model + read-only platformovou část, akce jsou limitované rolemi `TENANT_METAMODEL_ADMIN` a auditované.
- **Controlled core extensions:** vybrané core entities mají označení „tenant-extendable“. Tenant admin může přes návrhový proces přidat vlastní pole/relace bez rozbití globálního modelu – vše se drží v overlay vrstvě s jasným diffem k platform originálu.
- **Studio není pouze centrální nástroj:** per-tenant capabilities jsou součástí EPICu. Studio vždy vyhodnocuje realm z Keycloak SSO a auto-injektuje `tenant_id` do všech zápisů, takže tenant admin nikdy nemodifikuje cizí data. Approved overrides se verzují odděleně (platform vs tenant spaces).
- **Sandbox + proposal režim:** každý tenant-specific zásah (nová entita, rozšíření povoleného core modelu) jde nejdřív do sandboxu + proposal fronty; publikace probíhá až po schválení guardrails (DoR/DoD + validation). Bez toho by multi-tenant use-cases nebyly možné.

### Tenant Scope Metadata
- Každá entita/field/relace má v YAML/Studio atributy:
  - `tenant_aware: true|false` – automaticky přidá `tenant_id`, filtruje API/streaming.  
  - `tenant_scope: GLOBAL | TENANT_LOCAL | SHARED` – určuje, zda model může upravovat jen admin realm, tenant admin, nebo je sdílený s RBAC kontrolou.  
  - `realm_visibility`: definuje, ve kterých realmech je entita viditelná/editovatelná.
- Studio poskytuje dvě prostředí:
  - **Global Admin Studio:** běží v admin realmu, spravuje systémové entity, shared moduly a schvaluje tenant proposals.  
  - **Tenant Studio:** běží v tenant realmu, dovoluje spravovat vlastní entity/views/pravidla v rámci guardrails (jen označené `tenant_scope`).
- Publikace vytváří sjednocený artefakt = `platform model + tenant overlay`, který konzumuje runtime (DB, API, streaming, WF).

---

## 🧱 Metamodel Studio – Funkční rozsah (MVP)

- **Správa entit a polí:** create/update/delete entit, správa polí (typy, nullable, default, enumy), relací (1:N, N:M, hierarchie), indexů (fulltext, uniq, composite). Validace probíhá v reálném čase a změny se propisují do YAML + DB migrací (viz META-001..003).
- **Deklarativní validace:** konfigurujeme pravidla jako `length`, `pattern`, `required`, cross-field constraints nebo business rules (stavové guardy) bez psaní kódu – vše jako config, který se aplikuje při generování backendu i FE formulářů.
- **DMS integrace (EPIC-008 hook):** u každé entity lze zapnout dokumentové přílohy, definovat typy dokumentů, maximální velikost a mandatory flag. Studio při publikaci doplní UI spec o „Documents“ sekce + backend storage binding.
- **Workflow integrace (EPIC-006 hook):** entita může být navázána na workflow definici, definujeme business key a mapování kontextu. Studio zapisuje binding do metamodelu a Workflow Engine (WF17) čte přímo z těchto dat.
- **Streaming (povinné, ne optional):** každá entita může emitovat CRUD eventy. Studio nastaví Kafka topic, event payload mapping (včetně maskování), correlationId a version metadata. Generátor přidá idempotentní publish hooky a AsyncAPI kontrakt.

### Streaming as Mandatory Behavior
- Každý metamodel objekt má povinnou sekci `streaming`:
  ```yaml
  streaming:
    enabled: true
    topic: events.${tenant}.${entity}
    mode: OUTBOX   # nebo DIRECT (jen po security review)
    events: [created, updated, deleted, stateChanged]
    partition_key: entityId
    idempotence: correlationId
    ordering: per_entity
  ```
- Generátor vytváří AsyncAPI kontrakty, outbox tabulky (pokud `mode: OUTBOX`), publish hooky a validuje, že runtime event odpovídá schématu.  
- Deduplikace + ordering jsou zajištěny kombinací `entityId` + `eventId`; consumer guide je součástí kontraktu.  
- DoD: žádný publish „pokud se rozhodneme“ – streaming metadata jsou povinnou částí definice entity, kontrolované validátorem a CI.

### Contracts & Documentation Auto-Generation
- Z každé entity/metadat generujeme:  
  - **OpenAPI** pro CRUD + search endpoints (včetně RBAC pravidel, validation constraints, příkladů).  
  - **AsyncAPI** pro eventové topic (CRUD + state events) – využívají n8n konektory, externí integrace a QA simulace.  
  - **Markdown/HTML** dokumentaci pro adminy/integrátory (entity popisy, access matrix, workflow/DMS/streaming bindingy) + export do MCP tools.  
- Artefakty jsou versionované, dostupné přes Metamodel API (`/api/metamodel/entities`, `/contracts/openapi`, `/contracts/asyncapi`) a tvoří jediný zdroj pravdy – žádné shadow configy mimo metamodel.
- **Access control matrix:** per entita i per field definujeme, kdo vidí/edituje/maže, kdo může spouštět workflow přechody – kombinací rolí z Keycloaku a attribute-based pravidel (claim, group). Studio vynucuje konzistenci mezi metadaty a generovanými policy třídami.

### Security Matrix Editor (Row & Column Level)
- GUI editor pro každou entitu/field/stav definuje: `CAN_READ`, `CAN_WRITE`, `CAN_DELETE`, `CAN_TRANSITION`, `CAN_ATTACH_DOCUMENT`, `CAN_TRIGGER_WORKFLOW`, včetně podmínek podle RBAC rolí, Keycloak claims, tenant scopes a stavů (`status in [DRAFT]`, `owner == currentUser`).  
- Výstup = generovaný **Access Policy Model** (YAML/JSON + Java/TS classes), který využívá backend (Spring Security, ACL, CEL) i FE guardy; žádné „shadow“ konfigurace mimo metamodel.  
- Matice jsou verzované, součást diffu a exportují se spolu s kontrakty (viz Contracts & Docs).  
- Row-level filtry se automaticky promítnou do repository/service vrstev, GraphQL REST filtrů, streaming payloadů (maskování PII).
- **ER / Model vizualizace:** ER canvas (META-004) zobrazuje platformové vs tenant-specific entity, relace (včetně DMS/WF/streaming hrany) a umožňuje highlight konkrétní tenant. Z plátna lze otevřít detail entity se všemi výše popsanými atributy.

### ER / Graph View (Definition of Done)
- Vizualizace musí zobrazovat entity, jejich typ (platform / tenant / modul), relace s direction + kardinalitou.
- Klik na entitu otevře její detail (tabulka, pole, validace, workflow/DMS/streaming binding, security matrix).
- Filtry: per modul/domain, tenant overlay, změněné entity (draft vs published), typ relace (workflow, DMS, reference).
- Změnový mód ukazuje dopady: které entity/API/WF/n8n flows budou ovlivněny (highlight edges).  
- View je součást DoD EPICu – bez něj se Metamodel Studio nepovažuje za hotové.

### Workflow & n8n Integration (EPIC-006 & EPIC-011 Hooks)
- Každá entita může mít `workflowBinding`:
  ```yaml
  workflow:
    definition: contract-approval
    business_key: ${entityId}
    start_on: CREATE
    transitions:
      submit: start
      approve: approval_node
  ```
- Studio umožní mapovat entity akce na:
  - **Interní workflow engine executory** (APPROVAL, REST_SYNC, EXTERNAL_TASK).  
  - **n8n flows** přes náš BFF konektor (EXTERNAL_TASK handshake).  
- Binding se propisuje do Workflow Ops dashboardu (EPIC-006) a do n8n provisioning služby (EPIC-011).  
- Každá změna bindingu je auditovaná a stane se součástí kontraktů (OpenAPI includes workflow metadata, AsyncAPI pro external task eventy).

---

## 🤖 AI Assistant pro Metamodel (META-005)

- Embedded chat/side panel zná aktuální metamodel (platform + tenant scope) a reaguje na dotazy typu „rozšiř Requirements o time tracking napojený na Tasks“.
- AI navrhne nové entity/pole/relace, zobrazí diff (YAML + ER náhled) a vysvětlí dopady (UI, API, workflow, streaming, DMS). Součástí je navržená access matrix, bindingy i testy.
- Součástí návrhu jsou i generované migrace, validační pravidla, streaming/contract metadata a dokumentace – vše jako součást proposal balíčku.
- Návrhy putují do **proposal fronty** (4-eye principle). Admin/tenant admin změny schvaluje nebo vrací s komentářem. Bez schválení se nic nepřenese do produkčního metamodelu.
- Po schválení Studio auto-generuje DB migrace, aktualizuje kontrakty, dokumentaci i event schémata. Každý proposal má verzování + audit.
- AI integrace stojí na MCP toolingu: `metamodel-validate`, `metamodel-diff`, `generate-api-spec`, `suggest-migrations`, takže Copilot i externí asistenti mají standardizované rozhraní.
- **Žádné auto-apply:** AI nikdy nepublikuje změny bez explicitního approve uživatele s příslušnou rolí (admin/tenant admin).

---

## 🔗 Metamodel API & Contracts

- **Public (secured) API vrstvy:**
  - `GET /api/metamodel/entities` – kompletní definice (platform + tenant overlay).
  - `GET /api/metamodel/ui-spec` – UI kontrakty pro auto-generované formuláře/tab views.
  - `GET /api/metamodel/workflows/{entity}` – binding na workflow engine včetně business key.
  - `GET/POST /api/metamodel/proposals` – čtení a zakládání návrhů (AI, n8n, MCP).
- **Generované kontrakty:**
  - OpenAPI pro CRUD/aggregate endpoints (per entity) + modulární BFF.
  - AsyncAPI pro streaming/event topics (CRUD, state changes, SLA).
  - Dokumentace pro integrace (Markdown, ER exports) a n8n connector metadata – přímo využívá EPIC-011.
- API je multi-tenant safe (JWT realm filter), sledované v audit logu a verzované, takže n8n/workflow/test automation může bezpečně číst kontrakty a návrhy.

---

## 📡 Streaming & Serializace

- Všechny změny dat entit (CRUD) produkují Kafka eventy s **garantovanou sekvencí per `entityId`** (partition key) a idempotentními offsety.
- Schéma eventů je vázané na metamodel verzi; generátor publikuje schema registry entry (Avro/JSON Schema) s `schemaVersion` = git SHA návrhu.
- Payload obsahuje `version`, `correlationId`, `tenantId`, `entityType`, diff i plný snapshot (konfigurovatelné). Consumer strana má instrukce pro deduplikaci podle `eventId`.
- Streaming není volitelný modul; Studio defaultně povoluje publish pro všechny tenant-aware entity a umožňuje pouze jemné nastavení obsahu/retence.

---

## 🌐 Multi-Tenant Model (neměnný základ)

- Tenant = Keycloak realm = subdoména (`{tenant}.core-platform.{tld}`). Žádné sdílené realmy ani ruční přepínání; SSO určuje kontext Studia.
- Metamodel se skládá ze **shared/platform** části (spravuje admin realm) a **tenant overrides** (spravuje daný tenant). Každá část má vlastní verzi a audit trail, ale publikace generuje sjednocený artefakt.
- Studio UI respektuje realms: admin realm vidí vše (platform + tenant overlay), tenant admin vidí jen platform read-only + své entity k editaci. Exporty (YAML, OpenAPI, AsyncAPI) jsou implicitně scoped na aktuální realm.

## ⚙️ Technical Guidelines

- **Frontend:** používá existující metamodel/workflow stack (React, MUI, React Flow, React Query). Komponenty pro ER view, security matrix i DMS/Workflow binding jsou generické a reusabilní v dalších modulech.
- **Backend:** validace, SLA, security matrix enforcement i streaming metadata se počítají server-side. Žádné přímé napojení AI nebo FE na DB – vše přes metamodel služby.
- **No shadow config:** bezpečnost, streaming, workflow hooky, API kontrakty, documentation exporty i AI proposals jsou definovány v metamodelu (YAML/JSON + registry). Pokud něco není v metamodelu, nesmí to vzniknout v runtime – to kontroluje validátor a CI.

---

## ✅ Definition of Done (EPIC-005)

1. **Metamodel Core (Phase 1-3 – DONE):** schema diff engine, hot reload API, UNIQUE constraints a migrace běží v produkci.  
2. **Studio & Tenant Model:** existuje Global Admin Studio + Tenant Studio, entity mají `tenant_aware`, `tenant_scope`, guardrails a sandbox proposal flow.  
3. **Security Matrix:** vizuální editor generuje RBAC/ABAC policies pro entity/fields/states a exportuje je do backendu/FE.  
4. **Streaming:** každá entita má povinná streaming metadata (topic, mode, events). Runtime publikuje eventy dle AsyncAPI a garantuje ordering/idempotence.  
5. **Workflow & n8n binding:** entity mohou definovat workflow hooks + n8n flows; binding se promítá do EPIC-006/011 komponent.  
6. **ER / Impact View:** graf s filtrováním, kardinalitou a dopadovou analýzou diffů.  
7. **Contracts & Docs:** generujeme OpenAPI, AsyncAPI, TS typy, n8n connector metadata a admin/integration dokumentaci z metamodelu; dostupné přes Metamodel API.  
8. **AI-assisted modeling:** MCP-based asistent navrhuje změny (entity/validace/streaming/docs) jako drafty, nikdy nic neaplikuje bez schválení.  
9. **Multi-tenant runtime:** publikace vytvoří sjednocený artefakt (platform + tenant overlay) používaný DB/API/streaming/workflow, bez shadow configů.

---

## 📊 Progress Overview

**Overall Completion:** 🟡 **14% (3/21 stories done; Phase 1-3 DONE)**

| Phase | Feature | Stories | Status | Completion |
|-------|---------|---------|--------|------------|
| **Phase 1** | Schema Diff Detection | META-001 | ✅ DONE | 100% |
| **Phase 2** | Hot Reload API | META-002 | ✅ DONE | 100% |
| **Phase 3** | UNIQUE Constraints | META-003 | ✅ DONE | 100% |
| **Phase 4** | Visual Studio (ER Canvas + Use-case) | META-004 | 📋 PLANNED | 0% |
| **Phase 5** | AI Copilot Integration | META-005 | 📋 PLANNED | 0% |
| **Phase 6** | Contracts & Documentation | META-006 | 📋 PLANNED | 0% |
| **Phase 7** | Governance & Approval Flow | META-007 | 📋 PLANNED | 0% |
| **Phase 8** | Advanced Constraints | META-008 | 📋 PLANNED | 0% |

**Total Stories:** 21 (3 complete, 18 planned)  
**Implementation Time:** ~3 weeks (Phase 1-3) + TBD for Phase 4+

---

## 🏛️ Architecture Overview

### Metamodel Ecosystem

```
┌────────────────────────────────────────────────────────────────────┐
│                     METAMODEL STUDIO (Frontend)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │  ER Canvas   │  │  Use-case    │  │  AI Copilot Panel    │    │
│  │  (Graph View)│  │  Canvas      │  │  (Chat + Suggestions)│    │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘    │
│         │                  │                     │                 │
└─────────┼──────────────────┼─────────────────────┼─────────────────┘
          │                  │                     │
          ▼                  ▼                     ▼
┌────────────────────────────────────────────────────────────────────┐
│                  METAMODEL API (Backend)                           │
│  ┌──────────────────┐  ┌───────────────┐  ┌──────────────────┐   │
│  │  Read-only API   │  │  Change Mgmt  │  │  AI Tools API    │   │
│  │  (n8n, MCP, ETL) │  │  (Approval)   │  │  (MCP Protocol)  │   │
│  └──────────────────┘  └───────────────┘  └──────────────────┘   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │           METAMODEL REGISTRY (YAML Storage)                  │ │
│  │  - Entities (Platform + Tenant-specific)                     │ │
│  │  - Relations (1:N, M:N, References, DMS, Workflow)          │ │
│  │  - Security Policies (RBAC, Row-level)                      │ │
│  │  - Validation Rules (Constraints, Types)                    │ │
│  │  - Use-cases (Flows, Dependencies)                          │ │
│  └──────────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────────┐
│                    CODE GENERATOR ENGINE                           │
│  ┌────────────┐  ┌────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │ Java Code  │  │ OpenAPI    │  │ AsyncAPI    │  │ Docs (MD)  │ │
│  │ Generator  │  │ Generator  │  │ Generator   │  │ Generator  │ │
│  └────────────┘  └────────────┘  └─────────────┘  └────────────┘ │
└────────────────────────────────────────────────────────────────────┘
          │                  │                │              │
          ▼                  ▼                ▼              ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐
│ Spring Boot  │  │ OpenAPI Spec │  │ Event Schema │  │ Markdown   │
│ Entities     │  │ (REST API)   │  │ (Streaming)  │  │ Docs       │
└──────────────┘  └──────────────┘  └──────────────┘  └────────────┘
```

**Key Principles:**
- ✅ **Metamodel = Single Source of Truth** (YAML repository)
- ✅ **Read-only API** pro integrace (n8n, MCP, ETL)
- ✅ **Change Management** s approval workflow (human-in-the-loop)
- ✅ **AI Assistant** navrhuje, člověk schvaluje
- ✅ **Automatic Contracts** (OpenAPI, AsyncAPI) z metamodelu
- ✅ **Multi-tenant Aware** (Platform vs Tenant metamodel space)

---

## 🎯 Implemented Stories

### ✅ META-001: Schema Diff Detection Engine
**Implementováno:** 2025-09-15  
**LOC:** ~600 řádků  
**Status:** 🟢 DONE

**Funkce:**
- Porovnání YAML definic s aktuálním DB schématem
- Detekce změn (ADD COLUMN, ALTER TYPE, ALTER NULLABLE)
- Klasifikace změn jako SAFE vs RISKY
- Automatická aplikace safe změn, skip risky s warnings

**Komponenty:**
```
backend/src/main/java/cz/muriel/core/metamodel/schema/
├── MetamodelSchemaGenerator.java
│   ├── detectChanges() - hlavní diff engine
│   ├── getCurrentColumns() - čte DB schema z information_schema
│   ├── detectColumnChanges() - YAML vs DB comparison
│   ├── applyChanges() - execute safe DDL
│   └── typesMatch() - inteligentní type matching
│
├── TypeConversionRegistry.java
│   ├── Safe conversions: VARCHAR→TEXT, INTEGER→BIGINT
│   └── Risky conversions: TEXT→VARCHAR, BIGINT→INTEGER
│
├── SchemaDiff.java
│   ├── ColumnChange (ADD, ALTER_TYPE, ALTER_NULLABLE)
│   ├── IndexChange
│   ├── ConstraintChange
│   └── TriggerChange
│
└── ColumnInfo.java
    └── DB column metadata (type, nullable, default, FK)
```

**Test Results:**
- ✅ Detected 17 changes across 3 entities (User, Role, Group)
- ✅ Applied 10 safe changes (ADD COLUMN)
- ⚠️ Skipped 9 risky changes (type conversions, NOT NULL)
- ✅ Created version triggers for optimistic locking

**Value:**
- Eliminuje ruční DDL scripty
- Bezpečná evoluce schématu
- Auditovatelné změny

---

### ✅ META-002: Hot Reload REST API
**Implementováno:** 2025-09-20  
**LOC:** ~200 řádků  
**Status:** 🟢 DONE

**REST Endpoints:**

#### 1. `GET /api/admin/metamodel/reload`
Reload YAML definitions bez restartu serveru.

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
          "risky": false,
          "newType": "VARCHAR(255)"
        }
      ]
    }
  }
}
```

#### 2. `POST /api/admin/metamodel/apply-safe-changes`
Aplikuje všechny safe changes detekované z YAML.

**Behavior:**
- ✅ ADD COLUMN operations
- ✅ CREATE INDEX
- ✅ CREATE UNIQUE constraints
- ⚠️ Skip risky ops (type conversions, NOT NULL)

#### 3. `GET /api/admin/metamodel/status`
Health check - pending changes overview.

**Component:**
```java
@RestController
@RequestMapping("/api/admin/metamodel")
public class MetamodelAdminController {
  
  @GetMapping("/reload")
  public ResponseEntity<?> reloadMetamodel() {
    // Hot reload + diff
  }
  
  @PostMapping("/apply-safe-changes")
  public ResponseEntity<?> applySafeChanges() {
    // Execute DDL
  }
  
  @GetMapping("/status")
  public ResponseEntity<?> getStatus() {
    // Health check
  }
}
```

**Workflow:**
```bash
# 1. Edit YAML
vim backend/src/main/resources/metamodel/user.yaml

# 2. Reload without restart
curl http://localhost:8080/api/admin/metamodel/reload

# 3. Review changes

# 4. Apply if safe
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes
```

**Value:**
- Zero-downtime schema updates
- Controlled change deployment
- API-driven metamodel management

---

### ✅ META-003: UNIQUE Constraint Management
**Implementováno:** 2025-09-22  
**LOC:** ~50 řádků  
**Status:** 🟢 DONE

**Features:**
- Auto-create UNIQUE constraints from YAML `unique: true`
- Idempotent creation (check existence first)
- Standard naming: `uk_{table}_{column}`

**YAML Example:**
```yaml
# backend/src/main/resources/metamodel/user.yaml
entity: User
table: users_directory

fields:
  - name: username
    type: string
    unique: true  # ← AUTO-CREATES: uk_users_directory_username
  
  - name: email
    type: email
    unique: true  # ← AUTO-CREATES: uk_users_directory_email
```

**Generated DDL:**
```sql
ALTER TABLE users_directory 
  ADD CONSTRAINT uk_users_directory_username UNIQUE (username);

ALTER TABLE users_directory 
  ADD CONSTRAINT uk_users_directory_email UNIQUE (email);
```

**Implementation:**
```java
private void createUniqueConstraints(EntitySchema schema) {
  for (FieldSchema field : schema.getFields()) {
    if (Boolean.TRUE.equals(field.getUnique())) {
      String constraintName = "uk_" + schema.getTable() + "_" + field.getName();
      
      // Check existence
      if (!constraintExists(schema.getTable(), constraintName)) {
        String sql = String.format(
          "ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)",
          schema.getTable(), constraintName, field.getName()
        );
        jdbcTemplate.execute(sql);
      }
    }
  }
}
```

**Value:**
- Declarative constraints in YAML
- Automatic DB enforcement
- No manual DDL for constraints

---

## 📋 Planned Stories (Phase 4-8)

### 📋 META-004: Visual Metamodel Studio (ER Canvas + Use-case Canvas)
**Priority:** P1  
**Estimate:** 3 weeks  
**Status:** PLANNED

#### Overview

**Metamodel Studio** poskytuje dva hlavní vizuální pohledy:

**1. ER / Graph View (Entity-Relationship Canvas)**

**Purpose:** Vizualizace a navigace metamodelu jako graph database

**Uzly:**
- 🟦 **Platform Entities** (globální, vidí všichni tenanti)
  - Core: `User`, `Role`, `Group`, `Tenant`
  - RBAC: `Permission`, `Scope`
  - Workflow: `WorkflowDefinition`, `WorkflowInstance`
  - DMS: `Document`, `DocumentVersion`
- 🟩 **Tenant-specific Entities** (vlastní entity tenantu)
  - Příklad: `Project`, `Requirement`, `DeliveryItem`, `CustomField`
- 🟨 **Module Entities** (moduly, viz EPIC-017)
  - Příklad: `delivery-suite.DeliveryItem`, `helpdesk.Ticket`

**Hrany (Relace):**
- `1:N` - One-to-Many (User → Projects)
- `M:N` - Many-to-Many (User ↔ Groups)
- `REFERENCE` - Foreign Key (Project → Owner:User)
- `EMBEDDED` - Kompozice (Address ⊂ User)
- `DMS` - Document attachment (Requirement → Documents)
- `WORKFLOW` - Process binding (Project → WorkflowInstance)

**Filtrování:**
```typescript
// Příklad filtrů v UI
filters: {
  domain: ['CRM', 'Projects', 'DMS', 'Monitoring', 'Workflow'],
  module: ['core', 'delivery-suite', 'helpdesk', 'custom'],
  tenant: 'acme-corp',  // Tenant vidí: platform entities + své
  entityType: ['PLATFORM', 'TENANT_SPECIFIC', 'MODULE'],
  hasRelations: true,
  hasWorkflow: true,
  hasDMS: true
}
```

**Interaktivní funkce:**

1. **Klik na entitu** → Otevře detail panel:
   ```
   ┌─────────────────────────────────────────┐
   │ Entity: Project                         │
   ├─────────────────────────────────────────┤
   │ Table: projects                         │
   │ Type: TENANT_SPECIFIC                   │
   │ Tenant-aware: true                      │
   │                                         │
   │ Fields: (12)                            │
   │ ✓ id, name, description, status         │
   │ ✓ owner_id → User                       │
   │ ✓ tenant_id (auto-injected)            │
   │                                         │
   │ Relations: (3)                          │
   │ → Requirements (1:N)                    │
   │ → Documents (DMS)                       │
   │ → WorkflowInstance (WORKFLOW)           │
   │                                         │
   │ Validations:                            │
   │ • name: required, max 255               │
   │ • status: enum [NEW, ACTIVE, CLOSED]    │
   │                                         │
   │ Security:                               │
   │ • CREATE: ROLE_PROJECT_MANAGER          │
   │ • READ: ROLE_USER                       │
   │ • UPDATE: owner OR ROLE_ADMIN           │
   │                                         │
   │ Streaming:                              │
   │ • Events: project.created, *.updated    │
   │ • CDC: enabled                          │
   │                                         │
   │ [Edit] [Clone] [Export YAML]            │
   └─────────────────────────────────────────┘
   ```

2. **Zvýraznit závislosti:**
   - Hover na `Project` → Zvýrazní: `User`, `Requirement`, `Document`, `WorkflowInstance`
   - "What uses this entity?" - Zobrazí reverse dependencies
   - "What does this entity depend on?" - Zobrazí forward dependencies

3. **Kontrola konzistence:**
   - ❌ **Chybějící reference:** `Project.owner_id` → `User` (entity `User` neexistuje)
   - ⚠️ **Cykly:** `A → B → C → A` (detekce circular dependencies)
   - ❌ **Nesoulad typů:** `Project.owner_id: UUID` vs `User.id: BIGINT`
   - ⚠️ **Orphaned entities:** Entity bez vztahů (možná testovací)

**Rendering:**
- Tech: React Flow / D3.js / Cytoscape.js
- Layout: Force-directed graph (entities se odpuzují, relace přitahují)
- Zoom: In/Out, Pan, Mini-map
- Export: PNG, SVG, GraphML

---

**2. Use-case / Flow Canvas (Miro-like Diagram)**

**Purpose:** Dokumentace business procesů a jejich vazeb na metamodel

**Uzly:**
- 📋 **Use-cases** (business scénáře)
  - Příklad: "Create Project", "Approve Requirement", "Generate Report"
- ⚙️ **Process Steps** (workflow kroky)
  - Příklad: "Validate Input", "Send Notification", "Update Status"
- 🔗 **Entity Links** (které entity se používají)
  - Příklad: Use-case "Create Project" → Entity `Project`, `User`, `Workflow`
- 🔌 **Integration Links** (konektory)
  - Příklad: Use-case "Send Email" → Connector `SMTP`, n8n flow `email_notification`

**Canvas Layout:**
```
┌────────────────────────────────────────────────────────────────┐
│  Use-case: Create Project with Approval                       │
│                                                                │
│  [START]                                                       │
│     │                                                          │
│     ▼                                                          │
│  ┌──────────────┐                                             │
│  │ 1. User fills│  Links: Entity:Project, Entity:User         │
│  │    form      │                                             │
│  └──────┬───────┘                                             │
│         │                                                      │
│         ▼                                                      │
│  ┌──────────────┐                                             │
│  │ 2. Validate  │  Links: ValidationRule:project_name_unique  │
│  │    input     │                                             │
│  └──────┬───────┘                                             │
│         │                                                      │
│         ▼                                                      │
│  ┌──────────────┐                                             │
│  │ 3. Create    │  Links: Entity:Project, API:POST /projects  │
│  │    Project   │         Workflow:project_approval_v1        │
│  └──────┬───────┘                                             │
│         │                                                      │
│         ▼                                                      │
│  ┌──────────────┐                                             │
│  │ 4. Start WF  │  Links: WorkflowEngine, n8n:notify_manager  │
│  │    Approval  │                                             │
│  └──────┬───────┘                                             │
│         │                                                      │
│         ▼                                                      │
│  ┌──────────────┐                                             │
│  │ 5. Notify    │  Links: Connector:Email, DMS:attach_doc     │
│  │    Manager   │                                             │
│  └──────┬───────┘                                             │
│         │                                                      │
│         ▼                                                      │
│  [END]                                                         │
│                                                                │
│  Dependencies:                                                 │
│  • Entities: Project, User, WorkflowInstance                  │
│  • Workflows: project_approval_v1                             │
│  • APIs: POST /projects, GET /users/{id}                      │
│  • Connectors: SMTP, n8n                                      │
│  • DMS: Document upload                                       │
└────────────────────────────────────────────────────────────────┘
```

**Funkce:**
- **Živá dokumentace:** Use-case je vždy aktuální (linky na metamodel)
- **Dohledatelnost:** Klik na "Entity:Project" → Přeskok do ER View
- **Dependency tracking:** "Co se rozbije, když změním Project entity?"
- **AI integration:** AI navrhuje optimalizace use-case (viz META-005)

**Tech Stack:**
- React Flow nebo Excalidraw-like canvas
- Markdown notes pro use-case description
- YAML export (use-case definice)

---

#### Implementation Details

**Backend API:**
```java
@RestController
@RequestMapping("/api/metamodel")
public class MetamodelViewController {
  
  /**
   * Get ER graph data for visualization
   */
  @GetMapping("/graph")
  public MetamodelGraph getGraph(
    @RequestParam(required = false) String tenant,
    @RequestParam(required = false) List<String> domains,
    @RequestParam(required = false) List<String> modules
  ) {
    // Returns: nodes (entities), edges (relations), metadata
  }
  
  /**
   * Get entity detail
   */
  @GetMapping("/entities/{name}")
  public EntityDetail getEntityDetail(@PathVariable String name) {
    // Returns: fields, relations, validations, security, streaming, ...
  }
  
  /**
   * Get use-cases
   */
  @GetMapping("/use-cases")
  public List<UseCase> getUseCases() {
    // Returns: use-case definitions with linked entities/workflows/connectors
  }
  
  /**
   * Validate metamodel consistency
   */
  @PostMapping("/validate")
  public ValidationReport validate() {
    // Returns: errors (missing refs, cycles, type mismatches)
  }
}
```

**Frontend Components:**
```typescript
// ER Canvas Component
<ERCanvas
  entities={metamodel.entities}
  relations={metamodel.relations}
  filters={{ domain: 'Projects', tenant: 'acme-corp' }}
  onEntityClick={(entity) => openDetailPanel(entity)}
  onHighlightDependencies={(entity) => highlightGraph(entity)}
/>

// Use-case Canvas Component
<UseCaseCanvas
  useCases={metamodel.useCases}
  onStepClick={(step) => navigateToEntity(step.entityLink)}
  onExport={(format) => exportDiagram(format)}
/>
```

**Value:**
- 📊 **Visual understanding** metamodelu (ER diagram pro onboarding)
- 🔍 **Quick navigation** (klik na entitu → detail, dependency graph)
- ✅ **Consistency checks** (pre-commit validace)
- 📚 **Living documentation** (use-cases linkují na metamodel)

---

### 📋 META-005: Metamodel AI Copilot
**Priority:** P1  
**Estimate:** 2 weeks  
**Status:** PLANNED

#### Overview

**AI Copilot** je embedded chat panel v Metamodel Studiu pro:
- 🧠 Asistence při návrhu změn metamodelu
- 📝 Generování dokumentace a changelogů
- ⚠️ Analýzu dopadů změn (breaking changes, migrace)
- 🎯 Návrhy optimalizací (indexy, normalizace, security)

**Knowledge Base (Co AI zná):**
- ✅ **Aktuální metamodel** (všechny entity, relace, fields)
- ✅ **ER/Use-case diagram** (vizuální kontext)
- ✅ **Security pravidla** (RBAC policies, row-level security)
- ✅ **Historie změn** (audit log, kdo co změnil, kdy)
- ✅ **Dostupné konektory** (n8n, MCP, SMTP, Slack, ...)
- ✅ **Workflow definice** (workflow engine templates)
- ✅ **DMS pravidla** (retention policies, storage limits)

---

#### Capabilities

**1. Analýza návrhu uživatele**

**Příklad konverzace:**
```
User: "Chci přidat time-tracking k Requirements"

AI: Analyzuji váš požadavek...

✅ Návrh změn:

1. Nová entita: TimeEntry
   - Fields:
     * id: UUID
     * requirement_id: UUID (FK → Requirement)
     * user_id: UUID (FK → User)
     * hours: DECIMAL(5,2)
     * date: DATE
     * description: TEXT
     * tenant_id: UUID (auto-injected)
   
   - Relations:
     * Requirement.timeEntries: 1:N → TimeEntry
     * User.timeEntries: 1:N → TimeEntry
   
   - Validations:
     * hours: required, min=0.1, max=24
     * date: required, not future
     * requirement_id + user_id + date: unique constraint

2. Rozšíření entity Requirement:
   - Computed field: totalHours (SUM of TimeEntry.hours)
   - API endpoint: GET /requirements/{id}/time-entries

3. Workflow integration:
   - Optional: Time approval workflow (manager reviews entries)

⚠️ Dopady:
- Migration: Add table time_entries + FK constraints
- API: New endpoint /time-entries (CRUD)
- Security: Who can log time? (ROLE_USER or owner only)
- Breaking change: NO (pouze additive)

📊 Effort estimate:
- Backend: ~300 LOC, 4h
- Frontend: ~200 LOC (time entry form, summary view), 3h
- Migration: 1 Flyway script, 15 min
- Tests: ~150 LOC, 2h

✅ Doporučení:
1. Vytvořit TimeEntry entity
2. Přidat indexy: (requirement_id), (user_id), (date)
3. Security: Pouze owner požadavku nebo ROLE_ADMIN může editovat
4. Consider: Reporting dashboard (weekly time summary)

Mám vygenerovat YAML definici?
```

---

**2. Návrh změn (Structured Diff)**

**AI generuje:**

```yaml
# ai_proposal_20251110_timetracking.yaml
proposal_id: timetracking_001
created_at: 2025-11-10T14:30:00Z
author: AI Copilot
reviewed_by: null  # Čeká na schválení

changes:
  - type: ADD_ENTITY
    entity:
      name: TimeEntry
      table: time_entries
      tenant_aware: true
      fields:
        - name: id
          type: uuid
          primary: true
        - name: requirement_id
          type: uuid
          nullable: false
          foreign_key:
            entity: Requirement
            on_delete: CASCADE
        - name: user_id
          type: uuid
          nullable: false
          foreign_key:
            entity: User
        - name: hours
          type: decimal
          precision: 5
          scale: 2
          nullable: false
          validations:
            - type: min
              value: 0.1
            - type: max
              value: 24
        - name: date
          type: date
          nullable: false
          validations:
            - type: not_future
        - name: description
          type: text
      indexes:
        - columns: [requirement_id]
        - columns: [user_id]
        - columns: [date]
        - columns: [requirement_id, user_id, date]
          unique: true
      security:
        create: ROLE_USER
        read: owner OR ROLE_ADMIN
        update: owner OR ROLE_ADMIN
        delete: ROLE_ADMIN

  - type: MODIFY_ENTITY
    entity: Requirement
    changes:
      - add_computed_field:
          name: totalHours
          type: decimal
          expression: "SELECT SUM(hours) FROM time_entries WHERE requirement_id = this.id"
      - add_relation:
          name: timeEntries
          type: one_to_many
          target: TimeEntry
          inverse: requirement

rationale: |
  User requested time-tracking for Requirements. 
  
  Design decisions:
  - Separate TimeEntry entity (normalized, not embedded)
  - Unique constraint prevents duplicate entries (same user, date, requirement)
  - Cascade delete: Pokud se smaže Requirement, smažou se i TimeEntry
  - Row-level security: Pouze owner nebo admin mohou upravovat
  
  Future enhancements:
  - Time approval workflow
  - Reporting dashboard (Grafana)
  - Export to XLSX

migration:
  flyway_script: V20251110143000__add_time_tracking.sql
  estimated_downtime: 0 seconds (additive change)
  rollback_strategy: DROP TABLE time_entries

impacted_components:
  - backend/entities: +1 entity, +1 relation
  - backend/api: +5 endpoints (CRUD TimeEntry)
  - frontend: +2 views (time entry form, summary)
  - tests: +3 test files

breaking_changes: false
```

---

**3. Dokumentace (Markdown)**

**AI generuje:**

```markdown
# Time Tracking Feature - Design Document

**Author:** AI Copilot  
**Date:** 2025-11-10  
**Status:** Proposal (Pending Approval)

## Overview

Time-tracking umožní uživatelům logovat odpracované hodiny na Requirements.

## Entity: TimeEntry

**Table:** `time_entries`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | UUID | PK | Primary key |
| `requirement_id` | UUID | FK → Requirement, NOT NULL | Which requirement |
| `user_id` | UUID | FK → User, NOT NULL | Who logged time |
| `hours` | DECIMAL(5,2) | NOT NULL, 0.1-24 | Hours worked |
| `date` | DATE | NOT NULL, not future | When |
| `description` | TEXT | - | What was done |
| `tenant_id` | UUID | NOT NULL | Tenant isolation |

**Constraints:**
- `UNIQUE (requirement_id, user_id, date)` - Prevents duplicate entries

**Indexes:**
- `(requirement_id)` - Query by requirement
- `(user_id)` - Query by user
- `(date)` - Query by date

## Relations

**Requirement ↔ TimeEntry (1:N)**
- Requirement has many TimeEntry
- TimeEntry belongs to one Requirement

**User ↔ TimeEntry (1:N)**
- User has many TimeEntry
- TimeEntry belongs to one User

## API Endpoints

**TimeEntry CRUD:**
- `POST /api/time-entries` - Log time
- `GET /api/time-entries?requirement={id}` - Get entries for requirement
- `GET /api/time-entries?user={id}&date={date}` - Get user's daily log
- `PUT /api/time-entries/{id}` - Update entry
- `DELETE /api/time-entries/{id}` - Delete entry

**Requirement extension:**
- `GET /api/requirements/{id}/time-entries` - All time entries
- `GET /api/requirements/{id}/total-hours` - Computed total

## Security

**Who can log time?**
- Any user with `ROLE_USER`

**Who can edit/delete?**
- Entry owner OR `ROLE_ADMIN`

**Row-level security:**
- Users see only their own entries (unless ADMIN)

## Migration

**Flyway Script:** `V20251110143000__add_time_tracking.sql`

```sql
CREATE TABLE time_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requirement_id UUID NOT NULL REFERENCES requirements(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  hours DECIMAL(5,2) NOT NULL CHECK (hours >= 0.1 AND hours <= 24),
  date DATE NOT NULL CHECK (date <= CURRENT_DATE),
  description TEXT,
  tenant_id UUID NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (requirement_id, user_id, date)
);

CREATE INDEX idx_time_entries_requirement ON time_entries(requirement_id);
CREATE INDEX idx_time_entries_user ON time_entries(user_id);
CREATE INDEX idx_time_entries_date ON time_entries(date);
```

**Downtime:** 0 seconds (additive change)

## Testing

**Unit Tests:**
- TimeEntry entity validation
- Unique constraint enforcement
- Hours range validation

**Integration Tests:**
- CRUD operations
- Query by requirement/user/date
- Security (owner-only update)

**E2E Tests:**
- User logs time → appears in requirement summary
- User cannot edit other user's entries
- Admin can see/edit all entries

## Future Enhancements

1. **Time Approval Workflow**
   - Manager reviews weekly time entries
   - Approve/Reject workflow

2. **Reporting Dashboard**
   - Grafana dashboard: Time by user, by requirement, by week
   - Export to XLSX

3. **n8n Integration**
   - Automatic reminders (log time daily)
   - Weekly summary emails

---

**Review Required:** Platform Metamodel Admin  
**Estimated Implementation:** 9h (Backend 4h, Frontend 3h, Tests 2h)
```

---

**4. Changelog / Rationale**

**AI vysvětluje rozhodnutí:**

```
Proč jsem navrhl TimeEntry jako separátní entitu (ne embedded field)?

✅ Reasons for separate entity:
1. Normalizace: Každý záznam je unikátní (user + date + requirement)
2. Queryability: Snadno dotazovat "všechny záznamy uživatele X"
3. Auditing: Historie změn (kdo upravil entry)
4. Future-proof: Později lze přidat approval workflow

❌ Alternativa (embedded field):
- Requirement.time_entries: JSONB array
- Cons:
  * Těžko dotazovat (JSON query)
  * Žádná referenční integrita (FK)
  * Horší audit trail

Doporučení: Separate entity (standard best practice)
```

---

#### MCP Tools Integration

**AI komunikuje přes Model Context Protocol (MCP):**

**Tools poskytované Metamodel Studio:**

```typescript
// MCP Tool: get_metamodel
{
  name: "get_metamodel",
  description: "Get current metamodel (entities, relations, security)",
  input_schema: {
    tenant: "optional", // Filter by tenant
    domain: "optional", // Filter by domain (CRM, Projects, ...)
  },
  handler: async (input) => {
    return await metamodelRegistry.getMetamodel(input.tenant, input.domain);
  }
}

// MCP Tool: propose_changes
{
  name: "propose_changes",
  description: "AI proposes metamodel changes (structured diff)",
  input_schema: {
    changes: "object", // YAML-like change description
    rationale: "string",
  },
  handler: async (input) => {
    // Validate syntax
    const proposal = await changeManager.createProposal(input.changes, input.rationale);
    return { proposal_id: proposal.id, status: "pending_approval" };
  }
}

// MCP Tool: validate_changes
{
  name: "validate_changes",
  description: "Validate proposed changes (consistency, breaking changes)",
  input_schema: {
    proposal_id: "string",
  },
  handler: async (input) => {
    const validation = await changeValidator.validate(input.proposal_id);
    return {
      valid: validation.errors.length === 0,
      errors: validation.errors,
      warnings: validation.warnings,
      impacted_components: validation.impactedComponents,
    };
  }
}

// MCP Tool: generate_docs
{
  name: "generate_docs",
  description: "Generate documentation (Markdown) from proposal",
  input_schema: {
    proposal_id: "string",
    format: "markdown | html | pdf",
  },
  handler: async (input) => {
    const docs = await docGenerator.generate(input.proposal_id, input.format);
    return { content: docs, download_url: "/api/docs/..." };
  }
}

// MCP Tool: create_use_case_diagram
{
  name: "create_use_case_diagram",
  description: "Generate use-case diagram linking entities/workflows/connectors",
  input_schema: {
    use_case_name: "string",
    steps: "array", // Process steps
    linked_entities: "array",
    linked_workflows: "array",
  },
  handler: async (input) => {
    const diagram = await useCaseGenerator.create(input);
    return { diagram_id: diagram.id, svg_url: "/api/use-cases/..." };
  }
}
```

---

#### Approval Workflow (Human-in-the-Loop)

**Aplikace změn je VŽDY dvoufázová:**

**Fáze 1: AI vytvoří návrh**
```
User: "Přidej time-tracking k Requirements"
  ↓
AI: Analyzuje, navrhne změny
  ↓
AI: Vytvoří proposal (YAML diff + docs + changelog)
  ↓
Proposal uložen do: proposals/timetracking_001.yaml
  ↓
Status: PENDING_APPROVAL
```

**Fáze 2: Člověk schválí**
```
Admin: Otevře proposal v Studiu
  ↓
Admin: Review diff, docs, impacted components
  ↓
Admin: Schválí (ROLE: PLATFORM_METAMODEL_ADMIN nebo TENANT_METAMODEL_ADMIN)
  ↓
System: Aplikuje změny do metamodelu
  ↓
System: Regeneruje kód, kontrakty, dokumentaci
  ↓
Status: APPROVED + APPLIED
```

**Role-based Approval:**
- `PLATFORM_METAMODEL_ADMIN` - Může měnit platform entities (User, Role, ...)
- `TENANT_METAMODEL_ADMIN` - Může měnit tenant-specific entities (Project, CustomField, ...)
- AI **nemůže sama přímo měnit** produkční metamodel

---

#### UI/UX

**AI Copilot Panel:**

```
┌────────────────────────────────────────────────────────┐
│  🧠 Metamodel AI Copilot                              │
├────────────────────────────────────────────────────────┤
│                                                        │
│  User: Chci přidat time-tracking k Requirements       │
│                                                        │
│  AI: Analyzuji váš požadavek... ✓                     │
│                                                        │
│  Navrhuji:                                             │
│  • Nová entita: TimeEntry                             │
│  • Relace: Requirement ↔ TimeEntry (1:N)              │
│  • API endpoints: POST /time-entries, ...             │
│                                                        │
│  ⚠️ Dopady:                                            │
│  • Migration: Add table time_entries                  │
│  • Breaking change: NO                                │
│                                                        │
│  [📄 View Full Proposal] [✅ Generate YAML]           │
│                                                        │
│  ────────────────────────────────────────────────     │
│                                                        │
│  User: Vygeneruj YAML                                 │
│                                                        │
│  AI: ✓ Vygenerováno:                                  │
│      proposals/timetracking_001.yaml                  │
│                                                        │
│  [📋 Copy to Clipboard] [👁️ Preview] [✅ Submit]      │
│                                                        │
└────────────────────────────────────────────────────────┘
```

**Value:**
- 🧠 **AI asistent** navrhuje změny (ne implementuje)
- 📝 **Auto-dokumentace** (Markdown, changelog, rationale)
- ⚠️ **Impact analysis** (breaking changes, migration effort)
- ✅ **Human approval** required (governance)
- 🔗 **MCP integration** (AI tools pro metamodel)

**Workflow:**
1. Open Metamodel Studio UI
2. Drag entity to canvas
3. Add fields (name, type, constraints)
4. Define relationships (1:N, N:M)
5. Preview generated code
6. Export or deploy to backend

---

## 🏗️ Architecture

### YAML Metamodel Format

```yaml
# Example: Product entity
entity: Product
table: products
tenant_aware: true

fields:
  - name: name
    type: string
    length: 255
    nullable: false
    unique: true
  
  - name: description
    type: text
    nullable: true
  
  - name: price
    type: decimal
    precision: 10
    scale: 2
    nullable: false
  
  - name: category_id
    type: long
    nullable: false

relationships:
  - type: many_to_one
    target: Category
    field: category
    join_column: category_id
  
  - type: one_to_many
    target: OrderItem
    mapped_by: product
    field: orderItems

indexes:
  - columns: [name]
    unique: true
  - columns: [category_id, name]
```

### Generated Java Code

**Entity:**
```java
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product extends TenantAwareEntity {
  
  @Column(name = "name", length = 255, nullable = false, unique = true)
  private String name;
  
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;
  
  @Column(name = "price", precision = 10, scale = 2, nullable = false)
  private BigDecimal price;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;
  
  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
  private List<OrderItem> orderItems = new ArrayList<>();
}
```

**Repository:**
```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
  
  Optional<Product> findByTenantIdAndName(Long tenantId, String name);
  
  List<Product> findByTenantIdAndCategoryId(Long tenantId, Long categoryId);
  
  @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.price <= :maxPrice")
  List<Product> findAffordableProducts(@Param("tenantId") Long tenantId, 
                                       @Param("maxPrice") BigDecimal maxPrice);
}
```

**Service:**
```java
@Service
@Transactional
public class ProductService {
  
  private final ProductRepository repository;
  
  public Product create(Product product) {
    // Validation + save
  }
  
  public Product update(Long id, Product updates) {
    // Optimistic locking + update
  }
  
  public void delete(Long id) {
    // Soft delete or hard delete
  }
}
```

---

## 📊 Metrics & Performance

**Generator Performance:**
- Single entity generation: ~50ms
- Full metamodel reload: ~200ms (3 entities)
- Schema diff detection: ~100ms
- DDL execution: ~50ms per statement

**Code Quality:**
- ✅ 164 source files compiled successfully
- ✅ Zero compilation errors
- ✅ Standard Spring Boot patterns
- ✅ Lombok integration
- ✅ JPA best practices

**Schema Evolution:**
- Safe changes: Auto-applied (ADD COLUMN, CREATE INDEX)
- Risky changes: Manual review required
- Zero-downtime: Hot reload without restart

---

### 📋 META-006: Contracts & Documentation Generation
**Priority:** P1  
**Estimate:** 2 weeks  
**Status:** PLANNED

#### Overview

**Automatická generace API kontraktů a dokumentace** z metamodelu jako Single Source of Truth.

**Co se generuje:**
1. 📜 **Metamodel API** (OpenAPI) - Read-only přístup k metamodelu
2. 📜 **Runtime API** (OpenAPI) - CRUD nad business entitami
3. 📡 **Event Schema** (AsyncAPI) - Streaming events (CDC, workflow)
4. 📚 **Dokumentace** (Markdown/HTML) - Entity katalog, use-cases, changelog

---

#### 1. Metamodel API (Read-only)

**Purpose:** n8n, MCP tools, integrační služby potřebují číst metamodel

**OpenAPI Spec:** `/api/metamodel/openapi.json`

**Endpoints:**

```yaml
# GET /api/metamodel/entities
summary: List all entities
parameters:
  - name: tenant
    description: Filter by tenant (optional, only for tenant-specific entities)
  - name: domain
    description: Filter by domain (CRM, Projects, DMS, ...)
  - name: module
    description: Filter by module (core, delivery-suite, helpdesk, ...)
responses:
  200:
    schema:
      type: array
      items:
        $ref: '#/components/schemas/EntityMetadata'

# GET /api/metamodel/entities/{name}
summary: Get entity detail
parameters:
  - name: name
    description: Entity name (e.g., "Project", "User")
responses:
  200:
    schema:
      $ref: '#/components/schemas/EntityDetail'

# GET /api/metamodel/relations
summary: Get all relations (for ER diagram)
responses:
  200:
    schema:
      type: array
      items:
        $ref: '#/components/schemas/Relation'

# GET /api/metamodel/security-policies
summary: Get RBAC and row-level security policies
responses:
  200:
    schema:
      type: array
      items:
        $ref: '#/components/schemas/SecurityPolicy'

# GET /api/metamodel/use-cases
summary: Get use-case definitions
responses:
  200:
    schema:
      type: array
      items:
        $ref: '#/components/schemas/UseCase'

# GET /api/metamodel/spec-version
summary: Get metamodel version (for cache invalidation)
responses:
  200:
    schema:
      type: object
      properties:
        version: string  # e.g., "2.3.1"
        lastModified: string  # ISO 8601 timestamp
```

**Použití:**

**n8n Connector:**
```typescript
// n8n node: "Core Platform - Get Entities"
const response = await this.helpers.request({
  method: 'GET',
  url: 'https://api.core-platform.local/api/metamodel/entities',
  qs: { domain: 'Projects' },
  headers: { 'Authorization': 'Bearer ...' }
});

// Response:
[
  {
    name: "Project",
    table: "projects",
    fields: [
      { name: "id", type: "uuid", primary: true },
      { name: "name", type: "string", length: 255 },
      ...
    ],
    relations: [
      { name: "requirements", type: "one_to_many", target: "Requirement" }
    ]
  }
]

// n8n nyní ví, jak volat: POST /api/projects, GET /api/projects/{id}, ...
```

**MCP Tool:**
```typescript
// AI tool pro čtení metamodelu
{
  name: "get_core_platform_entities",
  description: "Get entity schema from Core Platform metamodel",
  input_schema: {
    domain: "optional string",
  },
  handler: async (input) => {
    return await fetch(`https://api.core-platform.local/api/metamodel/entities?domain=${input.domain}`);
  }
}
```

---

#### 2. Runtime API (OpenAPI)

**Purpose:** Business API nad entitami definovanými v metamodelu

**Generováno z:** Metamodel YAML → OpenAPI spec

**Příklad:**

**Metamodel YAML:**
```yaml
entity: Project
table: projects
api:
  enabled: true
  path: /projects
  operations: [CREATE, READ, UPDATE, DELETE, LIST]

fields:
  - name: id
    type: uuid
    primary: true
  - name: name
    type: string
    length: 255
    nullable: false
    validations:
      - type: min_length
        value: 3
  - name: status
    type: enum
    values: [NEW, ACTIVE, COMPLETED, ARCHIVED]
  - name: owner_id
    type: uuid
    foreign_key:
      entity: User
```

**Generovaný OpenAPI:**
```yaml
openapi: 3.0.0
info:
  title: Core Platform API
  version: 2.3.1

paths:
  /api/projects:
    post:
      summary: Create project
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProjectCreate'
      responses:
        201:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Project'
    
    get:
      summary: List projects
      parameters:
        - name: status
          schema:
            type: string
            enum: [NEW, ACTIVE, COMPLETED, ARCHIVED]
        - name: owner_id
          schema:
            type: string
            format: uuid
      responses:
        200:
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Project'
  
  /api/projects/{id}:
    get:
      summary: Get project by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        200:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Project'
    
    put:
      summary: Update project
      # ...
    
    delete:
      summary: Delete project
      # ...

components:
  schemas:
    Project:
      type: object
      required: [name, status]
      properties:
        id:
          type: string
          format: uuid
          readOnly: true
        name:
          type: string
          minLength: 3
          maxLength: 255
        status:
          type: string
          enum: [NEW, ACTIVE, COMPLETED, ARCHIVED]
        owner_id:
          type: string
          format: uuid
        created_at:
          type: string
          format: date-time
          readOnly: true
        updated_at:
          type: string
          format: date-time
          readOnly: true
```

**Použití:**
- **Frontend TypeScript types** generované z OpenAPI (via `openapi-generator`)
- **API client libraries** (Java, Python, Node.js)
- **API testing** (Postman collection import)
- **API documentation** (Swagger UI)

---

#### 3. Event Schema (AsyncAPI)

**Purpose:** Streaming events (CDC, workflow state changes)

**Generováno z:** Metamodel YAML → AsyncAPI spec

**Metamodel YAML:**
```yaml
entity: Project
streaming:
  enabled: true
  events:
    - name: project.created
      payload: full_entity
    - name: project.updated
      payload: changes_only
    - name: project.deleted
      payload: entity_id
```

**Generovaný AsyncAPI:**
```yaml
asyncapi: 2.6.0
info:
  title: Core Platform Events
  version: 2.3.1

channels:
  project.created:
    description: Fired when a new project is created
    subscribe:
      message:
        name: ProjectCreated
        contentType: application/json
        payload:
          $ref: '#/components/schemas/Project'
  
  project.updated:
    description: Fired when a project is updated
    subscribe:
      message:
        name: ProjectUpdated
        contentType: application/json
        payload:
          type: object
          properties:
            id:
              type: string
              format: uuid
            changes:
              type: object
              description: Changed fields only
            previous_values:
              type: object
              description: Previous values
  
  project.deleted:
    description: Fired when a project is deleted
    subscribe:
      message:
        name: ProjectDeleted
        contentType: application/json
        payload:
          type: object
          properties:
            id:
              type: string
              format: uuid
            deleted_at:
              type: string
              format: date-time

components:
  schemas:
    Project:
      $ref: './openapi.yaml#/components/schemas/Project'
```

**Použití:**
- **Event-driven architecture** (Kafka, RabbitMQ consumers)
- **n8n workflows** (subscribe to events, trigger actions)
- **Real-time dashboards** (WebSocket subscriptions)
- **Audit log** (všechny events persistovány)

---

#### 4. Dokumentace (Markdown/HTML)

**Generováno z:** Metamodel YAML + AI descriptions → Markdown

**Struktura:**

```
docs/entities/
├── index.md                   # Katalog všech entit
├── core/
│   ├── User.md
│   ├── Role.md
│   └── Group.md
├── projects/
│   ├── Project.md
│   ├── Requirement.md
│   └── TimeEntry.md
└── modules/
    ├── delivery-suite/
    │   └── DeliveryItem.md
    └── helpdesk/
        └── Ticket.md
```

**Příklad: Project.md**

```markdown
# Entity: Project

**Table:** `projects`  
**Domain:** Projects  
**Tenant-aware:** Yes  
**Streaming:** Enabled

## Overview

Project entity represents a work container for organizing requirements, tasks, and team collaboration.

## Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | UUID | PK | Unique identifier |
| `name` | String(255) | NOT NULL, min 3 chars | Project name |
| `description` | TEXT | - | Detailed description |
| `status` | ENUM | NOT NULL | NEW, ACTIVE, COMPLETED, ARCHIVED |
| `owner_id` | UUID | FK → User | Project owner |
| `start_date` | DATE | - | Planned start |
| `end_date` | DATE | - | Planned end |
| `tenant_id` | UUID | NOT NULL | Tenant isolation |

## Relations

**1:N Relations:**
- `requirements: Requirement[]` - Project has many Requirements
- `timeEntries: TimeEntry[]` - Time logged on project

**N:1 Relations:**
- `owner: User` - Project belongs to one User

**M:N Relations:**
- `members: User[]` - Project team members

**Special Relations:**
- `documents: Document[]` (DMS) - Attached documents
- `workflowInstance: WorkflowInstance` (Workflow) - Approval process

## API Endpoints

**CRUD:**
- `POST /api/projects` - Create project
- `GET /api/projects` - List projects
- `GET /api/projects/{id}` - Get project by ID
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project (soft delete)

**Relations:**
- `GET /api/projects/{id}/requirements` - Get requirements
- `GET /api/projects/{id}/time-entries` - Get time entries
- `GET /api/projects/{id}/documents` - Get documents

## Events (Streaming)

**Published Events:**
- `project.created` - New project created
- `project.updated` - Project fields changed
- `project.status_changed` - Status transition
- `project.deleted` - Project deleted

**Event Payload Example:**
```json
{
  "event": "project.created",
  "timestamp": "2025-11-10T14:30:00Z",
  "tenant_id": "acme-corp",
  "payload": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "New Website",
    "status": "NEW",
    "owner_id": "...",
    "created_at": "2025-11-10T14:30:00Z"
  }
}
```

## Security

**Permissions:**
- `CREATE`: `ROLE_PROJECT_MANAGER` or `ROLE_ADMIN`
- `READ`: `ROLE_USER` (tenant-scoped)
- `UPDATE`: Project owner OR `ROLE_ADMIN`
- `DELETE`: `ROLE_ADMIN` only

**Row-level Security:**
- Users see only projects in their tenant
- Users can only update projects they own (unless ADMIN)

## Validation Rules

**name:**
- Required
- Min length: 3
- Max length: 255
- Pattern: `^[a-zA-Z0-9\s\-_]+$`

**status:**
- Required
- Enum: NEW, ACTIVE, COMPLETED, ARCHIVED

**end_date:**
- Optional
- Must be >= start_date (if both provided)

## Use-cases

**Primary Use-cases:**
- [Create Project with Approval](../use-cases/create-project.md)
- [Assign Team Members](../use-cases/assign-team.md)
- [Track Project Progress](../use-cases/track-progress.md)

## Changelog

**v2.3.0** (2025-11-10)
- Added: `time_entries` relation (time-tracking)
- Added: Computed field `totalHours`

**v2.2.0** (2025-10-15)
- Added: `end_date` field
- Changed: `status` enum (added ARCHIVED)

**v2.1.0** (2025-09-20)
- Added: DMS integration (documents relation)

**v2.0.0** (2025-08-15)
- Initial metamodel definition

## Migration History

**V20251110143000:** Add time_entries relation  
**V20251015120000:** Add end_date field  
**V20250920100000:** Add DMS integration
```

**Generování:**
```java
@Service
public class DocumentationGenerator {
  
  public void generateEntityDocs(EntitySchema entity) {
    String markdown = generateMarkdown(entity);
    Files.writeString(
      Path.of("docs/entities/" + entity.getDomain() + "/" + entity.getName() + ".md"),
      markdown
    );
  }
  
  private String generateMarkdown(EntitySchema entity) {
    return """
      # Entity: %s
      
      **Table:** `%s`
      **Domain:** %s
      
      ## Fields
      %s
      
      ## Relations
      %s
      
      ## API Endpoints
      %s
      
      ...
      """.formatted(
        entity.getName(),
        entity.getTable(),
        entity.getDomain(),
        generateFieldsTable(entity),
        generateRelationsSection(entity),
        generateApiSection(entity)
      );
  }
}
```

---

#### Definition of Done (DoD)

**Po každé změně metamodelu:**

1. ✅ **Dokumentace se aktualizuje** (Markdown regenerován)
2. ✅ **OpenAPI spec obnoven** (`/api/openapi.json` aktualizován)
3. ✅ **AsyncAPI spec obnoven** (`/api/asyncapi.json` aktualizován)
4. ✅ **CI ověří konzistenci:**
   - Lint metamodel YAML
   - Validace generovaných OpenAPI/AsyncAPI
   - Generované TypeScript klienty kompilují
   - Unit testy prošly (entity validation)

**CI Pipeline:**
```yaml
# .github/workflows/metamodel-ci.yml
name: Metamodel CI

on:
  push:
    paths:
      - 'backend/src/main/resources/metamodel/**'

jobs:
  validate-and-generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Lint Metamodel YAML
        run: |
          yamllint backend/src/main/resources/metamodel/*.yaml
      
      - name: Generate OpenAPI
        run: |
          ./scripts/generate-openapi.sh
      
      - name: Validate OpenAPI
        run: |
          openapi-generator validate -i docs/api/openapi.json
      
      - name: Generate AsyncAPI
        run: |
          ./scripts/generate-asyncapi.sh
      
      - name: Generate TypeScript Client
        run: |
          openapi-generator generate \
            -i docs/api/openapi.json \
            -g typescript-fetch \
            -o frontend/src/api/generated
      
      - name: Compile TypeScript
        run: |
          cd frontend && npm run typecheck
      
      - name: Generate Documentation
        run: |
          ./scripts/generate-docs.sh
      
      - name: Commit Generated Files
        run: |
          git add docs/api/*.json frontend/src/api/generated docs/entities
          git commit -m "chore: regenerate API contracts and docs from metamodel"
          git push
```

**Value:**
- 📜 **Contracts always up-to-date** (OpenAPI/AsyncAPI z metamodelu)
- 📚 **Docs always current** (Markdown + changelog + examples)
- 🔌 **Integrations ready** (n8n, MCP, ETL znají schema)
- ✅ **CI validates consistency** (lint + compile + test)

---

### 📋 META-007: Governance & Approval Flow
**Priority:** P1  
**Estimate:** 1 week  
**Status:** PLANNED

#### Overview

**Change Management** pro metamodel s governance a auditováním.

**Principles:**
- ✅ **Human-in-the-loop** - AI navrhuje, člověk schvaluje
- ✅ **Audit trail** - Každá změna logována (kdo, kdy, proč)
- ✅ **Versioning** - Metamodel má `specVersion`, migration notes
- ✅ **Impact analysis** - Před schválením viditelné dopady
- ✅ **Role-based approval** - Pouze oprávněné role mohou měnit

---

#### Change Workflow

**1. Návrh změny (Proposal)**

**Zdroj:**
- 🧠 AI Copilot (META-005) vytvoří návrh
- 👤 Člověk vytvoří návrh ručně (YAML edit)

**Formát:**
```yaml
# proposals/timetracking_001.yaml
proposal_id: timetracking_001
created_at: 2025-11-10T14:30:00Z
author: ai-copilot  # nebo user email
status: PENDING_APPROVAL

changes:
  - type: ADD_ENTITY
    entity:
      name: TimeEntry
      table: time_entries
      # ... full entity definition
  
  - type: MODIFY_ENTITY
    entity: Requirement
    changes:
      - add_relation:
          name: timeEntries
          type: one_to_many
          target: TimeEntry

rationale: |
  User requested time-tracking for Requirements.
  
  Design decisions:
  - Separate TimeEntry entity (normalized)
  - Unique constraint prevents duplicate entries
  
impacted_components:
  backend:
    - entities: [TimeEntry]
    - api: [/api/time-entries]
  frontend:
    - views: [TimeEntryForm, TimeEntrySummary]
  database:
    - migrations: [V20251110143000__add_time_tracking.sql]
  tests:
    - files: [TimeEntryTest, TimeEntryIntegrationTest]

breaking_changes: false

required_approval:
  role: PLATFORM_METAMODEL_ADMIN
  reason: "Adding new entity to platform metamodel"
```

**Storage:**
```
backend/src/main/resources/metamodel/proposals/
├── timetracking_001.yaml        # Pending
├── custom_fields_002.yaml       # Approved
└── archive/
    └── old_proposal_003.yaml    # Rejected
```

---

**2. Validace návrhu (Validation)**

**Automated Checks:**
```java
@Service
public class ProposalValidator {
  
  public ValidationReport validate(Proposal proposal) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();
    
    // Syntax check
    if (!isValidYAML(proposal)) {
      errors.add("Invalid YAML syntax");
    }
    
    // Consistency check
    for (Change change : proposal.getChanges()) {
      if (change.getType() == MODIFY_ENTITY) {
        if (!entityExists(change.getEntityName())) {
          errors.add("Entity '" + change.getEntityName() + "' does not exist");
        }
      }
      
      if (change.getType() == ADD_ENTITY) {
        if (entityExists(change.getEntity().getName())) {
          errors.add("Entity '" + change.getEntity().getName() + "' already exists");
        }
      }
    }
    
    // Breaking change detection
    if (proposal.hasTypeChanges()) {
      warnings.add("Type changes may break existing API clients");
    }
    
    if (proposal.removesFields()) {
      errors.add("Removing fields is a BREAKING CHANGE - not allowed");
    }
    
    // Security check
    if (proposal.modifiesPlatformEntities() && !hasRole("PLATFORM_METAMODEL_ADMIN")) {
      errors.add("Insufficient permissions to modify platform entities");
    }
    
    return new ValidationReport(errors, warnings);
  }
}
```

**Validation UI:**
```
┌────────────────────────────────────────────────────────┐
│  Proposal: timetracking_001                           │
├────────────────────────────────────────────────────────┤
│                                                        │
│  ✅ Validation: PASSED                                │
│                                                        │
│  Checks:                                               │
│  ✅ YAML syntax valid                                 │
│  ✅ No missing references                             │
│  ✅ No circular dependencies                          │
│  ✅ No type mismatches                                │
│  ⚠️  Warning: Adding 1 new table (migration required) │
│                                                        │
│  Breaking Changes: NO                                  │
│                                                        │
│  Impacted Components:                                  │
│  • Backend: +1 entity, +5 API endpoints               │
│  • Frontend: +2 views                                 │
│  • Database: +1 migration script                      │
│  • Tests: +3 test files                               │
│                                                        │
│  Estimated Effort: 9h                                  │
│  Estimated Downtime: 0 seconds (additive change)      │
│                                                        │
│  [❌ Reject] [✅ Approve]                              │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

**3. Review & Approval**

**Approval Matrix:**

| Change Type | Required Role | Auto-apply? |
|-------------|---------------|-------------|
| ADD_ENTITY (platform) | `PLATFORM_METAMODEL_ADMIN` | No |
| ADD_ENTITY (tenant) | `TENANT_METAMODEL_ADMIN` | No |
| MODIFY_ENTITY (add field) | `*_METAMODEL_ADMIN` | No |
| MODIFY_ENTITY (remove field) | **REJECTED** | Never |
| ADD_RELATION | `*_METAMODEL_ADMIN` | No |
| ADD_INDEX | `*_METAMODEL_ADMIN` | Yes (if safe) |
| ADD_VALIDATION | `*_METAMODEL_ADMIN` | No |

**Approval Process:**
```
Proposal created (status: PENDING_APPROVAL)
  ↓
Validation runs automatically
  ↓
Admin reviews proposal in Studio
  ↓
Admin clicks [Approve] or [Reject]
  ↓
If Approved:
  → Status: APPROVED
  → Trigger: Apply changes to metamodel
  → Trigger: Regenerate code, contracts, docs
  → Trigger: Run CI/CD pipeline
  → Status: APPLIED
  ↓
If Rejected:
  → Status: REJECTED
  → Move to: proposals/archive/
  → Reason logged in audit
```

---

**4. Aplikace změn (Apply)**

**After Approval:**
```java
@Service
public class ChangeApplicator {
  
  @Transactional
  public void apply(Proposal proposal) {
    // 1. Update metamodel registry (YAML files)
    for (Change change : proposal.getChanges()) {
      if (change.getType() == ADD_ENTITY) {
        saveEntityYAML(change.getEntity());
      }
      if (change.getType() == MODIFY_ENTITY) {
        updateEntityYAML(change.getEntityName(), change.getChanges());
      }
    }
    
    // 2. Increment specVersion
    incrementSpecVersion();  // 2.3.0 → 2.4.0
    
    // 3. Generate migration script (Flyway)
    String migration = generateFlywayMigration(proposal);
    saveMigrationScript(migration);
    
    // 4. Regenerate code
    codeGenerator.regenerate();
    
    // 5. Regenerate contracts (OpenAPI, AsyncAPI)
    contractGenerator.regenerate();
    
    // 6. Regenerate docs (Markdown)
    docGenerator.regenerate();
    
    // 7. Audit log
    auditLog.log("METAMODEL_CHANGE_APPLIED", proposal.getId(), proposal.getAuthor());
    
    // 8. Update proposal status
    proposal.setStatus(ProposalStatus.APPLIED);
    proposal.setAppliedAt(Instant.now());
    proposalRepo.save(proposal);
    
    // 9. Notify (optional)
    notificationService.send("Metamodel updated: " + proposal.getId());
  }
}
```

---

**5. Audit Log**

**Table:** `metamodel_audit_log`

```sql
CREATE TABLE metamodel_audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  event_type VARCHAR(64),  -- PROPOSAL_CREATED, PROPOSAL_APPROVED, PROPOSAL_REJECTED, CHANGE_APPLIED
  proposal_id VARCHAR(64),
  entity_name VARCHAR(64),
  change_type VARCHAR(32),  -- ADD_ENTITY, MODIFY_ENTITY, ADD_RELATION, ...
  author VARCHAR(255),      -- User email or "ai-copilot"
  approver VARCHAR(255),    -- Who approved (if applicable)
  spec_version VARCHAR(32), -- Metamodel version after change
  diff JSONB,               -- Full diff (before/after)
  rationale TEXT            -- Why was this change made
);
```

**Events:**
```json
[
  {
    "event_type": "PROPOSAL_CREATED",
    "timestamp": "2025-11-10T14:30:00Z",
    "proposal_id": "timetracking_001",
    "author": "ai-copilot",
    "rationale": "User requested time-tracking for Requirements"
  },
  {
    "event_type": "PROPOSAL_APPROVED",
    "timestamp": "2025-11-10T15:00:00Z",
    "proposal_id": "timetracking_001",
    "approver": "admin@acme-corp.com",
    "spec_version": "2.4.0"
  },
  {
    "event_type": "CHANGE_APPLIED",
    "timestamp": "2025-11-10T15:01:00Z",
    "proposal_id": "timetracking_001",
    "entity_name": "TimeEntry",
    "change_type": "ADD_ENTITY",
    "diff": {
      "before": null,
      "after": { "name": "TimeEntry", "table": "time_entries", ... }
    }
  }
]
```

**Audit UI:**
```
┌────────────────────────────────────────────────────────┐
│  Metamodel Audit Log                                  │
├────────────────────────────────────────────────────────┤
│                                                        │
│  [2025-11-10 15:01] ✅ CHANGE_APPLIED                 │
│  Proposal: timetracking_001                           │
│  Entity: TimeEntry (ADD_ENTITY)                       │
│  Author: ai-copilot                                   │
│  Approver: admin@acme-corp.com                        │
│  Version: 2.3.0 → 2.4.0                               │
│  [View Diff] [View Proposal]                          │
│                                                        │
│  [2025-11-10 15:00] ✅ PROPOSAL_APPROVED              │
│  Proposal: timetracking_001                           │
│  Approver: admin@acme-corp.com                        │
│                                                        │
│  [2025-11-10 14:30] 📝 PROPOSAL_CREATED               │
│  Proposal: timetracking_001                           │
│  Author: ai-copilot                                   │
│  Rationale: "User requested time-tracking..."         │
│  [View Full Proposal]                                 │
│                                                        │
└────────────────────────────────────────────────────────┘
```

---

**6. Versioning**

**specVersion Management:**
```yaml
# backend/src/main/resources/metamodel/metadata.yaml
spec_version: "2.4.0"
last_modified: "2025-11-10T15:01:00Z"
last_modified_by: "admin@acme-corp.com"

changelog:
  - version: "2.4.0"
    date: "2025-11-10"
    changes:
      - "Added TimeEntry entity for time-tracking"
      - "Added Requirement.timeEntries relation"
    migration: "V20251110143000__add_time_tracking.sql"
    breaking: false
  
  - version: "2.3.0"
    date: "2025-10-15"
    changes:
      - "Added Project.end_date field"
      - "Added ARCHIVED status to Project.status enum"
    migration: "V20251015120000__add_project_end_date.sql"
    breaking: false
```

**Version Endpoint:**
```bash
GET /api/metamodel/spec-version

{
  "version": "2.4.0",
  "last_modified": "2025-11-10T15:01:00Z",
  "changelog_url": "/api/metamodel/changelog"
}
```

**Cache Invalidation:**
- n8n connector checks `/spec-version` před voláním API
- Pokud se verze změnila → invalidate cache, reload schema
- Zabraňuje použití zastaralého schema

---

#### Security & Access Control

**Platform Metamodel Studio:**
- 🔒 **Pouze admin realm** (`admin.core-platform.local`)
- 🔑 **Role:** `PLATFORM_METAMODEL_ADMIN`
- ✅ **Může měnit:** Platform entities (`User`, `Role`, `Workflow`, ...)
- ❌ **Nemůže měnit:** Tenant-specific entities (pouze číst)

**Tenant Metamodel Studio:**
- 🔒 **Běží v tenant realmu** (`acme-corp.core-platform.local`)
- 🔑 **Role:** `TENANT_METAMODEL_ADMIN`
- ✅ **Může měnit:** Tenant-specific entities (`CustomField`, vlastní entity)
- ❌ **Nemůže měnit:** Platform entities (read-only)
- ✅ **Vidí:** Platform entities + své tenant entities

**Validace:**
```java
@PreAuthorize("hasRole('PLATFORM_METAMODEL_ADMIN')")
public void modifyPlatformEntity(String entityName) {
  if (!isPlatformEntity(entityName)) {
    throw new ForbiddenException("Cannot modify tenant entity with PLATFORM_METAMODEL_ADMIN role");
  }
  // ...
}

@PreAuthorize("hasRole('TENANT_METAMODEL_ADMIN')")
public void modifyTenantEntity(String entityName, String tenantId) {
  if (isPlatformEntity(entityName)) {
    throw new ForbiddenException("Cannot modify platform entity with TENANT_METAMODEL_ADMIN role");
  }
  
  if (!belongsToTenant(entityName, tenantId)) {
    throw new ForbiddenException("Entity does not belong to your tenant");
  }
  // ...
}
```

**Audit:**
- ✅ Všechny změny logované do Loki
- ✅ Audit log obsahuje: kdo, kdy, co, proč (rationale)
- ✅ Diff (before/after) uložen v JSONB

**Validace (pre-commit):**
- ✅ Žádné "rozbití" core entit (nelze smazat `User`, `Role`, ...)
- ✅ Tenant entity nemůže reference platform entity způsobem, který by narušil izolaci
- ✅ Breaking changes jsou **REJECTED** (API kompatibilita)

---

#### Value

- 🔒 **Governance** - Human-in-the-loop, AI navrhuje ale neschvaluje
- 📝 **Audit trail** - Každá změna logována (compliance)
- 📈 **Versioning** - specVersion tracking, changelog, migration notes
- ⚠️ **Impact analysis** - Před schválením viditelné dopady
- 🔐 **Security** - Role-based access, tenant isolation preserved

---

### 📋 META-008: Advanced Constraints
**Priority:** P2  
**Estimate:** 2 weeks  
**Status:** PLANNED

**Scope:**
- CHECK constraints from YAML
- FOREIGN KEY cascade rules (CASCADE, SET NULL, RESTRICT)
- Custom validation rules (regex, custom functions)
- Multi-column UNIQUE constraints
- Conditional constraints (business logic)

**Example YAML:**
```yaml
entity: User
fields:
  - name: age
    type: integer
    constraints:
      - type: CHECK
        condition: "age >= 0 AND age <= 150"
  
  - name: status
    type: string
    constraints:
      - type: CHECK
        condition: "status IN ('active', 'inactive', 'suspended')"
  
  - name: email
    type: email
    validations:
      - type: regex
        pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"

indexes:
  - columns: [tenant_id, email]
    unique: true
    name: uk_tenant_email

foreign_keys:
  - field: manager_id
    references: User(id)
    on_delete: SET NULL
    on_update: CASCADE
```

---

## 🔧 Usage Guide

### Basic Workflow

**1. Define Entity in YAML:**
```yaml
# backend/src/main/resources/metamodel/product.yaml
entity: Product
table: products
tenant_aware: true

fields:
  - name: name
    type: string
    length: 255
    nullable: false
```

**2. Reload Metamodel:**
```bash
curl http://localhost:8080/api/admin/metamodel/reload
```

**3. Review Changes:**
```json
{
  "changes": {
    "Product": {
      "totalChanges": 1,
      "details": [
        {"type": "ADD", "column": "name", "risky": false}
      ]
    }
  }
}
```

**4. Apply Safe Changes:**
```bash
curl -X POST http://localhost:8080/api/admin/metamodel/apply-safe-changes
```

**5. Verify Schema:**
```sql
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'products';
```

---

## 🧪 Testing

**EPIC-level požadavky:**
- **Unit:** validace metamodel schémat, RBAC pravidel (access matrix), generování kontraktů a streaming payloadů.
- **Integration:** změna metamodelu → nasazení → CRUD nad generovanou entitou funguje + emituje validní eventy.
- **E2E:** admin vytvoří entitu ve Studiu, FE ji zobrazí v generické UI, data se uloží do DB i do streamu, multi-tenant izolace zůstává nedotčena.

**Strategie testování pokrývá všechny fáze (Generator → Visual Studio → AI Copilot → Contracts → Governance).**

### Unit Tests

**1. Metamodel Validation Tests** (`backend/src/test/java/metamodel/validation`)
```java
@Test
void testEntityYamlValidation() {
    MetamodelValidator validator = new MetamodelValidator();
    
    // Valid YAML
    String validYaml = """
        name: Project
        table: projects
        fields:
          - name: id
            type: UUID
            constraints: [PRIMARY_KEY, NOT_NULL]
    """;
    assertDoesNotThrow(() -> validator.validate(validYaml));
    
    // Invalid: missing required field
    String invalidYaml = """
        name: Project
        # Missing 'table' field
        fields: []
    """;
    assertThrows(MetamodelValidationException.class, 
                 () -> validator.validate(invalidYaml));
}

@Test
void testRelationConsistency() {
    // Entity A references Entity B that doesn't exist
    String entityA = "name: Order\nrelations:\n  - name: customer\n    target: Customer\n    type: MANY_TO_ONE";
    String[] allEntities = {entityA}; // Customer missing
    
    ConsistencyChecker checker = new ConsistencyChecker();
    List<ValidationError> errors = checker.checkRelations(allEntities);
    
    assertEquals(1, errors.size());
    assertEquals("Target entity 'Customer' not found", errors.get(0).getMessage());
}
```

**2. Code Generation Tests** (`backend/src/test/java/metamodel/codegen`)
```java
@Test
void testJavaEntityGeneration() {
    String yaml = """
        name: Product
        table: products
        fields:
          - name: name
            type: String
            constraints: [NOT_NULL]
          - name: price
            type: BigDecimal
    """;
    
    JavaEntityGenerator generator = new JavaEntityGenerator();
    String javaCode = generator.generate(yaml);
    
    // Ověření syntaxe
    assertDoesNotThrow(() -> JavaParser.parse(javaCode));
    
    // Ověření obsahu
    assertTrue(javaCode.contains("@Entity"));
    assertTrue(javaCode.contains("@Table(name = \"products\")"));
    assertTrue(javaCode.contains("@Column(nullable = false)"));
}

@Test
void testTypeScriptInterfaceGeneration() {
    String yaml = "name: User\nfields:\n  - name: email\n    type: String\n  - name: age\n    type: Integer";
    
    TypeScriptGenerator generator = new TypeScriptGenerator();
    String tsCode = generator.generate(yaml);
    
    assertTrue(tsCode.contains("export interface User {"));
    assertTrue(tsCode.contains("email: string;"));
    assertTrue(tsCode.contains("age: number;"));
}
```

**3. API Contract Generation Tests** (`backend/src/test/java/metamodel/contracts`)
```java
@Test
void testOpenAPIGeneration() {
    String entityYaml = """
        name: Project
        fields:
          - name: id
            type: UUID
          - name: title
            type: String
            constraints: [NOT_NULL]
    """;
    
    OpenAPIGenerator generator = new OpenAPIGenerator();
    OpenAPI spec = generator.generate(entityYaml);
    
    // Ověření CRUD endpoints
    assertNotNull(spec.getPaths().get("/api/projects"));
    assertNotNull(spec.getPaths().get("/api/projects/{id}"));
    
    // Ověření schema
    Schema projectSchema = spec.getComponents().getSchemas().get("Project");
    assertNotNull(projectSchema);
    assertEquals("string", projectSchema.getProperties().get("id").getType());
    assertEquals("string", projectSchema.getProperties().get("title").getType());
}

@Test
void testAsyncAPIGeneration() {
    String yaml = "name: Order\nstreaming:\n  enabled: true\n  events: [created, updated, cancelled]";
    
    AsyncAPIGenerator generator = new AsyncAPIGenerator();
    AsyncAPI spec = generator.generate(yaml);
    
    // Ověření event channels
    assertNotNull(spec.getChannels().get("events.order.created"));
    assertNotNull(spec.getChannels().get("events.order.updated"));
    assertNotNull(spec.getChannels().get("events.order.cancelled"));
}
```

**4. AI Proposal Validation Tests** (`backend/src/test/java/metamodel/ai`)
```java
@Test
void testProposalSyntaxValidation() {
    String proposalYaml = """
        proposal_id: add_time_tracking
        changes:
          - type: ADD_ENTITY
            entity:
              name: TimeEntry
              table: time_entries
              fields:
                - name: hours
                  type: BigDecimal
        rationale: "User requested time tracking for Requirements"
    """;
    
    ProposalValidator validator = new ProposalValidator();
    assertDoesNotThrow(() -> validator.validateSyntax(proposalYaml));
}

@Test
void testProposalImpactAnalysis() {
    String proposal = "type: MODIFY_ENTITY\nentity: Project\nchanges: [remove_field: status]";
    
    ImpactAnalyzer analyzer = new ImpactAnalyzer(currentMetamodel);
    ImpactReport report = analyzer.analyze(proposal);
    
    assertTrue(report.hasBreakingChanges());
    assertTrue(report.getAffectedComponents().contains("frontend/src/views/ProjectList.tsx"));
    assertTrue(report.getAffectedComponents().contains("backend/src/api/ProjectController.java"));
}
```

### Integration Tests

**1. Metamodel Change → Contract Regeneration** (`backend/src/test/java/integration`)
```java
@SpringBootTest
@Testcontainers
class MetamodelWorkflowIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Autowired MetamodelService metamodelService;
    @Autowired ContractGenerator contractGenerator;
    
    @Test
    void testMetamodelChangeRegeneratesContracts() {
        // 1. Změň metamodel (přidej entitu)
        String newEntityYaml = """
            name: Comment
            table: comments
            fields:
              - name: text
                type: Text
        """;
        metamodelService.addEntity(newEntityYaml);
        
        // 2. Regeneruj OpenAPI spec
        OpenAPI openapi = contractGenerator.generateOpenAPI();
        
        // 3. Ověř nový endpoint
        assertNotNull(openapi.getPaths().get("/api/comments"));
        
        // 4. Ověř AsyncAPI
        AsyncAPI asyncapi = contractGenerator.generateAsyncAPI();
        assertNotNull(asyncapi.getChannels().get("events.comment.created"));
    }
    
    @Test
    void testProposalWorkflow() {
        // 1. Vytvoř proposal
        Proposal proposal = new Proposal();
        proposal.setProposalId("test_proposal_001");
        proposal.setChanges(List.of(new AddEntityChange("Tag", "tags")));
        proposal.setRationale("Testing governance workflow");
        
        ProposalService proposalService = new ProposalService();
        UUID proposalId = proposalService.createProposal(proposal);
        
        // 2. Validuj
        ValidationResult validation = proposalService.validate(proposalId);
        assertTrue(validation.isValid());
        
        // 3. Schvali (simulace admin user)
        proposalService.approve(proposalId, "admin@core-platform.local");
        
        // 4. Aplikuj změny
        proposalService.apply(proposalId);
        
        // 5. Ověř audit log
        List<AuditLogEntry> logs = auditLogRepository.findByProposalId("test_proposal_001");
        assertEquals(3, logs.size()); // CREATED, APPROVED, APPLIED
        
        // 6. Ověř specVersion increment
        String newVersion = metamodelService.getSpecVersion();
        assertEquals("2.4.0", newVersion); // Předpokládáme 2.3.0 → 2.4.0
    }
}
```

**2. Frontend Build Test** (`e2e/integration`)
```typescript
import { test, expect } from '@playwright/test';

test.describe('Metamodel Change → Frontend Rebuild', () => {
  test('Adding entity updates TypeScript types', async () => {
    // 1. Přidej entitu do metamodelu (REST API call)
    const response = await fetch('http://localhost:8080/api/metamodel/entities', {
      method: 'POST',
      headers: { 'Content-Type': 'application/yaml' },
      body: `
        name: Task
        table: tasks
        fields:
          - name: title
            type: String
      `
    });
    expect(response.ok).toBeTruthy();
    
    // 2. Spusť contract generation
    await fetch('http://localhost:8080/api/metamodel/regenerate-contracts', {
      method: 'POST'
    });
    
    // 3. Ověř vygenerované TypeScript typy
    const typesFile = await fetch('http://localhost:5173/src/types/entities/Task.ts');
    const content = await typesFile.text();
    expect(content).toContain('export interface Task {');
    expect(content).toContain('title: string;');
    
    // 4. Build frontend (simulace CI)
    const { exec } = require('child_process');
    const buildResult = await new Promise((resolve) => {
      exec('cd frontend && npm run build', (error, stdout) => {
        resolve({ success: !error, output: stdout });
      });
    });
    expect(buildResult.success).toBeTruthy();
  });
});
```

### E2E Tests (Playwright)

**1. Visual Studio - ER Canvas** (`e2e/specs/metamodel`)
```typescript
test.describe('META-004: Visual Studio - ER Canvas', () => {
  test('ER Canvas displays entities and relations', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/er-canvas');
    
    // Ověř entity nodes
    await expect(page.locator('.entity-node:has-text("Project")')).toBeVisible();
    await expect(page.locator('.entity-node:has-text("User")')).toBeVisible();
    
    // Ověř edge (Project → User, relation: owner)
    const edge = page.locator('.relation-edge[data-source="Project"][data-target="User"]');
    await expect(edge).toBeVisible();
    await expect(edge).toHaveAttribute('data-type', 'MANY_TO_ONE');
    
    // Klikni na entitu → detail panel
    await page.click('.entity-node:has-text("Project")');
    const detailPanel = page.locator('.entity-detail-panel');
    await expect(detailPanel).toBeVisible();
    await expect(detailPanel).toContainText('Fields: 8'); // title, description, ...
    await expect(detailPanel).toContainText('Relations: 3'); // owner, assignees, tasks
  });
  
  test('Filtering by domain', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/er-canvas');
    
    // Filter CRM domain
    await page.selectOption('select[name="domain-filter"]', 'CRM');
    
    // Viditelné jen CRM entity
    await expect(page.locator('.entity-node:has-text("Contact")')).toBeVisible();
    await expect(page.locator('.entity-node:has-text("Project")')).not.toBeVisible();
  });
  
  test('Consistency check highlights issues', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/er-canvas');
    
    // Klikni consistency check
    await page.click('button:has-text("Run Consistency Check")');
    
    // Zobrazí se seznam problémů
    await expect(page.locator('.consistency-error')).toBeVisible();
    await expect(page.locator('text=Missing reference: Entity "Invoice" not found')).toBeVisible();
    
    // Klikni error → highlight entity
    await page.click('text=Missing reference: Entity "Invoice"');
    await expect(page.locator('.entity-node.highlighted')).toBeVisible();
  });
});

test.describe('META-004: Visual Studio - Use-case Canvas', () => {
  test('Use-case canvas displays process flow', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/use-case-canvas');
    
    // Use-case: "Create Project"
    const useCase = page.locator('.use-case-diagram:has-text("Create Project")');
    await expect(useCase).toBeVisible();
    
    // Process steps
    await expect(useCase.locator('.process-step:has-text("1. User fills form")')).toBeVisible();
    await expect(useCase.locator('.process-step:has-text("2. Validate data")')).toBeVisible();
    await expect(useCase.locator('.process-step:has-text("3. Save to DB")')).toBeVisible();
    
    // Entity links
    const entityLink = useCase.locator('.entity-link:has-text("Entity:Project")');
    await expect(entityLink).toBeVisible();
    
    // Klikni entity link → jump to ER View
    await entityLink.click();
    await expect(page).toHaveURL(/\/metamodel\/er-canvas/);
    await expect(page.locator('.entity-node:has-text("Project").highlighted')).toBeVisible();
  });
});
```

**2. AI Copilot** (`e2e/specs/metamodel`)
```typescript
test.describe('META-005: AI Copilot for Metamodel', () => {
  test('AI proposes entity change, user approves', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/studio');
    
    // Otevři AI chat panel
    await page.click('button:has-text("AI Copilot")');
    const chatPanel = page.locator('.ai-chat-panel');
    await expect(chatPanel).toBeVisible();
    
    // User request: "Add time-tracking to Requirements"
    await chatPanel.locator('textarea[name="message"]').fill('Add time-tracking feature to Requirements entity');
    await chatPanel.locator('button:has-text("Send")').click();
    
    // AI response s proposal
    await expect(chatPanel.locator('.ai-message')).toContainText('I propose adding a TimeEntry entity');
    await expect(chatPanel.locator('.proposal-card')).toBeVisible();
    
    // Generuj YAML diff
    await page.click('button:has-text("Generate Proposal YAML")');
    const yamlDiff = page.locator('.proposal-yaml');
    await expect(yamlDiff).toBeVisible();
    await expect(yamlDiff).toContainText('type: ADD_ENTITY');
    await expect(yamlDiff).toContainText('name: TimeEntry');
    
    // Zobrazí se impacted components
    await expect(page.locator('.impacted-components')).toContainText('backend: entities: [TimeEntry]');
    await expect(page.locator('.impacted-components')).toContainText('frontend: views: [TimeEntryForm]');
    
    // User approves (PLATFORM_METAMODEL_ADMIN role)
    await page.click('button:has-text("Approve Proposal")');
    
    // Confirm dialog
    await page.click('button:has-text("Confirm Apply")');
    
    // Status změna: PENDING → APPLIED
    await expect(page.locator('.proposal-status')).toContainText('APPLIED');
    
    // SpecVersion increment
    await expect(page.locator('.spec-version')).toContainText('2.4.0'); // From 2.3.0
    
    // Audit log entry
    await page.goto('/metamodel/audit-log');
    await expect(page.locator('.audit-entry:has-text("timetracking_001")')).toBeVisible();
    await expect(page.locator('text=Event: CHANGE_APPLIED')).toBeVisible();
  });
  
  test('AI warns about breaking changes', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/studio');
    
    await page.click('button:has-text("AI Copilot")');
    await page.locator('textarea[name="message"]').fill('Remove status field from Project entity');
    await page.locator('button:has-text("Send")').click();
    
    // AI warning
    await expect(page.locator('.ai-message.warning')).toBeVisible();
    await expect(page.locator('.ai-message.warning')).toContainText('⚠️ Breaking change detected');
    await expect(page.locator('.ai-message.warning')).toContainText('Removing field "status" will break:');
    await expect(page.locator('.ai-message.warning')).toContainText('- frontend/src/views/ProjectList.tsx');
    await expect(page.locator('.ai-message.warning')).toContainText('- backend/src/api/ProjectController.java');
    
    // Proposal je REJECTED pokud user nesouhlasí
    await page.click('button:has-text("I understand risks, generate proposal anyway")');
    await expect(page.locator('.proposal-yaml')).toBeVisible();
    await expect(page.locator('.breaking-changes-flag')).toContainText('true');
  });
});
```

**3. Contracts & Documentation** (`e2e/specs/metamodel`)
```typescript
test.describe('META-006: Contracts & Documentation Generation', () => {
  test('Metamodel change regenerates OpenAPI spec', async ({ page }) => {
    await loginAsAdmin(page);
    
    // 1. Přidej entitu přes UI
    await page.goto('/metamodel/studio');
    await page.click('button:has-text("Add Entity")');
    await page.fill('input[name="entity-name"]', 'Invoice');
    await page.fill('input[name="table-name"]', 'invoices');
    await page.click('button:has-text("Add Field")');
    await page.fill('input[name="field-name"]', 'amount');
    await page.selectOption('select[name="field-type"]', 'BigDecimal');
    await page.click('button:has-text("Save Entity")');
    
    // 2. Počkej na regeneraci contracts
    await expect(page.locator('.toast:has-text("Contracts regenerated")')).toBeVisible();
    
    // 3. Ověř OpenAPI spec
    const openapi = await page.request.get('http://localhost:8080/api/openapi.json');
    expect(openapi.ok()).toBeTruthy();
    const spec = await openapi.json();
    expect(spec.paths['/api/invoices']).toBeDefined();
    expect(spec.paths['/api/invoices/{id}']).toBeDefined();
    expect(spec.components.schemas['Invoice']).toBeDefined();
    expect(spec.components.schemas['Invoice'].properties.amount.type).toBe('number');
    
    // 4. Ověř AsyncAPI spec
    const asyncapi = await page.request.get('http://localhost:8080/api/asyncapi.json');
    const asyncSpec = await asyncapi.json();
    expect(asyncSpec.channels['events.invoice.created']).toBeDefined();
  });
  
  test('Markdown documentation auto-generated', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/studio');
    
    // Přidej entitu (stejný flow jako výše)
    await page.click('button:has-text("Add Entity")');
    // ... fill form ...
    await page.click('button:has-text("Save Entity")');
    
    // Zobrazí se link na docs
    await expect(page.locator('a:has-text("View Documentation")')).toBeVisible();
    await page.click('a:has-text("View Documentation")');
    
    // Markdown viewer
    await expect(page.locator('.markdown-viewer')).toBeVisible();
    await expect(page.locator('h1:has-text("Entity: Invoice")')).toBeVisible();
    await expect(page.locator('text=Table: invoices')).toBeVisible();
    await expect(page.locator('text=Field: amount (BigDecimal)')).toBeVisible();
    await expect(page.locator('text=API Endpoints:')).toBeVisible();
    await expect(page.locator('code:has-text("POST /api/invoices")')).toBeVisible();
  });
  
  test('n8n can read Metamodel API', async ({ request }) => {
    // Simulace n8n connector
    
    // 1. Načti seznam entit
    const entitiesResponse = await request.get('http://localhost:8080/api/metamodel/entities');
    expect(entitiesResponse.ok()).toBeTruthy();
    const entities = await entitiesResponse.json();
    expect(entities.length).toBeGreaterThan(0);
    
    // 2. Načti detail entity
    const projectResponse = await request.get('http://localhost:8080/api/metamodel/entities/Project');
    expect(projectResponse.ok()).toBeTruthy();
    const project = await projectResponse.json();
    expect(project.name).toBe('Project');
    expect(project.fields).toBeDefined();
    expect(project.relations).toBeDefined();
    
    // 3. Zkontroluj specVersion (cache invalidation)
    const versionResponse = await request.get('http://localhost:8080/api/metamodel/spec-version');
    const version = await versionResponse.json();
    expect(version.specVersion).toMatch(/^\d+\.\d+\.\d+$/); // e.g., "2.3.0"
    
    // n8n by cachoval entities a checknul version při každém run
    // Pokud version != cached version → reload entities
  });
});
```

**4. Governance & Approval Flow** (`e2e/specs/metamodel`)
```typescript
test.describe('META-007: Governance & Approval Flow', () => {
  test('Proposal workflow: create → validate → approve → apply', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/proposals');
    
    // 1. Vytvoř proposal
    await page.click('button:has-text("New Proposal")');
    await page.fill('input[name="proposal-id"]', 'test_proposal_001');
    await page.fill('textarea[name="rationale"]', 'Testing governance workflow');
    
    // Přidej změnu: ADD_ENTITY
    await page.click('button:has-text("Add Change")');
    await page.selectOption('select[name="change-type"]', 'ADD_ENTITY');
    await page.fill('input[name="entity-name"]', 'TestEntity');
    await page.fill('input[name="table-name"]', 'test_entities');
    await page.click('button:has-text("Save Proposal")');
    
    // Status: CREATED
    await expect(page.locator('.proposal-status')).toContainText('CREATED');
    
    // 2. Validace (automatická)
    await page.click('button:has-text("Validate")');
    await expect(page.locator('.toast:has-text("Validation passed")')).toBeVisible();
    await expect(page.locator('.proposal-status')).toContainText('VALIDATED');
    
    // 3. Review (zobrazí diff)
    const diffViewer = page.locator('.proposal-diff');
    await expect(diffViewer).toBeVisible();
    await expect(diffViewer).toContainText('+ name: TestEntity');
    await expect(diffViewer).toContainText('+ table: test_entities');
    
    // 4. Approve (PLATFORM_METAMODEL_ADMIN)
    await page.click('button:has-text("Approve")');
    await page.fill('textarea[name="approval-comment"]', 'Approved for testing');
    await page.click('button:has-text("Confirm Approval")');
    
    await expect(page.locator('.proposal-status')).toContainText('APPROVED');
    
    // 5. Apply
    await page.click('button:has-text("Apply Changes")');
    await expect(page.locator('.toast:has-text("Changes applied successfully")')).toBeVisible();
    await expect(page.locator('.proposal-status')).toContainText('APPLIED');
    
    // 6. Ověř audit log
    await page.goto('/metamodel/audit-log');
    await expect(page.locator('.audit-entry:has-text("test_proposal_001")')).toBeVisible();
    
    const auditEntry = page.locator('.audit-entry:has-text("test_proposal_001")').first();
    await auditEntry.click(); // Expand
    
    await expect(auditEntry).toContainText('Event: PROPOSAL_CREATED');
    await expect(auditEntry).toContainText('Author: admin@core-platform.local');
    await expect(auditEntry).toContainText('Approver: admin@core-platform.local');
    await expect(auditEntry).toContainText('SpecVersion: 2.4.0'); // Increment
  });
  
  test('Tenant admin cannot modify platform entities', async ({ page }) => {
    // Login jako tenant admin (ne platform admin)
    await loginAsTenantAdmin(page, 'acme-corp');
    await page.goto('/metamodel/studio');
    
    // Pokus o editaci platform entity (např. User)
    await page.click('.entity-node:has-text("User")');
    await page.click('button:has-text("Edit Entity")');
    
    // Error: Permission denied
    await expect(page.locator('.error-message')).toBeVisible();
    await expect(page.locator('.error-message')).toContainText('Permission denied: Cannot modify platform entity');
    
    // Tenant admin může editovat jen tenant entities
    await page.click('button:has-text("Add Entity")');
    await page.fill('input[name="entity-name"]', 'CustomField_AcmeCorp');
    await page.fill('input[name="table-name"]', 'custom_fields_acme_corp');
    await page.click('button:has-text("Save Entity")');
    
    // Success
    await expect(page.locator('.toast:has-text("Entity created")')).toBeVisible();
  });
  
  test('Audit log tracks all changes', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/metamodel/audit-log');
    
    // Filtr: last 7 days
    await page.selectOption('select[name="time-range"]', '7d');
    
    // Zobrazí se seznam změn
    await expect(page.locator('.audit-entry').count()).toBeGreaterThan(0);
    
    // Detail entry
    const entry = page.locator('.audit-entry').first();
    await entry.click(); // Expand
    
    // Mandatory fields
    await expect(entry).toContainText(/Event: (PROPOSAL_CREATED|APPROVED|APPLIED)/);
    await expect(entry).toContainText(/Author: .+@.+/); // Email
    await expect(entry).toContainText(/Timestamp: \d{4}-\d{2}-\d{2}/); // Date
    
    // Optional: diff viewer
    if (await entry.locator('.diff-viewer').isVisible()) {
      await expect(entry.locator('.diff-viewer')).toContainText(/[+-]\s/); // Diff format
    }
  });
});
```

### Performance Tests

**1. Metamodel Load Test** (`backend/src/test/java/performance`)
```java
@Test
void testLargeMetamodelGeneration() {
    // Simulace 500 entit, 2000 fields, 1500 relations
    MetamodelGenerator generator = new MetamodelGenerator();
    
    long startTime = System.currentTimeMillis();
    generator.generateFromYaml(largeMetamodelYaml);
    long duration = System.currentTimeMillis() - startTime;
    
    // Generation musí být < 30 sekund
    assertTrue(duration < 30000, "Large metamodel generation took too long: " + duration + "ms");
}

@Test
void testContractGenerationPerformance() {
    // 100 entit → OpenAPI + AsyncAPI
    long startTime = System.currentTimeMillis();
    contractGenerator.generateAll(mediumMetamodel);
    long duration = System.currentTimeMillis() - startTime;
    
    // < 5 sekund
    assertTrue(duration < 5000, "Contract generation took too long: " + duration + "ms");
}
```

### CI/CD Integration Tests

**GitHub Actions workflow** (`.github/workflows/metamodel-ci.yml`)
```yaml
name: Metamodel CI

on:
  push:
    paths:
      - 'backend/src/main/resources/metamodel/**/*.yaml'

jobs:
  validate-and-generate:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Lint YAML
        run: yamllint backend/src/main/resources/metamodel/
      
      - name: Validate Metamodel
        run: cd backend && ./mvnw test -Dtest=MetamodelValidationTest
      
      - name: Generate Contracts
        run: cd backend && ./mvnw exec:java -Dexec.mainClass=cz.muriel.core.metamodel.ContractGenerator
      
      - name: Validate OpenAPI
        run: npx @apidevtools/swagger-cli validate backend/target/generated-sources/openapi.json
      
      - name: Validate AsyncAPI
        run: npx @asyncapi/cli validate backend/target/generated-sources/asyncapi.json
      
      - name: Compile TypeScript Clients
        run: cd frontend && npm run codegen:validate
      
      - name: Run Tests
        run: |
          cd backend && ./mvnw test
          cd frontend && npm test
      
      - name: Commit Generated Files
        if: success()
        run: |
          git add backend/target/generated-sources/*.json
          git add docs/entities/**/*.md
          git commit -m "chore: regenerate contracts from metamodel [skip ci]" || true
          git push
```

---

## 🚀 Future Enhancements

**Phase 6: Multi-Tenancy Enhancements**
- Tenant-specific schema variations
- Column-level tenant isolation
- Shared vs dedicated tables

**Phase 7: Versioning & Migrations**
- Entity version history
- Automatic Flyway migration generation
- Rollback support

**Phase 8: Advanced Code Generation**
- GraphQL schema generation
- REST API documentation (OpenAPI)
- Frontend TypeScript types
- Test scaffolding

---

## 📚 Documentation

**Developer Guides:**
- [Metamodel YAML Reference](../../docs/METAMODEL_YAML_REFERENCE.md)
- [Schema Evolution Guide](../../docs/METAMODEL_SCHEMA_EVOLUTION.md)
- [Generator Capabilities](../../docs/METAMODEL_GENERATOR_CAPABILITIES.md)

**Implementation Details:**
- [Phase 1 Complete](../../docs/METAMODEL_PHASE_1_COMPLETE.md)
- [Phase 2-3 Complete](../../docs/METAMODEL_PHASE_2_3_COMPLETE.md)
- [Final Summary](../../docs/METAMODEL_FINAL_SUMMARY.md)

**Operations:**
- [Testing Guide](../../docs/METAMODEL_TESTING_GUIDE.md)
- [DB Sync Strategy](../../docs/METAMODEL_DB_SYNC_STRATEGY.md)

---

## 🎯 Success Criteria

**Phase 1-3:** ✅ ACHIEVED
- [x] Schema diff detection works
- [x] Hot reload API functional
- [x] UNIQUE constraints auto-created
- [x] Zero compilation errors
- [x] Safe change classification
- [x] Risky change warnings

**Overall Project:**
- [x] 10x faster entity development
- [x] Consistent code architecture
- [x] Zero-downtime schema updates
- [ ] Visual editor (Phase 5)
- [ ] Full constraint support (Phase 4)

---

**Epic Owner:** Development Team  
**Start Date:** 2025-08-15  
**Phase 1-3 Completion:** 2025-09-22  
**Total Duration:** ~6 weeks (vs 8 estimated - 25% ahead of schedule)
