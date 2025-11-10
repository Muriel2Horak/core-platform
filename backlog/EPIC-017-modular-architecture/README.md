# EPIC-017: Modular Architecture & Custom Products

**Status:** 🔮 **PLANNED** (0% – architektonický design, dependency na EPIC-005/006/011)  
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

## 🧾 Definice & Terminologie

### Core Platform
- **Identity & Tenanti:** Keycloak multi-realm (tenant = subdoména = realm) s RBAC/ABAC guardrails (EPIC-000, EPIC-010).  
- **Metamodel & Workflow:** EPIC-005 (Metamodel Studio) + EPIC-006 (Workflow Engine) + EPIC-006 W-OPS dashboard.  
- **Streaming & Integrace:** Kafka/AsyncAPI event bus, EXTERNAL_TASK konektor na n8n (EPIC-011), Core APIs.  
- **Observabilita:** Loki, Prometheus, Grafana (EPIC-003).  
- **Security Baseline:** audit, secrets, policy enforcement (EPIC-000).  
- **n8n Integration Bridge:** oficiální konektor + BFF (EPIC-011).  
- Core poskytuje framework a SDK, nikdy se neforkuje kvůli modu.

### Modul
- Distribuovaný balíček obsahující kombinaci: metamodel spec (entities, tenant scopes), workflow definice, role/scopes, UI views, connectors, n8n flow šablony, testy.  
- Modul se aktivuje konfigurací/licencí; žádné změny v core codebase.  
- Typy: **Core bundled**, **Premium/licencované**, **Partner**, **Custom (customer-specific)**.

### Custom Produkt
- Předpřipravená sada modulů + branding pro konkrétní doménu (např. “Agile Management Suite / Project Hub”).  
- Využívá modulární architekturu: modul bundles + per-tenant aktivace + workflow + streaming.  
- Deploy stále běží na Core runtime (sdílí identity, observabilitu, security).

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

> **Kompletní dokumentace:** [MODULE_LICENSING.md](../../docs/MODULE_LICENSING.md)

> **Registry Architecture:** [MODULE_REGISTRY.md](../../docs/MODULE_REGISTRY.md)

## 🔐 Licensing Model

| Module Type | License | Activation | Revenue Model |
|-------------|---------|------------|---------------|
| **Core features** | MIT | Always enabled | Free |
| **Internal free modules** (Task Mgmt, Audit) | MIT | Enabled by default, lze vypnout | Free |
| **Premium modules** (CRM, Helpdesk, Agile Hub+) | Proprietary | Vyžaduje platnou licenci | €X/tenant/month |
| **Partner modules** | Vendor-specific | Vendor-issued JWT, validované Core | Revenue share |
| **Customer-specific** | Custom (SOW) | Hard-bound na tenant | Project-based fee |

### License Token (Signed JWT)

- **Format:** JWT (RS256/HMAC), claimy: `moduleId`, `tenantId`, `validFrom`, `validTo`, `limits` (uživatelé, instancí, feature flags).  
- **Storage:** Admin nahraje token přes API/Console; encrypted v `module_licenses`.  
- **Verification (backend only):**
  1. Ověř signaturu proti trust store (EPIC-000).  
  2. Ověř tenantId vs. aktuální realm.  
  3. Ověř platnost (`iat`, `exp`, limity).  
  4. Logni výsledek (audit trail).

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

### Enforcement
- Modul se neaktivuje bez validní licence (backend blokuje load).  
- UI zobrazuje stavy: `Active`, `License Expiring`, `Expired / Locked`.  
- API vrací `403 ModuleNotLicensed` + audit log, pokud uživatel volá modul bez licence.  
- Licence změny (upload, revoke) se zapisují do audit trailu a exportují do Security analytics (EPIC-000).  
- Pro user-limit enforcement se modul integruje s usage telemetry (počet aktivních userů / instancí).  
- Žádné ověřování pouze na FE; FE vždy rely na backend state.

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

**Validation Flow:**

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

### Typy Modulů
1. **Internal modules** – vyvíjené CORE týmem (Project Hub, Helpdesk).  
2. **Partner modules** – certifikovaní vendori, publikují balíčky přes Module Registry.  
3. **Customer-specific** – moduly vzniklé pro konkrétní projekt (např. velký enterprise tenant).

