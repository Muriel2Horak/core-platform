# EPIC-017: Modular Architecture & Custom Products

**Status:** 🔮 **PLANNED** (0% – architektonický design, dependency na EPIC-005/006/011)  
**Definice:** ✅ **100%** (ADM/MOD/LIC/INF/FWK stories specifikovano s AC + tasky)  
**Effort:** ~45 dní (registry, licensing, admin UI, reference modul)  
**Priority:** 🔥 **CRITICAL** (komerční moduly, partner ekosystém)  
**Business Value:** €500k+/rok (placené moduly, partner řešení)  
**Timeline:** Q1 2026 (po stabilizaci RBAC, Metamodel Studia, Workflow Ops)

---

## 🔗 Integrace s ostatními EPICy

| EPIC | Vazba |
|------|-------|
| **EPIC-000 – Security Platform Hardening** | License signing keys, audit logování aktivace modulů, policy enforcement, secret management pro vendor connectors |
| **EPIC-002 – E2E Testing Infrastructure** | Každý modul musí dodat min. jeden E2E scénář (happy path + licensing gate) napojený do Playwright pipeline |
| **EPIC-003 – Monitoring & Observability** | Modul-level metriky (requests, errors, latency), Loki label `moduleId`, dashboardy pro usage/licensing expirace |
| **EPIC-005 – Metamodel Generator & Studio** | Moduly jsou bundly metamodel + UI specifikací; aktivace = publikace overlaye; Studio slouží k tvorbě modulů |
| **EPIC-006 – Workflow Engine** | Moduly registrují workflow definice + W-OPS integraci (state graph, audit, timers) |
| **EPIC-011 – n8n Workflow Automation** | Moduly mohou dodávat n8n flow šablony; runtime komunikuje přes Core connector a respektuje tenant licence |
| **EPIC-020 – Secure SDLC Quality Gates** | Modul repo i manifest prochází stejnými DoD (lint, tests, code review, dependency scanning) |

---

## 🛡️ RBAC & Security napříč moduly

- Moduly nesmí zavádět ad-hoc auth – používají Core RBAC + tenant isolation (Keycloak realm roles + attribute-based rules z EPIC-010).  
- Manifest definuje nové role/scopes (`MODULE_X_ADMIN`, `MODULE_X_USER`, `MODULE_X_VIEWER`), které se registrují v Core RBAC engine.  
- Tenant admin mapuje moduly na role (kdo modul vidí/používá).  
- Všechny přístupy k modulům se auditují (EPIC-000) – aktivace, licence, API usage.  
- Module UI/FE používá Core authorization hooks; backend policies generované z Metamodel Studio (EPIC-005) + modul role metadata.  
- Integrace s Security EPIC: license signing keys, secret storage, threat model pro partner moduly.

---

## 🎯 Executive Summary

**Problém:**  
Core Platform je monolitický systém. Každé nové rozšíření (projektové řízení, helpdesk, CRM) vyžaduje změny v jádru. Nelze:
- Prodávat moduly samostatně (vše je MIT)
- Povolit moduly jen pro konkrétní tenanty
- Dát třetím stranám možnost stavět nad CORE bez forku
- Kontrolovat licenční politiku (expirace, uživatelské limity)

**Řešení:**  
**Modulární architektura s licenčním systémem**:
1. **Modul** = samostatný balíček s manifestem (entities, workflow, UI, connectors)
2. **Module Registry** v runtime (registrace, validace závislostí, lifecycle)
3. **Licensing Engine** (JWT podepsané licence, tenant + modul + expirace)
4. **Admin UI** pro správu modulů (global + tenant level)
5. **CORE as Framework** (Maven artefakty, NPM balíky, stabilní public API)

**Výsledek:**
- ✅ Core zůstává MIT, moduly mohou být komerční
- ✅ Vendor může vydat licenci pro modul X pro tenant Y na 12 měsíců
- ✅ Partner vendor může stavět nad CORE jako dependency (bez forku)
- ✅ Tenant admin vidí jen povolené moduly (RBAC + licensing)
- ✅ Moduly nepřepisují core (namespacy, manifest validace)

---

## 🔍 GAP analýza (Current vs Target)

| Oblast | Stories | Gap / Riziko |
| --- | --- | --- |
| Modul registrace | MOD-001, MOD-002 | Neexistuje loader/registry → nelze aktivovat moduly |
| Licensing | LIC-001..003 | Bez JWT validace a enforcementu nelze monetizovat |
| Admin UI | ADM-001..005 | Chybí katalog, assignment, konfigurace a connectors UI |
| SDK distribuce | FWK-001..004 | Bez SDK/artefaktů nelze budovat externí moduly |
| Bezpečnost & audit | INF-001..003 | Chybí validace manifestu, audit log, sandbox |
| UI integrace | MOD-004 | UI routes/menus nejsou napojeny na modul loader |
| Connector registry | MOD-005 | Bez registry nelze řídit konektory v modulech |

## 🧩 DEV tasky (PENDING) - popis a scope

