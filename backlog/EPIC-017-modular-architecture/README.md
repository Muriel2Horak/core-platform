# EPIC-017: Modular Architecture & Licensing

**Status:** 🔮 **PLANNED** (0% done, architektonický design)  
**Effort:** ~45 dní (modulární systém + licensing + admin UI + framework API)  
**Priority:** 🔥 **CRITICAL** (strategický foundation pro komerční moduly)  
**Business Value:** €500,000+/rok (komerční moduly + vendor licensing)  
**Timeline:** Q1 2026 (po RBAC, Metamodel, Workflow)

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
- ✅ Ivigee může stavět nad CORE jako dependency (bez forku)
- ✅ Tenant admin vidí jen povolené moduly (RBAC + licensing)
- ✅ Moduly nepřepisují core (namespacy, manifest validace)

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

### Licensing Model

| Module Type | License | Activation | Revenue Model |
|-------------|---------|------------|---------------|
| **Core** (RBAC, Metamodel, Workflow) | MIT | Always enabled | Free |
| **Free Modules** (Task Mgmt, Audit) | MIT | Enabled by default | Free |
| **Premium Modules** (CRM, Helpdesk) | Proprietary | Requires license | €X/tenant/month |
| **Partner Modules** (Ivigee, vendor) | Vendor license | Vendor-issued JWT | Revenue share |

### License Structure (JWT)

**Generated by:** Vendor (CORE Team nebo třetí strana)

```json
{
  "iss": "core-platform.com",
  "sub": "module:helpdesk",
  "aud": "tenant:ivigee",
  "iat": 1704067200,
  "exp": 1735689600,
  "claims": {
    "moduleId": "helpdesk",
    "tenantId": "ivigee",
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
  → 403 Forbidden: "Module 'helpdesk' is not licensed for tenant 'ivigee'"
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

## 🏗️ CORE as Framework (Embedding)

### Distribution Model

**Goal:** Ivigee (or any vendor) can build on CORE without forking.

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
| **Partner Licensing** (Ivigee, third-party vendors) | €150,000 | Revenue share 20% |
| **Enterprise Support** (custom module development) | €100,000 | Professional services |
| **TOTAL** | **€550,000/year** | Conservative estimate |

### Cost Avoidance

- **No forking:** Ivigee builds on CORE, not separate codebase → €200k/year saved
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
- ✅ Ivigee migration to CORE framework
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

**Tenant:** Ivigee buys license for 12 months, 100 users  
**Activation:** Admin uploads JWT license → backend validates → module enabled  
**Usage:** Ivigee users see "Helpdesk" in menu, can create/manage tickets

### Use Case 2: Ivigee Custom Module (Partner)

**Vendor:** Ivigee s.r.o.  
**License:** Ivigee-signed JWT (self-licensing)  
**Module:** "Project Portfolio Management"  
**Built on:** CORE framework (Maven dependencies)  
**Deployment:** Ivigee's own infrastructure (Helm chart)  
**Extends:** CORE entities (`User`, `Group`) + adds `Portfolio`, `Program`  
**Revenue:** Ivigee sells to their clients, CORE gets 20% revenue share

---

## 📚 References

- **Metamodel Engine:** EPIC-002 (entity definitions)
- **Workflow Engine:** EPIC-003 (process automation)
- **RBAC:** EPIC-001 (role-based access control)
- **UI-Spec Engine:** EPIC-004 (dynamic UI rendering)
- **DMS:** EPIC-008 (document storage for module assets)

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