### Požadavky na modul
- **Manifest** + balíček (`metamodel bundle`, `workflow defs`, `UI spec`, `n8n flows` volitelně, `connectors`).  
- **Metamodel YAML/JSON** – validovaný EPIC-005 nástroji, včetně `tenant_scope` a streaming sekce.  
- **Workflow definitions** – kompatibilní s EPIC-006; modul může přidat nové executory pouze přes definované rozhraní.  
- **UI registrace** – route, menu, RBAC tagy; modul nemůže ignorovat Core RBAC.  
- **Tests:** min. API + E2E happy path (napojení na EPIC-002).  
- **Migration bundle** – musí používat metamodel migration engine, nikoliv ruční SQL.  
- **N8n flows (optional)** – export JSON šablon + binding na Core connector.

### Governance
- Registrace probíhá přes Module Registry API nebo Global Admin UI; modul dostane semver verzi a audit ID.  
- Každý release = nový balík + migrace + test evidence.  
- Rollback = registry provede `uninstall + reinstall` s předchozí verzí (data zachována).  
- Modul nemůže měnit Core DB schema mimo metamodel pipelines; registry blokuje neautorizované změny.

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
- Zajistit E2E test (EPIC-002) – “Create sprint → move tasks → complete sprint → verify event stream”.

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

## 🏗️ Prototype Module: delivery-suite

**Purpose:** První funkční modul demonstrující celý module system (manifest, licensing, multi-tenant isolation, FE/BE integration)

### Overview

| Property | Value |
|----------|-------|
| **Module ID** | `delivery-suite` |
| **Name** | Delivery Suite |
| **Type** | EXTENSION |
| **License Required** | ✅ Yes |
| **Description** | Generic agile work management - issue tracking, sprints, kanban boards (NOT Jira-branded) |

**Why "Delivery Suite"?**
- Generic name (ne "Jira clone" nebo "EPIC-010 Agile")
- Fokus na "delivery" (dodání hodnoty), ne "agile" buzzword
- Příklad modulu, ne produkční feature

---

### Entity: DeliveryItem

**Purpose:** Generic work item/ticket/issue (agnostic naming)

```java
@Entity
@Table(name = "delivery_items")
@MultiTenant  // Automatic tenant_id column + filter
public class DeliveryItem extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  @Column(unique = true, nullable = false)
  private String key;  // Auto-generated: DLV-1, DLV-2, ...
  
  @Column(nullable = false)
  private String title;
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;  // NEW, IN_PROGRESS, DONE
  
  @Column
  private String assignee;  // User ID nebo email
  
  @Enumerated(EnumType.STRING)
  private Priority priority;  // LOW, MEDIUM, HIGH, CRITICAL
  
  @Column
  private LocalDate dueDate;
  
  @Column(columnDefinition = "TEXT[]")
  private String[] tags;
  
  // Multi-tenant isolation
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;  // From JWT context
  
  // Audit fields
  private Instant createdAt;
  private Instant updatedAt;
  private String createdBy;
  private String updatedBy;
}
```

---

### Workflow Definition

**Workflow ID:** `delivery_lifecycle`

**States:**
```
NEW → IN_PROGRESS → DONE
```

**Transitions:**
- `NEW → IN_PROGRESS`: "start_work" (permission: `MODULE_DELIVERY_ACCESS`)
- `IN_PROGRESS → DONE`: "complete" (permission: `MODULE_DELIVERY_ACCESS`)
- `IN_PROGRESS → NEW`: "reopen" (permission: `MODULE_DELIVERY_ADMIN`)

**Integration:** Používá existující EPIC-006 Workflow Engine, žádná nová infrastruktura

---

### Frontend Views

**Route:** `/app/delivery` (visible only if licensed)

#### 1. Table View

**Columns:**
- Key (DLV-1, DLV-2, ...)
- Title
- Status (badge: NEW 🔵 | IN_PROGRESS 🟡 | DONE 🟢)
- Assignee (avatar + name)
- Priority (badge: LOW | MEDIUM | HIGH | CRITICAL)
- Due Date (with overdue warning)

**Features:**
- Filtering: by assignee, status, text search
- Sorting: by any column
- Actions: Create, Edit, Delete, Bulk Status Update

---

#### 2. Kanban Board

**Layout:**
```
┌─────────────────┬─────────────────┬─────────────────┐
│      NEW        │   IN PROGRESS   │      DONE       │
├─────────────────┼─────────────────┼─────────────────┤
│ ┌─────────────┐ │ ┌─────────────┐ │ ┌─────────────┐ │
│ │ DLV-1       │ │ │ DLV-3       │ │ │ DLV-5       │ │
│ │ Fix bug...  │ │ │ Add feature │ │ │ Completed   │ │
│ │ @john       │ │ │ @mary       │ │ │ @alice      │ │
│ │ 🔴 HIGH     │ │ │ 🟡 MEDIUM   │ │ │ ✅ Done     │ │
│ └─────────────┘ │ └─────────────┘ │ └─────────────┘ │
│ ┌─────────────┐ │ ┌─────────────┐ │                 │
│ │ DLV-2       │ │ │ DLV-4       │ │                 │
│ │ New task    │ │ │ Testing...  │ │                 │
│ └─────────────┘ │ └─────────────┘ │                 │
└─────────────────┴─────────────────┴─────────────────┘
```