| DEV task | Popis (high-level) | Výstup |
| --- | --- | --- |
| [MOD-001: Manifest Loader](./stories/MOD-001-module-manifest-loader/README.md) | Scan + parse + hot-reload | Nahrávání manifestů |
| [MOD-002: Module Registry](./stories/MOD-002-module-registry/README.md) | Registry + lifecycle | Evidence modulů |
| [MOD-003: Entity Extension API](./stories/MOD-003-entity-extension-api/README.md) | Metamodel overlay + rollback | Rozšíření entit |
| [MOD-004: UI Manifest Integration](./stories/MOD-004-ui-manifest-integration/README.md) | UI routes/menus + RBAC | Modulové UI |
| [MOD-005: Connector Registry](./stories/MOD-005-connector-registry/README.md) | Registry konektorů | Konektory modulů |
| [LIC-001: License JWT Validation](./stories/LIC-001-license-jwt-validation/README.md) | JWT validace + key mgmt | Ověření licencí |
| [LIC-002: License Enforcement](./stories/LIC-002-license-enforcement-middleware/README.md) | Middleware enforcement | Gate podle licence |
| [LIC-003: License Management API](./stories/LIC-003-license-management-api/README.md) | CRUD licencí + audit | Správa licencí |
| [ADM-001: Global Module Catalog](./stories/ADM-001-global-module-catalog/README.md) | Katalog + akce | Admin katalog |
| [ADM-002: Tenant Module Assignment](./stories/ADM-002-tenant-module-assignment/README.md) | Enable/disable per tenant | Tenant správa |
| [ADM-003: Licensing Dashboard](./stories/ADM-003-licensing-dashboard/README.md) | KPI + expirace | Licenční přehled |
| [ADM-004: Tenant Module Configuration](./stories/ADM-004-tenant-module-configuration/README.md) | Konfigurační UI | Modul konfigurace |
| [ADM-005: Connector Management UI](./stories/ADM-005-connector-management-ui/README.md) | UI pro konektory | Connector UI |
| [INF-001: Manifest Validator](./stories/INF-001-manifest-validator/README.md) | Validace manifestů | Bezpečný loader |
| [INF-002: Module Audit Log](./stories/INF-002-module-audit-log/README.md) | Audit událostí | Observability |
| [INF-003: Module Sandbox](./stories/INF-003-module-sandbox/README.md) | Sandbox politiky | Izolace modulů |
| [FWK-001: Maven Artifacts](./stories/FWK-001-maven-artifacts/README.md) | SDK artefakty | Java SDK |
| [FWK-002: npm Packages](./stories/FWK-002-npm-packages/README.md) | UI SDK balíky | Frontend SDK |
| [FWK-003: Public API Stabilization](./stories/FWK-003-public-api-stabilization/README.md) | Stabilní API | Kompatibilita |
| [FWK-004: Helm Chart Distribution](./stories/FWK-004-helm-chart-distribution/README.md) | Helm chart | Deploy modulu |

---

## 🧾 Definice & Terminologie

### Core Platform
- **Identity + Tenanti:** Keycloak realm-per-tenant, shared IdP, konzistentní RBAC/ABAC guardrails (EPIC-000, EPIC-010).  
- **Metamodel + Workflow Engine:** EPIC-005 (Metamodel Studio) generuje entity/UI bundly, EPIC-006 zajišťuje workflow runtime, audit, timers, streaming presence.  
- **Integration & Streaming Layer:** Kafka/AsyncAPI event bus, Core REST/BFF, oficiální n8n bridge pro orchestraci (EPIC-011).  
- **Observability Stack:** Loki, Prometheus, Grafana dashboards s modul-id labely (EPIC-003).  
- **Security Baseline:** secrets, policy enforcement, compliance/lint gates (EPIC-000 + EPIC-020).  
- **Admin & SDK:** Core runtime + SDK + Admin Console; Core codebase se nikdy neforkuje, vše se dělá přes moduly.

### Modul
- **Balík:** metamodel spec, workflow definice, role/scopes, UI views, integrace, n8n flows, test evidence.  
- **Isolace od Core:** žádný fork; modul se pouze registruje přes manifest a loader zaregistruje entity/workflow/UI.  
- **Aktivace:** jen konfigurací + licencí; code-level změny probíhají přes SDK hooky.  
- **Typy:** Core bundled (OSS), oficiální placené, partner, customer-specific; všechny sdílí stejný lifecycle.

### Custom Produkt
- **Sada modulů** kurátorovaná pro konkrétní doménu (např. "Agile Management Suite / Project Hub").  
- **Branding/Config:** může mít vlastní branding, licencování a výchozí konfiguraci, ale běží na Core infrastruktuře.  
- **Per-tenant řízení:** aktivuje se přes Module Registry, využívá licenční enforcement a sdílí metamodel/workflow/n8n integrace Core.

---

## 📐 Architektura Vision

### Jak to funguje dnes (monolitický problém)

```
┌─────────────────────────────────────────────────┐
│         CORE Platform (monolith)                │
│                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐         │
│  │ Entities│ │ Workflow │ │ Reports │         │
│  └─────────┘ └──────────┘ └─────────┘         │
│                                                 │
│  💥 Každé rozšíření = změna jádra              │
│  💥 Nelze vypnout jen pro tenant               │
│  💥 Nelze licencovat                           │
└─────────────────────────────────────────────────┘
```

### Jak to bude fungovat (modulární systém)

