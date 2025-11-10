# Module Registry Architecture

**Version:** 1.0.0  
**Last Updated:** 2025-10-26  
**Status:** 🔮 PLANNED

---

## 📖 Overview

**Module Registry** je centrální systém pro správu modulů v Core Platform. Umožňuje:
- ✅ Dynamické načítání modulů při startu aplikace
- ✅ Per-tenant aktivaci/deaktivaci modulů
- ✅ Verzování a dependency management
- ✅ Hot-swap modulů (future enhancement)

---

## 🗄️ Database Schema

### `core_module_catalog` (Global Module Definitions)

**Purpose:** Definuje všechny dostupné moduly v systému (globální katalog)

```sql
CREATE TABLE core_module_catalog (
  module_id VARCHAR(64) PRIMARY KEY,        -- e.g., "delivery-suite", "agile-hub", "helpdesk"
  name VARCHAR(255) NOT NULL,               -- e.g., "Delivery Suite"
  description TEXT,
  version VARCHAR(32) NOT NULL,             -- Semantic versioning (1.2.3)
  type VARCHAR(32) NOT NULL,                -- BUILTIN | EXTENSION | EXTERNAL
  license_required BOOLEAN DEFAULT false,   -- If true, tenant needs valid license
  entrypoints JSONB,                        -- { "fe": {...}, "api": {...}, "wf": [...] }
  status VARCHAR(32) DEFAULT 'ACTIVE',      -- ACTIVE | DEPRECATED | HIDDEN
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(255)                   -- User who modified (audit)
);

CREATE INDEX idx_module_status ON core_module_catalog(status);
CREATE INDEX idx_module_type ON core_module_catalog(type);
```

**Columns Explained:**