**Features:**
- Drag-and-drop between columns (triggers workflow transition)
- Card shows: title, assignee, priority badge, due date
- Filtering: by assignee, tags, priority
- Swimlanes (future): by assignee, priority, sprint

---

#### 3. Detail View

**Sections:**
- **Header:** Key (DLV-123), Status badge, Priority, Due Date
- **Content:** Editable title, description (Markdown editor)
- **Metadata:** Assignee dropdown, Tags input, Created/Updated timestamps
- **Workflow:** State diagram (visual current state + available transitions)
- **Comments:** Thread with @mentions (optional DMS integration)
- **Audit Log:** Table (who changed what field, when)

**Actions:**
- Save changes
- Workflow transitions (buttons: "Start Work", "Complete", "Reopen")
- Delete item (confirmation modal)

---

### Module Manifest

**File:** `modules/delivery-suite/module.yaml`

```yaml
module_id: delivery-suite
name: Delivery Suite
description: Agile work management - generic issue tracking, sprints, kanban boards
type: EXTENSION
version: 1.0.0
license_required: true

entrypoints:
  fe:
    route: /app/delivery
    permission: MODULE_DELIVERY_ACCESS
    menuLabel: Delivery Board
    icon: kanban
    weight: 100
  api:
    basePath: /api/modules/delivery
  wf:
    definitions:
      - delivery_lifecycle

requires:
  core: ">=1.0.0"
  workflow-engine: ">=2.1.0"

provides:
  entities:
    - DeliveryItem
  permissions:
    - MODULE_DELIVERY_ACCESS
    - MODULE_DELIVERY_ADMIN
```

---

### License Enforcement

**Scenario 1: Without Valid License**

**Behavior:**
- ❌ Frontend route `/app/delivery` hidden (not in menu, 404 if accessed directly)
- ❌ API calls to `/api/modules/delivery/*` return `403 Forbidden`
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "message": "Module 'delivery-suite' requires a license. Contact sales.",
    "error_code": "FEATURE_DISABLED"
  }
  ```
- ℹ️ Admin UI (`/admin/modules`) shows:
  - Module card with 🔵 "License Required" badge
  - Button: "Upload License" → opens modal for JWT upload

---

**Scenario 2: With Valid License**

**Behavior:**
- ✅ Menu item "Delivery Board" visible (icon: kanban)
- ✅ Route `/app/delivery` accessible
- ✅ API calls allowed
- ✅ Admin UI shows:
  - Module card with 🟢 "Active" badge
  - License info: "Valid until 2025-12-31" (green text)
  - Button: "Manage Module" → opens config editor

---

**Scenario 3: With Trial License**

**Behavior:**
- ✅ Module accessible (fully functional)
- ⚠️ Warning banner at top of `/app/delivery` page:
  ```
  ┌───────────────────────────────────────────────────┐
  │ ⚠️ Trial License - Expires in 15 days            │
  │ Upgrade to full license: [Contact Sales]         │
  └───────────────────────────────────────────────────┘
  ```
- ℹ️ Admin UI shows:
  - Module card with 🟡 "Trial" badge
  - Trial countdown: "Trial ends 2025-02-01 (15 days left)"
  - Button: "Upgrade License" → sales contact form

---

**Scenario 4: License Expired**

**Behavior:**
- ❌ Frontend route redirects to `/app/home` with notification:
  ```
  License for Delivery Suite expired. Contact sales to renew.
  ```
- ❌ API returns `403 Forbidden`:
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "message": "Module 'delivery-suite' license expired. Contact sales to renew.",
    "error_code": "LICENSE_EXPIRED"
  }
  ```
- ℹ️ Admin UI shows:
  - Module card with 🔴 "Expired" badge
  - Message: "License expired on 2024-12-31"
  - Button: "Renew License" → upload new JWT

---

### Multi-Tenant Isolation

**Database Level:**
```sql
-- Tenant A creates item
INSERT INTO delivery_items (tenant_id, key, title, status)
VALUES ('acme-corp', 'DLV-1', 'Fix bug', 'NEW');

-- Tenant B queries
SELECT * FROM delivery_items WHERE tenant_id = 'tenant-b';
-- Result: 0 rows (Tenant A's data not visible)
```