```
┌─────────────────────────────────────────────────────────────┐
│                    CORE Platform (MIT)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Module Registry + Licensing Engine                 │  │
│  │  - Load manifests                                    │  │
│  │  - Validate dependencies                             │  │
│  │  - Check licenses (JWT signature)                    │  │
│  │  - Enable/disable per tenant                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │  Metamodel  │  │  Workflow   │  │    RBAC     │       │
│  │   Engine    │  │   Engine    │  │   Engine    │       │
│  └─────────────┘  └─────────────┘  └─────────────┘       │
└─────────────────────────────────────────────────────────────┘
         ▲                  ▲                  ▲
         │                  │                  │
    ┌────┴────┐        ┌────┴────┐       ┌────┴────┐
    │ MODULE  │        │ MODULE  │       │ MODULE  │
    │ Project │        │Helpdesk │       │   CRM   │
    │  Mgmt   │        │  (paid) │       │  (paid) │
    └─────────┘        └─────────┘       └─────────┘
    
    manifest.json         manifest.json      manifest.json
    + entities            + entities         + entities
    + workflow            + workflow         + workflow
    + UI specs            + UI specs         + UI specs
    + connectors          + connectors       + connectors
    + RBAC scopes         + RBAC scopes      + RBAC scopes
    
    ✅ FREE               🔐 REQUIRES        🔐 REQUIRES
                            LICENSE            LICENSE
```

---

## 📦 Module Registry & Activation

### Central Module Registry (Core Service)
- Eviduje všechny dostupné moduly (`moduleId`, název, vendor, verze, typ **FREE/PAID/CUSTOM**, kompatibilita).  
- Ukládá metadata: požadované migrace, metamodel bundles, workflow/n8n definice, UI balíčky, test status.  
- API:
  ```http
  GET  /api/admin/modules            # katalog
  POST /api/admin/modules/register   # upload manifestu + bundle
  POST /api/admin/modules/{id}/sync  # re-load metamodel/workflow/UI
  ```

### Loader Lifecycle
1. **Discover** (manifest + bundle)  
2. **Validate** (signatura, dependencies, metamodel schema, migrations dry-run)  
3. **Register** (uložení v registry, publikace do Module Catalog)  
4. **Activate** (per-tenant)  
   - Načte metamodel overlay (EPIC-005 API)  
   - Registruje workflow definition (EPIC-006 API)  
   - Zpřístupní UI routes/menu pouze pokud modul aktivní  
   - Nainstaluje n8n flow šablony, pokud existují (EPIC-011 connector)  
5. **Deactivate/Uninstall** (zablokuje UI/API, zachová data, případně rollback migrací)

### Tenant Module Assignment
- Každý tenant má v admin konzoli seznam dostupných modulů + stav (enabled/disabled/licence expired).  
- Aktivace = zápis do `tenant_modules` (moduleId, version, licenseKey, status).  
- Integrace s licensing (viz níže) → bez platné licence se modul neaktivuje.  
- Deaktivace z UI/CLI = loader odregistruje UI routes, workflow triggers, a loguje akci (audit).

---

## 🧩 Co je Modul

### Definice

**Modul** = rozšíření platformy, **NE** forknutí platformy.

Modul může obsahovat:
- ✅ **Nové entity** v metamodelu (např. `Project`, `Ticket`, `Lead`)
- ✅ **Rozšíření existujících entit** (přidat field do `User`, vztah na `Project`)
- ✅ **Workflow definice** (procesy, schvalování, SLA, stavy)
- ✅ **Integrační konektory** (Jira, M365, CRM, fakturace, Slack...)
- ✅ **UI specifikace**: listy, detailovky, dashboardy, formuláře, widgety
- ✅ **Reporty** (přes Reporting Engine / Loki / vlastní)
- ✅ **Vlastní oprávnění** (role/scopes) promítnuté do Keycloak

### Manifest Example

**File:** `modules/project-management/manifest.json`

```json
{
  "id": "project-management",
  "name": "Project Management Module",
  "version": "1.2.0",
  "vendor": "CORE Team",
  "license": "MIT",
  "requires": {
    "core": ">=1.0.0",
    "task-management": ">=1.2.0"
  },
  "provides": {
    "entities": [
      {
        "name": "Project",
        "namespace": "pm",
        "attributes": ["name", "description", "startDate", "budget"],
        "relationships": [
          { "entity": "core.User", "type": "manyToOne", "name": "owner" },
          { "entity": "pm.Task", "type": "oneToMany", "name": "tasks" }
        ]
      }
    ],
    "workflows": [
      {
        "name": "ProjectApproval",
        "states": ["draft", "review", "approved", "active", "closed"],
        "transitions": [ /* ... */ ]
      }
    ],
    "ui": {
      "menu": [
        { "label": "Projects", "route": "/projects", "icon": "folder", "roles": ["PM_USER"] }
      ],
      "views": [
        { "entity": "pm.Project", "type": "list", "spec": "project-list.json" },
        { "entity": "pm.Project", "type": "detail", "spec": "project-detail.json" }
      ]
    },
    "roles": [
      { "name": "PM_ADMIN", "scopes": ["pm:project:*"] },
      { "name": "PM_USER", "scopes": ["pm:project:read", "pm:project:create"] }
    ],
    "connectors": [
      { "type": "jira", "config": "jira-connector.json" }
    ]
  },
  "migrations": [
    { "version": "1.0.0", "file": "migrations/v1.0.0-init.sql" },
    { "version": "1.2.0", "file": "migrations/v1.2.0-add-budget.sql" }
  ]
}
```

### Module Lifecycle