| Column | Type | Description | Example Values |
|--------|------|-------------|----------------|
| `module_id` | VARCHAR(64) | Unique identifier (kebab-case) | `delivery-suite`, `helpdesk`, `crm-plus` |
| `name` | VARCHAR(255) | Human-readable name | `Delivery Suite`, `Helpdesk Pro` |
| `description` | TEXT | Detailed description | `Agile work management module with sprints and kanban boards` |
| `version` | VARCHAR(32) | Semantic version | `1.0.0`, `2.3.1-beta` |
| `type` | VARCHAR(32) | Module type | `BUILTIN`, `EXTENSION`, `EXTERNAL` |
| `license_required` | BOOLEAN | Needs license to use? | `true`, `false` |
| `entrypoints` | JSONB | Module integration points | See [Entrypoints Structure](#entrypoints-structure) |
| `status` | VARCHAR(32) | Lifecycle status | `ACTIVE`, `DEPRECATED`, `HIDDEN` |
| `created_at` | TIMESTAMP | When registered | `2025-01-15 10:30:00` |
| `updated_at` | TIMESTAMP | Last modification | `2025-02-20 14:15:00` |
| `updated_by` | VARCHAR(255) | Who modified | `admin`, `system` |

**Module Types:**

- **BUILTIN** - Core platform modules (always available, can't be disabled)
  - Example: `rbac`, `metamodel`, `workflow-engine`
  
- **EXTENSION** - Official modules (developed by CORE Team, can be licensed)
  - Example: `delivery-suite`, `helpdesk`, `crm-plus`
  
- **EXTERNAL** - Third-party vendor modules (custom JARs, SPI-based)
  - Example: `vendor-custom-reporting`, `partner-integration`

**Status Lifecycle:**

```
ACTIVE → Module is ready for use (shown in catalog, can be activated)
  ↓
DEPRECATED → Module is phased out (shown with warning, no new activations)
  ↓
HIDDEN → Module is removed from catalog (existing tenants keep access)
```

### Entrypoints Structure

**Stored in `entrypoints` JSONB column:**

```json
{
  "fe": {
    "route": "/app/delivery",
    "permission": "MODULE_DELIVERY_ACCESS",
    "menuLabel": "Delivery Board",
    "icon": "kanban",
    "weight": 100
  },
  "api": {
    "basePath": "/api/modules/delivery",
    "version": "v1"
  },
  "wf": {
    "definitions": [
      "delivery_approval_v1",
      "sprint_automation"
    ]
  },
  "n8n": {
    "flows": [
      "delivery_notifications",
      "sprint_sync"
    ]
  },
  "reporting": {
    "dashboards": [
      "delivery_metrics",
      "sprint_velocity"
    ]
  }
}
```

**Entrypoint Fields:**

| Section | Field | Type | Description | Example |
|---------|-------|------|-------------|---------|
| `fe` | `route` | string | Frontend route path | `/app/delivery` |
| `fe` | `permission` | string | Required permission | `MODULE_DELIVERY_ACCESS` |
| `fe` | `menuLabel` | string | Menu item text | `Delivery Board` |
| `fe` | `icon` | string | Icon name (Material/Lucide) | `kanban`, `clipboard` |
| `fe` | `weight` | number | Menu ordering (higher = lower in menu) | `100` |
| `api` | `basePath` | string | API prefix | `/api/modules/delivery` |
| `api` | `version` | string | API version | `v1`, `v2` |
| `wf` | `definitions` | array | Workflow IDs (EPIC-006) | `["approval_v1"]` |
| `n8n` | `flows` | array | n8n workflow names | `["notifications"]` |
| `reporting` | `dashboards` | array | Report/dashboard IDs | `["metrics_board"]` |

---

### `core_tenant_modules` (Per-Tenant Module Activation & Licensing)

**Purpose:** Tracks which tenants have which modules activated + license status

```sql
CREATE TABLE core_tenant_modules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id VARCHAR(255) NOT NULL,          -- From JWT (realm/subdomain)
  module_id VARCHAR(64) NOT NULL REFERENCES core_module_catalog(module_id),
  
  -- Licensing
  license_state VARCHAR(32) NOT NULL DEFAULT 'MISSING',  -- VALID | EXPIRED | MISSING | INVALID | TRIAL
  license_payload TEXT,                     -- Encrypted JWT token or license key
  license_expires_at TIMESTAMP,             -- Expiration date (from JWT)
  
  -- Configuration
  config JSONB,                             -- Tenant-specific settings (SLA times, limits, etc.)
  enabled BOOLEAN DEFAULT false,            -- Admin can enable/disable
  
  -- Audit
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(255),                  -- Admin user who activated
  
  UNIQUE(tenant_id, module_id)              -- One record per tenant+module
);

CREATE INDEX idx_tenant_modules ON core_tenant_modules(tenant_id);
CREATE INDEX idx_module_license ON core_tenant_modules(module_id, license_state);
```

**Columns Explained:**

| Column | Type | Description | Example Values |
|--------|------|-------------|----------------|
| `id` | UUID | Primary key | `550e8400-e29b-41d4-a716-446655440000` |
| `tenant_id` | VARCHAR(255) | Tenant identifier (Keycloak realm) | `acme-corp`, `tenant-b` |
| `module_id` | VARCHAR(64) | FK to `core_module_catalog` | `delivery-suite`, `helpdesk` |
| `license_state` | VARCHAR(32) | License validation state | `VALID`, `EXPIRED`, `MISSING`, `INVALID`, `TRIAL` |
| `license_payload` | TEXT | Encrypted JWT token | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |
| `license_expires_at` | TIMESTAMP | When license expires | `2025-12-31 23:59:59` |
| `config` | JSONB | Tenant-specific settings | `{"sla_hours": 24, "max_items": 1000}` |
| `enabled` | BOOLEAN | Admin toggle | `true`, `false` |
| `created_at` | TIMESTAMP | When activated | `2025-01-15 10:30:00` |
| `updated_at` | TIMESTAMP | Last modification | `2025-02-20 14:15:00` |
| `updated_by` | VARCHAR(255) | Who modified | `admin@acme-corp.com` |

**License States:**

| State | Description | Module Behavior | UI Indicator |
|-------|-------------|-----------------|--------------|
| `VALID` | License is active and not expired | ✅ Fully functional | 🟢 Green badge "Active" |
| `EXPIRED` | License was valid but expired | ❌ Disabled (403 on API) | 🔴 Red badge "Expired" |
| `MISSING` | No license provided (module requires license) | ❌ Disabled | 🔵 Blue badge "License Required" |
| `INVALID` | License signature/format invalid | ❌ Disabled | 🟠 Orange badge "Invalid License" |
| `TRIAL` | Trial period active (limited time) | ✅ Functional (with warning) | 🟡 Yellow badge "Trial (15 days left)" |

**Config JSONB Examples:**

```json
{
  "delivery-suite": {
    "max_items_per_sprint": 50,
    "default_sprint_duration_weeks": 2,
    "enable_reports": true,
    "notification_channels": ["email", "slack"]
  },
  "helpdesk": {
    "sla_response_hours": 4,
    "sla_resolution_hours": 24,
    "auto_assign": true,
    "escalation_enabled": true
  },
  "crm": {
    "lead_scoring_enabled": true,
    "pipeline_stages": ["prospect", "qualified", "proposal", "closed"],
    "email_sync": true
  }
}
```

---

## 📦 Module Manifest Format (YAML)

### Manifest File Location

**Pattern:** `resources/modules/<module-id>/module.yaml`

**Examples:**
- `resources/modules/delivery-suite/module.yaml`
- `resources/modules/helpdesk/module.yaml`
- `resources/modules/crm-plus/module.yaml`

**External Modules (JAR):**
- JAR must contain `module.yaml` at root or in `META-INF/modules/`
- Spring Boot scans classpath automatically

---

### Manifest Schema (YAML)

```yaml
# Module Identifier (kebab-case, globally unique)
module_id: delivery-suite

# Human-readable name
name: Delivery Suite

# Detailed description
description: Agile work management module with issue tracking, sprints, and kanban boards

# Module type: BUILTIN | EXTENSION | EXTERNAL
type: EXTENSION

# Semantic versioning
version: 1.0.0

# Does this module require a license to activate?
license_required: true

# Module entrypoints (see Entrypoints Structure)
entrypoints:
  fe:
    route: /app/delivery
    permission: MODULE_DELIVERY_ACCESS
    menuLabel: Delivery Board
    icon: kanban
    weight: 100
  api:
    basePath: /api/modules/delivery
    version: v1
  wf:
    definitions:
      - delivery_approval_v1
      - sprint_automation

# Dependencies (other modules or core version)
requires:
  core: ">=1.0.0"
  workflow-engine: ">=2.1.0"

# Provided capabilities (for other modules to depend on)
provides:
  entities:
    - DeliveryItem
    - SprintMetadata
  permissions:
    - MODULE_DELIVERY_ACCESS
    - MODULE_DELIVERY_ADMIN
  api:
    - /api/modules/delivery/items
    - /api/modules/delivery/sprints
```

**Field Descriptions:**

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `module_id` | string | ✅ Yes | Unique identifier (kebab-case) | `delivery-suite` |
| `name` | string | ✅ Yes | Display name | `Delivery Suite` |
| `description` | string | ⚪ Optional | Detailed description | `Agile work management...` |
| `type` | enum | ✅ Yes | `BUILTIN`, `EXTENSION`, `EXTERNAL` | `EXTENSION` |
| `version` | string | ✅ Yes | Semantic version | `1.0.0` |
| `license_required` | boolean | ⚪ Optional (default: `false`) | Needs license? | `true` |
| `entrypoints` | object | ⚪ Optional | Integration points | See [Entrypoints](#entrypoints-structure) |
| `requires` | object | ⚪ Optional | Dependencies | `{ "core": ">=1.0.0" }` |
| `provides` | object | ⚪ Optional | Exported capabilities | `{ "entities": [...] }` |

---

### Manifest Validation Rules

**Validator checks:**

1. ✅ **module_id** must be kebab-case (lowercase, hyphens only)
   - ✅ Valid: `delivery-suite`, `helpdesk-pro`
   - ❌ Invalid: `DeliverySuite`, `delivery_suite`, `delivery suite`

2. ✅ **version** must be semantic (MAJOR.MINOR.PATCH)
   - ✅ Valid: `1.0.0`, `2.3.1`, `1.0.0-beta`
   - ❌ Invalid: `1.0`, `v1.0.0`, `latest`

3. ✅ **type** must be one of: `BUILTIN`, `EXTENSION`, `EXTERNAL`

4. ✅ **requires** versions must use valid comparators
   - ✅ Valid: `>=1.0.0`, `~2.1.0`, `^3.0.0`
   - ❌ Invalid: `1.0`, `latest`, `>1.0.0 <2.0.0` (use `>=1.0.0 <2.0.0`)

5. ✅ **entrypoints.fe.permission** must be valid permission name
   - Format: `MODULE_{MODULE_ID}_{SCOPE}`
   - Example: `MODULE_DELIVERY_ACCESS`, `MODULE_HELPDESK_ADMIN`

6. ✅ **entrypoints.api.basePath** must start with `/api/modules/`
   - ✅ Valid: `/api/modules/delivery`, `/api/modules/helpdesk`
   - ❌ Invalid: `/api/delivery`, `/modules/delivery`

**Validation Errors:**

```java
// Example validation error response
throw new ManifestValidationException(
  "Module 'delivery suite' has invalid module_id: must be kebab-case (e.g., 'delivery-suite')"
);
```

---

## 🚀 Startup Loading Process

### Loading Flow

**When:** Application starts (Spring Boot `@PostConstruct`)

**Trigger:** `ModuleRegistryService.loadModules()` called on startup

**Process:**

```
Application Start
  ↓
1. Scan Classpath
   └─→ Find all: resources/modules/**/*.yaml
   └─→ Find all: META-INF/modules/**/*.yaml (external JARs)
  ↓
2. Parse Manifests
   └─→ YAML parser validates schema
   └─→ Check required fields (module_id, name, version, type)
  ↓
3. Validate Dependencies
   └─→ Check "requires" section
   └─→ Resolve version constraints
   └─→ FAIL if dependency missing or version incompatible
  ↓
4. Update Catalog (Idempotent)
   └─→ For each manifest:
       ├─→ SELECT FROM core_module_catalog WHERE module_id = ?
       ├─→ IF NOT EXISTS → INSERT (new module)
       │   └─→ Log: "Registered new module: delivery-suite v1.0.0"
       ├─→ IF EXISTS AND version changed → UPDATE
       │   └─→ Log: "Updated module: delivery-suite 0.9.0 → 1.0.0"
       └─→ IF EXISTS AND unchanged → SKIP
           └─→ Log: "Module unchanged: helpdesk v2.3.1"
  ↓
5. Log Summary
   └─→ [ModuleRegistry] Loaded 12 modules (3 new, 1 updated, 8 unchanged)
   └─→ [ModuleRegistry] BUILTIN: 5, EXTENSION: 6, EXTERNAL: 1
  ↓
Application Ready
```

---

### Idempotent Update Logic

**SQL pseudocode:**

```sql
-- For each parsed manifest
INSERT INTO core_module_catalog (module_id, name, description, version, type, ...)
VALUES (?, ?, ?, ?, ?, ...)
ON CONFLICT (module_id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  version = EXCLUDED.version,
  type = EXCLUDED.type,
  entrypoints = EXCLUDED.entrypoints,
  updated_at = CURRENT_TIMESTAMP,
  updated_by = 'system';
```

**Java implementation:**

```java
@Service
public class ModuleRegistryService {
  
  @PostConstruct
  public void loadModules() {
    List<ModuleManifest> manifests = scanClasspathForManifests();
    
    int newCount = 0;
    int updatedCount = 0;
    int unchangedCount = 0;
    
    for (ModuleManifest manifest : manifests) {
      Optional<ModuleCatalog> existing = catalogRepo.findById(manifest.getModuleId());
      
      if (existing.isEmpty()) {
        // New module
        catalogRepo.save(manifest.toCatalogEntity());
        log.info("Registered new module: {} v{}", manifest.getModuleId(), manifest.getVersion());
        newCount++;
        
      } else if (!existing.get().getVersion().equals(manifest.getVersion())) {
        // Version changed
        ModuleCatalog updated = existing.get();
        updated.setVersion(manifest.getVersion());
        updated.setEntrypoints(manifest.getEntrypoints());
        updated.setUpdatedAt(Instant.now());
        catalogRepo.save(updated);
        log.info("Updated module: {} {} → {}", manifest.getModuleId(), 
                 existing.get().getVersion(), manifest.getVersion());
        updatedCount++;
        
      } else {
        // Unchanged
        log.debug("Module unchanged: {} v{}", manifest.getModuleId(), manifest.getVersion());
        unchangedCount++;
      }
    }
    
    log.info("[ModuleRegistry] Loaded {} modules ({} new, {} updated, {} unchanged)",
             manifests.size(), newCount, updatedCount, unchangedCount);
  }
}
```

---

### Classpath Scanning Strategy

**1. Spring Resource Scanning:**

```java
@Service
public class ManifestScanner {
  
  private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
  
  public List<Resource> findManifests() throws IOException {
    List<Resource> manifests = new ArrayList<>();
    
    // Scan internal modules (backend/src/main/resources/modules/)
    manifests.addAll(Arrays.asList(
      resourceResolver.getResources("classpath*:modules/**/*.yaml")
    ));
    
    // Scan external JARs (META-INF/modules/)
    manifests.addAll(Arrays.asList(
      resourceResolver.getResources("classpath*:META-INF/modules/**/*.yaml")
    ));
    
    return manifests;
  }
}
```

**2. External Module JAR Structure:**

```
my-custom-module.jar
├── META-INF/
│   └── modules/
│       └── my-module/
│           └── module.yaml
├── com/
│   └── vendor/
│       └── mymodule/
│           ├── MyModuleController.java
│           └── MyModuleService.java
└── application-mymodule.yml
```

**3. Loading Priority:**

1. **BUILTIN modules** (always loaded first)
2. **EXTENSION modules** (official, from backend/resources)
3. **EXTERNAL modules** (third-party JARs)

---

### Dependency Resolution

**Example:**

```yaml
# delivery-suite/module.yaml
module_id: delivery-suite
version: 1.0.0
requires:
  core: ">=1.0.0"
  workflow-engine: ">=2.1.0"
  rbac: "~1.5.0"
```

**Validation logic:**

```java
public void validateDependencies(ModuleManifest manifest) {
  for (Map.Entry<String, String> dep : manifest.getRequires().entrySet()) {
    String depModuleId = dep.getKey();
    String versionConstraint = dep.getValue();
    
    Optional<ModuleCatalog> depModule = catalogRepo.findById(depModuleId);
    
    if (depModule.isEmpty()) {
      throw new MissingDependencyException(
        "Module '" + manifest.getModuleId() + "' requires '" + depModuleId + "', but it is not registered"
      );
    }
    
    if (!satisfiesVersionConstraint(depModule.get().getVersion(), versionConstraint)) {
      throw new IncompatibleVersionException(
        "Module '" + manifest.getModuleId() + "' requires '" + depModuleId + " " + versionConstraint + 
        "', but version " + depModule.get().getVersion() + " is installed"
      );
    }
  }
}
```

**Version Constraint Operators:**

| Operator | Meaning | Example | Matches |
|----------|---------|---------|---------|
| `>=X.Y.Z` | Greater than or equal | `>=1.0.0` | `1.0.0`, `1.2.3`, `2.0.0` |
| `~X.Y.Z` | Tilde (patch updates) | `~1.5.0` | `1.5.0`, `1.5.9` (NOT `1.6.0`) |
| `^X.Y.Z` | Caret (minor updates) | `^1.5.0` | `1.5.0`, `1.9.0` (NOT `2.0.0`) |
| `X.Y.Z` | Exact version | `1.5.0` | `1.5.0` only |

---

## 🔧 Custom Module Development

### External Module Setup

**1. Create Maven/Gradle Project:**

```xml
<!-- pom.xml -->
<project>
  <groupId>com.vendor</groupId>
  <artifactId>my-custom-module</artifactId>
  <version>1.0.0</version>
  
  <dependencies>
    <!-- CORE Platform Framework -->
    <dependency>
      <groupId>cz.muriel.core</groupId>
      <artifactId>core-framework</artifactId>
      <version>1.0.0</version>
      <scope>provided</scope>  <!-- Provided by runtime -->
    </dependency>
  </dependencies>
</project>
```

**2. Create Module Manifest:**

```yaml
# src/main/resources/META-INF/modules/my-custom-module/module.yaml
module_id: my-custom-module
name: My Custom Module
version: 1.0.0
type: EXTERNAL
license_required: true

entrypoints:
  api:
    basePath: /api/modules/my-custom
  fe:
    route: /app/my-custom
    permission: MODULE_MY_CUSTOM_ACCESS

requires:
  core: ">=1.0.0"

provides:
  entities:
    - MyCustomEntity
```

**3. Implement SPI (Optional):**

```java
// src/main/java/com/vendor/mymodule/MyModuleProvider.java
package com.vendor.mymodule;

import cz.muriel.core.module.spi.ModuleProvider;
import org.springframework.stereotype.Component;

@Component
public class MyModuleProvider implements ModuleProvider {
  
  @Override
  public String getModuleId() {
    return "my-custom-module";
  }
  
  @Override
  public void onActivate(String tenantId) {
    // Called when tenant activates this module
    System.out.println("Module activated for tenant: " + tenantId);
  }
  
  @Override
  public void onDeactivate(String tenantId) {
    // Called when tenant deactivates
    System.out.println("Module deactivated for tenant: " + tenantId);
  }
}
```

**4. Package as JAR:**

```bash
mvn clean package
# Produces: target/my-custom-module-1.0.0.jar
```

**5. Deploy to Core Platform:**

**Option A: Classpath (Development)**
```bash
# Add JAR to backend classpath
cp my-custom-module-1.0.0.jar backend/lib/
# Restart application
make clean-fast
```

**Option B: Docker Volume (Production)**
```yaml
# docker-compose.yml
backend:
  volumes:
    - ./custom-modules:/app/modules  # Mount external modules
```

```bash
# Copy JAR to mounted directory
cp my-custom-module-1.0.0.jar custom-modules/
# Module auto-loaded on restart
docker restart core-backend
```

---

## 🛠️ Troubleshooting

### Common Issues

**1. Manifest Not Found**

**Error:**
```
[ERROR] ModuleRegistry: No manifests found in classpath
```

**Solution:**
- Check file location: `resources/modules/<module-id>/module.yaml`
- Verify build includes YAML files: `mvn resources:resources`
- For external JARs: ensure `META-INF/modules/` structure

---

**2. Invalid module_id**

**Error:**
```
ManifestValidationException: Module 'delivery suite' has invalid module_id: must be kebab-case
```

**Solution:**
```yaml
# ❌ WRONG
module_id: delivery suite

# ✅ CORRECT
module_id: delivery-suite
```

---

**3. Missing Dependency**

**Error:**
```
MissingDependencyException: Module 'delivery-suite' requires 'workflow-engine', but it is not registered
```

**Solution:**
- Ensure dependency module's manifest is loaded first
- Check module loading order (BUILTIN → EXTENSION → EXTERNAL)
- Verify dependency module_id spelling

---

**4. Version Incompatibility**

**Error:**
```
IncompatibleVersionException: Module 'delivery-suite' requires 'core >=2.0.0', but version 1.5.0 is installed
```

**Solution:**
- Upgrade CORE Platform to compatible version
- OR update module's `requires` constraint
- Check version constraint syntax (e.g., `>=2.0.0` not `>= 2.0.0`)

---

**5. Duplicate module_id**

**Error:**
```
DuplicateModuleException: Module 'delivery-suite' already registered (version 1.0.0)
```

**Solution:**
- Check for duplicate manifests in classpath
- Verify no conflicting external JARs loaded
- Review `resources/modules/` directory for duplicates

---

## 📊 Monitoring & Observability

### Startup Logs

```
[INFO ] ModuleRegistry: Scanning classpath for module manifests...
[INFO ] ModuleRegistry: Found 12 manifests (8 internal, 4 external)
[INFO ] ModuleRegistry: Registered new module: delivery-suite v1.0.0
[INFO ] ModuleRegistry: Updated module: helpdesk 2.3.0 → 2.3.1
[DEBUG] ModuleRegistry: Module unchanged: rbac v1.5.2
[INFO ] ModuleRegistry: Loaded 12 modules (3 new, 1 updated, 8 unchanged)
[INFO ] ModuleRegistry: BUILTIN: 5, EXTENSION: 6, EXTERNAL: 1
[INFO ] ModuleRegistry: Dependency validation complete (0 errors)
```

### Metrics (Prometheus)

```java
@Component
public class ModuleMetrics {
  
  @Gauge(name = "modules_registered_total", description = "Total modules in catalog")
  public int getRegisteredModulesCount() {
    return catalogRepo.count();
  }
  
  @Gauge(name = "modules_active_total", description = "Active modules")
  public int getActiveModulesCount() {
    return catalogRepo.countByStatus("ACTIVE");
  }
  
  @Gauge(name = "tenant_modules_enabled", description = "Enabled modules per tenant", labelNames = {"tenant_id"})
  public int getEnabledModulesForTenant(String tenantId) {
    return tenantModuleRepo.countByTenantAndEnabled(tenantId, true);
  }
}
```

### Audit Log

**Table:** `core_module_audit_log`

```sql
CREATE TABLE core_module_audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  event_type VARCHAR(64),  -- MODULE_LOADED, MODULE_UPDATED, MODULE_ACTIVATED, etc.
  module_id VARCHAR(64),
  tenant_id VARCHAR(255),
  old_value JSONB,
  new_value JSONB,
  user_id VARCHAR(255)
);
```

**Example events:**
```json
{
  "event_type": "MODULE_LOADED",
  "module_id": "delivery-suite",
  "new_value": {"version": "1.0.0", "status": "ACTIVE"}
}

{
  "event_type": "MODULE_ACTIVATED",
  "module_id": "delivery-suite",
  "tenant_id": "acme-corp",
  "user_id": "admin@acme-corp.com"
}
```

---

## 📚 References

- **Licensing:** [MODULE_LICENSING.md](MODULE_LICENSING.md)
- **Workflow Engine:** EPIC-006 (workflow definitions integration)
- **RBAC:** EPIC-001 (permission scopes)
- **Metamodel:** EPIC-002 (entity definitions)

---

**Last Updated:** 2025-10-26  
**Version:** 1.0.0  
**Status:** 🔮 PLANNED