**Application Level:**
```java
@GetMapping("/items")
public List<DeliveryItem> getItems(@TenantId String tenantId) {
  // tenantId ALWAYS from JWT, NEVER from request parameter
  // Repository auto-filters by tenantId (Hibernate @Filter)
  return deliveryRepo.findAll();  // Only current tenant's items
}
```

**E2E Test Verification:**
```typescript
test('Tenant A cannot see Tenant B delivery items', async ({ page, context }) => {
  // Create item as Tenant A
  await loginAsTenant(page, 'acme-corp');
  await createDeliveryItem(page, 'Secret item for Tenant A');
  
  // Switch to Tenant B
  const page2 = await context.newPage();
  await loginAsTenant(page2, 'tenant-b');
  await activateModule(page2, 'delivery-suite');  // Give license
  
  // Navigate to delivery board
  await page2.goto('/app/delivery');
  
  // Verify Tenant A's item NOT visible
  await expect(page2.locator('text=Secret item for Tenant A')).not.toBeVisible();
  
  // API verification
  const items = await page2.request.get('/api/modules/delivery/items');
  const json = await items.json();
  expect(json.every(item => item.tenant_id === 'tenant-b')).toBe(true);
});
```

---

### Implementation Effort

| Component | LOC Estimate | Time Estimate | Priority |
|-----------|--------------|---------------|----------|
| **Backend** |
| Entity (DeliveryItem) | 150 | 2h | 🔥 HIGH |
| Repository + Service | 200 | 3h | 🔥 HIGH |
| REST Controller | 250 | 4h | 🔥 HIGH |
| Workflow definition | 100 | 2h | 🟡 MEDIUM |
| **Frontend** |
| Table view | 300 | 6h | 🔥 HIGH |
| Kanban board | 400 | 8h | 🟡 MEDIUM |
| Detail view | 250 | 5h | 🟡 MEDIUM |
| License enforcement UI | 150 | 3h | 🔥 HIGH |
| **Module System Integration** |
| Module manifest | 50 | 1h | 🔥 HIGH |
| License guard integration | 100 | 2h | 🔥 HIGH |
| **Testing** |
| Unit tests | 200 | 4h | 🟡 MEDIUM |
| Integration tests | 150 | 3h | 🟡 MEDIUM |
| E2E tests | 200 | 4h | 🔥 HIGH |
| **TOTAL** | **~2,500 LOC** | **~47h** | **6-8 days** |

---

### Success Criteria

**Functional:**
- ✅ Modul se načte ze YAML manifestu při startu
- ✅ Bez licence: menu hidden, API returns 403
- ✅ S licencí: menu visible, CRUD funguje
- ✅ Trial license: funguje + warning banner
- ✅ Expired license: přístup zablokován
- ✅ Multi-tenant: Tenant A nevidí data Tenant B

**Technical:**
- ✅ Zero hardcoded module logic v core (vše přes registry)
- ✅ Workflow engine integration (delivery_lifecycle workflow)
- ✅ RBAC integration (MODULE_DELIVERY_ACCESS permission)
- ✅ Audit log (všechny změny DeliveryItem logged)

**Testing:**
- ✅ 100% code coverage (unit tests)
- ✅ Integration tests (license scenarios)
- ✅ E2E tests (tenant isolation, licensing)

---

## 🎯 Deliverables

### v1 (Launch)
1. **Module Registry (BE + UI)** – registrace manifestů, dependency graph, health status.  
2. **Tenant module assignment + licensing enforcement** – admin workflows, audit log, API guard.  
3. **Module SDK & conventions** – referenční repo, manifest schema, CI templates.  
4. **Reference modul “Agile Management Lite / Project Hub”** – aktivovaný tenant, end-to-end demo.  
5. **E2E scénář** – automat test ověřující aktivaci/licenci a základní CRUD/kanban flow.

### v2 (Scale & Marketplace)
1. **Module Marketplace UI** – katalog modulů, filtry (free/premium/partner), detail view.  
2. **Remote registry / vendor onboarding** – možnost připojit externí vendor registry (API/SCM).  
3. **Usage telemetry** – per-modul statistiky (aktivní uživatelé, eventy, latence) s opt-in nastavením.  
4. **Advanced orchestration** – rolling upgrade modulu, canary rollout, multi-region sync.  
5. **Partner automation** – self-service onboarding (lint/security scans), revenue reporting, license distribution.

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