```
┌───────────────┐
│   Available   │  (manifest exists, not installed)
└───────┬───────┘
        │ install
        ▼
┌───────────────┐
│   Installing  │  (running migrations, registering entities)
└───────┬───────┘
        │ success
        ▼
┌───────────────┐
│   Enabled     │  (active, API accessible)
└───────┬───────┘
        │ disable / license expired
        ▼
┌───────────────┐
│   Disabled    │  (logical delete, API returns 403)
└───────┬───────┘
        │ uninstall
        ▼
┌───────────────┐
│  Uninstalled  │  (removed from registry, data retained)
└───────────────┘
```

---

## 🔐 Licensing & Activation

> **Detailní dizajn:** [MODULE_LICENSING.md](../../docs/MODULE_LICENSING.md) & [MODULE_REGISTRY.md](../../docs/MODULE_REGISTRY.md)

### Licensing Model Overview

| Module Type | License | Activation | Revenue Model |
|-------------|---------|------------|---------------|
| **Core features** | MIT | Always enabled | Free |
| **Internal free modules** (Task Mgmt, Audit) | MIT | Enabled by default, lze vypnout | Free |
| **Premium modules** (CRM, Helpdesk, Agile Hub+) | Proprietary | Vyžaduje platnou licenci | €X/tenant/month |
| **Partner modules** | Vendor-specific | Vendor-issued JWT, validované Core | Revenue share |
| **Customer-specific** | Custom (SOW) | Hard-bound na tenant | Project-based fee |

### License Key Format

- **Signed token:** JWT/HMAC nebo RS256, obsahuje `moduleId`, `tenantId`, platnost (`validFrom`, `validTo`), limity (`maxUsers`, `maxInstances`, opt-in feature flags).  
- **Transport:** JSON upload skrz Admin Console nebo API (`POST /admin/license`).  
- **Persistence:** Uložený šifrovaně v `module_licenses`, nikdy na FE.

```json
{
  "iss": "core-platform.com",
  "sub": "module:helpdesk",
  "aud": "tenant:customer-a",
  "iat": 1704067200,
  "exp": 1735689600,
  "claims": {
    "moduleId": "helpdesk",
    "tenantId": "customer-a",
    "maxUsers": 100,
    "features": ["sla", "automations", "reports"],
    "validFrom": "2024-01-01",
    "validTo": "2025-01-01"
  },
  "signature": "<RSA-SHA256 signature>"
}
```

### Verification & Enforcement

1. **Backend verification only:** signatura proti trust-store (EPIC-000), tenant scope match, expirace, licence limity.  
2. **Activation gate:** loader modul nespustí, dokud `licenseState === valid`.  
3. **UI feedback:** menu/cards zobrazují `Active`, `Expiring`, `Expired`; bez licence se zobrazí "Module locked/expired" call-to-action.  
4. **Audit:** každé ověření a pokus o aktivaci logovaný do audit logu (EPIC-000) + Loki label `moduleId`.  
5. **Runtime enforcement:** API vrací `403 ModuleNotLicensed`, workflow triggers se neinicializují, FE route vrací 404.  
6. **Never FE-only validation:** FE čte jen stav z backendu, žádné lokální decode licencí.

### Enforcement
- Modul se neaktivuje bez validní licence (backend blokuje load).  
- UI zobrazuje stavy: `Active`, `License Expiring`, `Expired / Locked`.  
- API vrací `403 ModuleNotLicensed` + audit log, pokud uživatel volá modul bez licence.  
- Licence změny (upload, revoke) se zapisují do audit trailu a exportují do Security analytics (EPIC-000).  
- Pro user-limit enforcement se modul integruje s usage telemetry (počet aktivních userů / instancí).  
- Žádné ověřování pouze na FE; FE vždy rely na backend state.

**API Validation Flow**

```
User → API Request (/api/modules/helpdesk/tickets)
  ↓
Backend checks:
  1. Module exists in registry? ✅
  2. Module enabled for tenant? ✅
  3. License valid? (signature + exp) ✅
  4. User count within limit? ✅
  → Allow request
  
If any check fails:
  → 403 Forbidden: "Module 'helpdesk' is not licensed for tenant 'customer-a'"
```

---

## 🎛️ Admin UI & Configuration

### 3.1 Global Admin (Platform Owner)

**URL:** `https://admin.core-platform.local/global-admin`

**Sections:**

1. **Module Catalog**
   - List all modules (name, version, vendor, license type)
   - Status: Free / Premium / Experimental / Stable
   - Dependencies graph (what depends on what)

2. **Tenant Module Assignment**
   - Table: Tenants × Modules → status (enabled/disabled/expired)
   - Actions:
     - Enable module for tenant
     - Upload license file
     - View migration status
     - Check errors

3. **Licensing Dashboard**
   - Expiring licenses (next 30 days)
   - License usage: tenant X has 87/100 users
   - Audit log: who activated what, when

4. **Connectors Registry**
   - Global connectors (M365, email, SMS, Slack)
   - Modules reference connectors by ID
   - Configure credentials (encrypted)

5. **System Settings**
   - Logging level (per module)
   - Feature flags (enable beta features)
   - Rate limits (per tenant, per module)

### 3.2 Tenant Admin

**URL:** `https://admin.core-platform.local/admin` (tenant context)

**Sections:**

1. **My Modules**
   - List modules available to tenant
   - Status: Active / Inactive / License Expiring
   - Can enable/disable if license allows

2. **Module Configuration**
   - Per-module settings:
     - Helpdesk: SLA times, notification channels
     - CRM: Required fields, stages, pipelines
     - DMS: Storage limits, retention policies

3. **Role Management**
   - Which roles have access to which modules
   - Module-specific scopes (e.g., `helpdesk:ticket:close`)

4. **Integrations**
   - Configure connectors for this tenant:
     - Jira: project mapping
     - M365: mailbox sync
     - Slack: notification webhooks

---

## 🔧 Custom Modules (Internal, Partner, Customer)

### Podporované zdroje
1. **Internal modules** – první-party balíčky udržované Core týmem (Project Hub, Helpdesk).  
2. **Partner modules** – certifikovaní vendori s přístupem do Module Registry, validovaní přes governance pipeline.  
3. **Customer-specific moduly** – projektové rozšíření pro velkého klienta, stále publikované jako modul, ne jako fork.

### Povinné součásti modulu
- **Manifest + bundle:** metamodel YAML/JSON, workflow definice, role a scopes, UI metadata, integrace (+ n8n flows pokud jsou).  
- **Metamodel & migrace:** vše jde přes EPIC-005 pipelines; žádné ad-hoc SQL.  
- **Workflow & automations:** definice kompatibilní s EPIC-006; vlastní executory pouze přes oficiální rozhraní.  
- **UI registrace:** route, menu, RBAC tagy; modul nesmí obcházet Core RBAC/tenant isolation.  
- **Integrations:** reference na schválené konektory; secrets uložené v Core vaultu.  
- **Minimum testů:** API test + E2E "happy path" scénář registrující modul a ověřující základní flow (EPIC-002).  
- **Docs & metadata:** release notes, DoD evidence, n8n flow export, compliance checklist (EPIC-020).

### Governance
- Registrace přes API/Admin UI → modul získá semver verzi, vendor ID, audit identifikátor.  
- Každý release prochází automatickým lintem, security scanem a DoD bránou (lint/tests/licensing).  
- Rollback = registry automaticky reinstaluje předchozí verzi a aplikuje reverse migrace.  
- Modul nesmí měnit core DB schema mimo metamodel pipelines; registry odmítne neschválené změny a loguje incident.

---

## 🏗️ CORE as Framework (Embedding)

### Distribution Model

**Goal:** Partner vendors can build on CORE without forking.

**Artifacts:**

1. **Backend (Maven)**
   ```xml
   <dependency>
     <groupId>cz.muriel.core</groupId>
     <artifactId>core-platform-starter</artifactId>
     <version>1.0.0</version>
   </dependency>
   <dependency>
     <groupId>cz.muriel.core</groupId>
     <artifactId>core-metamodel</artifactId>
     <version>1.0.0</version>
   </dependency>
   ```

2. **Frontend (NPM)**
   ```json
   {
     "dependencies": {
       "@core-platform/ui": "^1.0.0",
       "@core-platform/entity-view": "^1.0.0",
       "@core-platform/workflow-designer": "^1.0.0"
     }
   }
   ```

3. **Docker Compose / Helm Chart**
   ```yaml
   services:
     core-runtime:
       image: core-platform/runtime:1.0.0
       volumes:
         - ./modules:/app/modules
         - ./config:/app/config
   ```

### Public API Stability

**Guaranteed stable interfaces:**

```java
// Module Registration API
public interface ModuleRegistry {
    void registerModule(ModuleManifest manifest);
    void unregisterModule(String moduleId);
    ModuleInfo getModule(String moduleId);
}

// Metamodel Extension API
public interface MetamodelExtension {
    void addEntity(EntityDefinition entity);
    void addAttributeToEntity(String entityName, AttributeDefinition attribute);
    void addRelationship(RelationshipDefinition relationship);
}

// Workflow API
public interface WorkflowRegistry {
    void registerWorkflow(WorkflowDefinition workflow);
    WorkflowInstance startWorkflow(String workflowName, Map<String, Object> context);
}
```

**Versioning:** Semver (1.2.3)
- **Major** (1.x.x): Breaking changes (API signature change)
- **Minor** (x.2.x): New features (backward compatible)
- **Patch** (x.x.3): Bug fixes

**Changelog:** Every release notes breaking changes, deprecations.

---

## 📚 Reference Modul: Agile Management / Project Hub

### Scope
- **Entities:** `Project`, `Epic`, `Story`, `Task`, `Sprint`, `Board`, `Comment`, `Attachment`, `ActivityLog`.  
- **Relations:** Project↔Epic↔Story↔Task chain, Task↔Sprint, Board↔Swimlane, ActivityLog ↔ (Project, Task).  
- **Workflow:** default `To Do → In Progress → In Review → Done`, per-tenant overrides přes Metamodel Studio (EPIC-005) a Workflow Engine (EPIC-006).  
- **UI:** Kanban board (drag & drop), Sprint planning view, Project dashboard, Notifications panel.  
- **Integrace:** Out-of-box n8n flows pro sync s Jira/Trello/Git (přes EPIC-011 connector), streaming eventy `project.*`, `task.*`.  
- **Security:** Role `AGILE_ADMIN`, `AGILE_PM`, `AGILE_USER`; modul respektuje Core RBAC + tenant isolation.

### Cíle Reference Modulu
- Ověřit Module Registry + loader (manifest, migrations, streaming, UI).  
- Ověřit licensing enforcement (Community vs Premium features).  
- Dokázat metamodel-driven UI + workflow binding + streaming telemetry.  
- Integrovat s W-OPS (workflow analytics) a Monitoring stack (module-level metrics).  
- Zajistit E2E test (EPIC-002) – "Create sprint → move tasks → complete sprint → verify event stream".

---

## 📋 Stories Overview

| Category | ID | Story | Effort | Priority | Dependencies |
|----------|----|----|--------|----------|--------------|
| **Module System** | [MOD-001](#mod-001-module-manifest-loader) | Module Manifest Loader | 3d | 🔥 HIGH | Metamodel |
| **Module System** | [MOD-002](#mod-002-module-registry) | Module Registry | 4d | 🔥 HIGH | MOD-001 |
| **Module System** | [MOD-003](#mod-003-entity-extension-api) | Entity Extension API | 3d | 🔥 HIGH | Metamodel |
| **Module System** | [MOD-004](#mod-004-ui-manifest-integration) | UI Manifest Integration | 3d | 🟡 MEDIUM | UI-Spec Engine |
| **Module System** | [MOD-005](#mod-005-connector-registry) | Connector Registry | 2d | 🟡 MEDIUM | - |
| **Licensing** | [LIC-001](#lic-001-license-jwt-validation) | License JWT Validation | 3d | 🔥 HIGH | - |
| **Licensing** | [LIC-002](#lic-002-license-enforcement-middleware) | License Enforcement Middleware | 2d | 🔥 HIGH | LIC-001 |
| **Licensing** | [LIC-003](#lic-003-license-management-api) | License Management API | 2d | 🟡 MEDIUM | LIC-001 |
| **Admin UI** | [ADM-001](#adm-001-global-module-catalog) | Global Module Catalog | 3d | 🔥 HIGH | MOD-002 |
| **Admin UI** | [ADM-002](#adm-002-tenant-module-assignment) | Tenant Module Assignment | 3d | 🔥 HIGH | LIC-002 |
| **Admin UI** | [ADM-003](#adm-003-licensing-dashboard) | Licensing Dashboard | 2d | 🟡 MEDIUM | LIC-003 |
| **Admin UI** | [ADM-004](#adm-004-tenant-module-config) | Tenant Module Configuration | 3d | 🟡 MEDIUM | MOD-002 |
| **Admin UI** | [ADM-005](#adm-005-connector-management-ui) | Connector Management UI | 2d | 🟢 LOW | MOD-005 |
| **Framework** | [FWK-001](#fwk-001-maven-artifacts) | Maven Artifacts (Core Modules) | 4d | 🔥 HIGH | - |
| **Framework** | [FWK-002](#fwk-002-npm-packages) | NPM Packages (UI Components) | 3d | 🔥 HIGH | - |
| **Framework** | [FWK-003](#fwk-003-public-api-stabilization) | Public API Stabilization | 3d | 🔥 HIGH | MOD-002 |
| **Framework** | [FWK-004](#fwk-004-helm-chart-distribution) | Helm Chart Distribution | 2d | 🟡 MEDIUM | - |
| **Infrastructure** | [INF-001](#inf-001-manifest-validator) | Manifest Validator | 2d | 🔥 HIGH | MOD-001 |
| **Infrastructure** | [INF-002](#inf-002-module-audit-log) | Module Audit Log | 2d | 🟡 MEDIUM | - |
| **Infrastructure** | [INF-003](#inf-003-module-sandbox) | Module Sandbox (Test Tenant) | 3d | 🟢 LOW | MOD-002 |
| **TOTAL** | | **20 stories** | **54d** | **11 HIGH / 7 MED / 2 LOW** | **5 categories** |

---

## 🎯 Business Value

### Revenue Potential

| Revenue Stream | Annual Value | Notes |
|----------------|-------------|-------|
| **Premium Modules** (CRM, Helpdesk, Advanced Analytics) | €300,000 | €50/tenant/month × 500 tenants |
| **Partner Licensing** (third-party vendors) | €150,000 | Revenue share 20% |
| **Enterprise Support** (custom module development) | €100,000 | Professional services |
| **TOTAL** | **€550,000/year** | Conservative estimate |

### Cost Avoidance

- **No forking:** Partners build on CORE, not separate codebase → €200k/year saved
- **Module marketplace:** Third-party vendors contribute modules → ecosystem growth
- **Faster TTM:** New features as modules (weeks, not months) → competitive advantage

---

## 🚧 Implementation Phases

### Phase 1: Foundation (Q1 2026, 15 days)
- ✅ MOD-001: Manifest loader
- ✅ MOD-002: Module registry
- ✅ LIC-001: JWT license validation
- ✅ INF-001: Manifest validator

**Milestone:** Can load module manifest, validate license, register in registry.

### Phase 2: Core APIs (Q1 2026, 15 days)
- ✅ MOD-003: Entity extension API
- ✅ MOD-004: UI manifest integration
- ✅ LIC-002: License enforcement middleware
- ✅ FWK-003: Public API stabilization

**Milestone:** Module can add entities, UI specs, API checks licenses.

### Phase 3: Admin UI (Q2 2026, 12 days)
- ✅ ADM-001: Global module catalog
- ✅ ADM-002: Tenant module assignment
- ✅ ADM-003: Licensing dashboard
- ✅ ADM-004: Tenant module config

**Milestone:** Admins can manage modules, upload licenses, configure per tenant.

### Phase 4: Framework Distribution (Q2 2026, 12 days)
- ✅ FWK-001: Maven artifacts
- ✅ FWK-002: NPM packages
- ✅ FWK-004: Helm chart
- ✅ INF-002: Audit log

**Milestone:** External vendors can build on CORE as dependency.

### Phase 5: Ecosystem (Q3 2026, ongoing)
- ✅ First premium module: Helpdesk
- ✅ Partner vendor migration to CORE framework
- ✅ Module marketplace (optional)

---

## 🔒 Security Considerations

1. **License tampering:**
   - Licenses signed with RSA private key (vendor holds key)
   - Backend validates signature before enabling module
   - Frontend never sees license logic

2. **Module isolation:**
   - Modules cannot access other module's data without RBAC
   - Namespaces prevent entity/field conflicts
   - Sandbox tenant for testing before production

3. **Malicious modules:**
   - Code review required for third-party modules
   - Manifest validator checks for suspicious patterns
   - Runtime sandboxing (future: JVM SecurityManager / containers)

4. **License server (optional):**
   - Online license check (daily/weekly)
   - Revocation list (compromised licenses)
   - Usage telemetry (anonymized)

---

## 📊 Success Metrics

**Technical KPIs:**
- Module load time: <500ms
- License validation: <10ms (cached)
- Zero conflicts on module install (namespace validation)
- API uptime: 99.9% (license checks don't affect core)

**Business KPIs:**
- 5+ premium modules available (Q4 2026)
- 50+ tenants with paid modules (Q4 2026)
- €200k+ MRR from licensing (Q1 2027)
- 3+ third-party vendors building on CORE (Q2 2027)

**Developer Experience:**
- Module creation time: <1 day (with generator CLI)
- Documentation completeness: 100% public API
- Breaking changes: <1 per major version
- Community contributions: 10+ modules/year

---

## 🎓 Example Use Cases

### Use Case 1: Helpdesk Module (Premium)

**Vendor:** CORE Team  
**License:** €50/tenant/month  
**Manifest:**
- Entities: `Ticket`, `TicketComment`, `SLA`
- Workflow: `TicketLifecycle` (new, assigned, resolved, closed)
- UI: List, detail, kanban board, reporting dashboard
- Connectors: Email (ticket creation), Slack (notifications)
- Roles: `HELPDESK_AGENT`, `HELPDESK_ADMIN`

**Tenant:** Customer-A buys license for 12 months, 100 users  
**Activation:** Admin uploads JWT license → backend validates → module enabled  
**Usage:** Customer-A users see "Helpdesk" in menu, can create/manage tickets

### Use Case 2: Partner Custom Module

**Vendor:** Partner Vendor s.r.o.  
**License:** Partner-signed JWT (self-licensing)  
**Module:** "Project Portfolio Management"  
**Built on:** CORE framework (Maven dependencies)  
**Deployment:** Partner's own infrastructure (Helm chart)  
**Extends:** CORE entities (`User`, `Group`) + adds `Portfolio`, `Program`  
**Revenue:** Partner sells to their clients, CORE gets 20% revenue share

---

## 🧪 Reference Module: Generic Agile Management / Project Hub

**Účel:** první "proof" modul, na kterém validujeme modulární architekturu end-to-end (manifest → registry → licensing → metamodel-driven UI → workflow → streaming/logging → E2E pipeline).

**Výsledek:** demonstruje, že Core zvládne komerční modul bez forku, s licencováním a multi-tenant izolací.

### Scope & Entities

| Entity | Popis |
|--------|-------|
| `Project` | Top-level container (OKR/initiative) |
| `Epic` | Cross-sprint objective, child of Project |
| `Story` | Uživatelský příběh, child of Epic |
| `Task` | Atomická práce s owners/watchers |
| `Sprint` | Timebox pro plánování, navázaný na Board |
| `Board` | Konfigurace kanbanu, filtry, swimlanes |
| `Comment` | Diskuze s @mentions a notifikacemi |
| `Attachment` | Files z DMS / externích úložišť |
| `ActivityLog` | Streaming audit (status changes, assignments) |

### Workflow & Automation
- Default workflow: **ToDo → In Progress → In Review → Done**, per-tenant možnost přidat extra stavy přes manifest overlay.
- Workflow engine (EPIC-006) obstarává guardrails (permissions, SLA timers, webhooks).
- Streaming: každá změna `ActivityLog` posílá event do Kafka (moduleId label) + n8n trigger pro synchronizace.

### Feature Set
- Kanban board s drag & drop (per Board swimlanes).
- Sprint board + basic burndown, velocity.
- Watchers, mentions, email/Slack notifikace.
- Inline komentáře + attachments (DMS integration).
- n8n templaty pro sync s Jira/Trello/Git (bidirectional).
- License state banner + link na Admin Console.

### Architecture Hooks
- **Metamodel:** entity definitions + UI view specs generované ve Studio (EPIC-005) → loader publikuje do Core.
- **Workflow:** `project_hub_flow.yaml` registrovaný přes Workflow API (EPIC-006).
- **n8n:** `project-hub-sync.json` template volající Core connector.
- **Monitoring:** Prometheus counter `module_project_hub_requests_total{tenantId,...}` + Loki label `moduleId=project-hub`.
- **Security:** modul definuje role `PROJECT_HUB_ADMIN`, `PROJECT_HUB_USER`, `PROJECT_HUB_VIEWER`, mapované na tenant role; žádný vlastní auth.
- **Licensing:** modul je placený, bez licence se nezobrazí menu a API končí 403.

### Manifest Snapshot

**File:** `modules/project-hub/module.yaml`

```yaml
module_id: agile-project-hub
name: Generic Agile Management / Project Hub
type: PREMIUM
version: 1.0.0
license_required: true

entrypoints:
  fe:
    route: /app/project-hub
    menuLabel: Project Hub
    icon: kanban
    permission: PROJECT_HUB_USER
  api:
    basePath: /api/modules/project-hub
  workflows:
    - file: workflows/project_hub_flow.yaml

metamodel:
  bundles:
    - file: metamodel/project.yml
    - file: metamodel/task.yml
  ui:
    - file: ui/project-board.json
    - file: ui/task-detail.json

n8n:
  templates:
    - file: n8n/jira-sync.json
    - file: n8n/git-webhook.json

roles:
  - PROJECT_HUB_ADMIN
  - PROJECT_HUB_USER
  - PROJECT_HUB_VIEWER

tests:
  e2e: e2e/project-hub-license.spec.ts
```

### License Scenarios & Activation
- Trial licence (30 dní) → UI banner + telemetry event.
- Paid licence → modul se aktivuje přes Admin Console, loader registruje entity/workflow/UI + n8n templates.
- Expired licence → modul přejde do `locked` stavu, UI cards ukáží CTA "Renew license", API vrací 403.
- Audit log: kdo modul aktivoval/deaktivoval, kdo nahrál licenci.

### Testing & DoD
- **API & Workflow tests:** verify CRUD, workflow transitions, streaming events.
- **E2E scénář:** `project-hub-license.spec.ts` – aktivace modulu, ověření, že Tenant A/B jsou izolovaní, licensing gating funguje.
- **Performance smoke:** Kanban drag-drop + sprint planning se logují do Grafany (latency < 200ms p95).
- **Security:** RBAC perms mapované v Keycloak, audit entries v Loki.
- **Documentation:** manifest schema, admin guide, integration cookbook for n8n/Trello/Jira.
- **Goal validation:** architektura modulu, licensing enforcement, metamodel-driven UI, workflow streaming a logování, E2E pipeline – vše musí být prokázáno v CI reportu.

---

## 🎯 Deliverables

### v1 (Launch)
1. **Module Registry (BE + UI)** – registrace manifestů, dependency graph, health status.  
2. **Tenant module assignment + licensing enforcement** – admin workflows, audit log, API guard.  
3. **Module SDK & conventions** – referenční repo, manifest schema, CI templates.  
4. **Reference modul "Agile Management Lite / Project Hub"** – aktivovaný tenant, end-to-end demo.  
5. **E2E scénář** – automat test ověřující aktivaci/licenci a základní CRUD/kanban flow.

### v2 (Scale & Marketplace)
1. **Module Marketplace UI** – katalog modulů, filtry (free/premium/partner), detail view.  
2. **Remote registry / vendor onboarding** – možnost připojit externí vendor registry (API/SCM).  
3. **Usage telemetry** – per-modul statistiky (aktivní uživatelé, eventy, latence) s opt-in nastavením.  
4. **Advanced orchestration** – rolling upgrade modulu, canary rollout, multi-region sync.  
5. **Partner automation** – self-service onboarding (lint/security scans), revenue reporting, license distribution.

---

## ✅ Definition of Done

- **Module Registry + Loader** běží v Core, umí registrovat/aktivovat/deaktivovat modul a loguje každou akci do audit trailu.  
- **Licensing enforcement** blokuje modul bez platné licence (API 403, UI banner), expirace se propisuje do Admin Console a monitoringu.  
- **Tenant assignment UI** umožňuje per-tenant zapnout/vypnout modul, nahrát licenci a přiřadit role (`PROJECT_HUB_*`).  
- **Reference Modul Project Hub** je nasaditelný, používá manifest+workflow+n8n bundly, poskytuje kanban/sprint experience a metriky.  
- **Automatizované testy** (unit + API + E2E) běží v CI, zahrnují licensing, multi-tenant izolaci a n8n sync smoke.  
- **Docs & SDK** obsahují manifest schema, vývojářský postup, DoD checklist a odkaz na související EPICy.

---

## 📚 References

- **Security Baseline:** EPIC-000  
- **Metamodel Studio & tooling:** EPIC-005  
- **Workflow Engine & W-OPS:** EPIC-006  
- **n8n Integration & connectors:** EPIC-011  
- **Secure SDLC Quality Gates:** EPIC-020  
- **Documentation & SDK drafts:** `docs/MODULE_REGISTRY.md`, `docs/MODULE_LICENSING.md`, `docs/modules/PRODUCT_TEMPLATES.md`

---

**For detailed implementation, see individual stories:**
- Module System: [MOD-001 through MOD-005](#stories-overview)
- Licensing: [LIC-001 through LIC-003](#stories-overview)
- Admin UI: [ADM-001 through ADM-005](#stories-overview)
- Framework: [FWK-001 through FWK-004](#stories-overview)
- Infrastructure: [INF-001 through INF-003](#stories-overview)

---

**Last Updated:** 9. listopadu 2025  
**Status:** 🔮 PLANNED (architektonický návrh kompletní)  
**Next Action:** Finalize manifest schema + create MOD-001 story
