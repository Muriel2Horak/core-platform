# MOD-001: Module Manifest Loader

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH (foundation pro celý modulární systém)  
**Dependencies:** Metamodel Engine  
**Category:** Module System

---

## 📖 User Story

**As a platform developer**,  
I want the system to load and parse module manifests from JSON files,  
So that modules can declare their entities, workflows, UI, and dependencies without code changes.

---

## 🎯 Acceptance Criteria

- ⏳ System scans `/modules/` directory for `manifest.json` files at startup
- ⏳ Manifest is parsed and validated against JSON Schema
- ⏳ Module metadata stored in `module_registry` table
- ⏳ Invalid manifests logged (errors) but don't crash startup
- ⏳ Hot-reload: Admin can trigger manifest re-scan via API (without restart)
- ⏳ Version conflicts detected (e.g., `requires: core>=2.0` but core is 1.5)

---

## 📐 Manifest Schema (JSON Schema)

**File:** `schemas/module-manifest-schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "name", "version", "provides"],
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^[a-z0-9-]+$",
      "description": "Unique module identifier (kebab-case)"
    },
    "name": {
      "type": "string",
      "description": "Human-readable module name"
    },
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "Semver version (e.g., 1.2.3)"
    },
    "vendor": {
      "type": "string",
      "description": "Module vendor/author"
    },
    "license": {
      "type": "string",
      "enum": ["MIT", "Apache-2.0", "Proprietary"],
      "description": "License type"
    },
    "requires": {
      "type": "object",
      "description": "Dependencies on other modules",
      "patternProperties": {
        "^[a-z0-9-]+$": {
          "type": "string",
          "description": "Version constraint (semver range)"
        }
      }
    },
    "provides": {
      "type": "object",
      "description": "What this module provides",
      "properties": {
        "entities": {
          "type": "array",
          "items": { "$ref": "#/definitions/entityDefinition" }
        },
        "workflows": {
          "type": "array",
          "items": { "$ref": "#/definitions/workflowDefinition" }
        },
        "ui": { "$ref": "#/definitions/uiDefinition" },
        "roles": {
          "type": "array",
          "items": { "$ref": "#/definitions/roleDefinition" }
        },
        "connectors": {
          "type": "array",
          "items": { "$ref": "#/definitions/connectorDefinition" }
        }
      }
    },
    "migrations": {
      "type": "array",
      "description": "Database migrations",
      "items": {
        "type": "object",
        "required": ["version", "file"],
        "properties": {
          "version": { "type": "string" },
          "file": { "type": "string" }
        }
      }
    }
  },
  "definitions": {
    "entityDefinition": {
      "type": "object",
      "required": ["name", "namespace"],
      "properties": {
        "name": { "type": "string" },
        "namespace": { "type": "string" },
        "attributes": {
          "type": "array",
          "items": { "type": "string" }
        },
        "relationships": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["entity", "type", "name"],
            "properties": {
              "entity": { "type": "string" },
              "type": { "enum": ["oneToOne", "oneToMany", "manyToOne", "manyToMany"] },
              "name": { "type": "string" }
            }
          }
        }
      }
    },
    "workflowDefinition": {
      "type": "object",
      "required": ["name", "states"],
      "properties": {
        "name": { "type": "string" },
        "states": {
          "type": "array",
          "items": { "type": "string" }
        },
        "transitions": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["from", "to", "action"],
            "properties": {
              "from": { "type": "string" },
              "to": { "type": "string" },
              "action": { "type": "string" }
            }
          }
        }
      }
    },
    "uiDefinition": {
      "type": "object",
      "properties": {
        "menu": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["label", "route"],
            "properties": {
              "label": { "type": "string" },
              "route": { "type": "string" },
              "icon": { "type": "string" },
              "roles": {
                "type": "array",
                "items": { "type": "string" }
              }
            }
          }
        },
        "views": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["entity", "type", "spec"],
            "properties": {
              "entity": { "type": "string" },
              "type": { "enum": ["list", "detail", "form", "dashboard"] },
              "spec": { "type": "string" }
            }
          }
        }
      }
    },
    "roleDefinition": {
      "type": "object",
      "required": ["name", "scopes"],
      "properties": {
        "name": { "type": "string" },
        "scopes": {
          "type": "array",
          "items": { "type": "string" }
        }
      }
    },
    "connectorDefinition": {
      "type": "object",
      "required": ["type"],
      "properties": {
        "type": { "type": "string" },
        "config": { "type": "string" }
      }
    }
  }
}
```

---

## 💻 Implementation

### Database Schema

**Table:** `module_registry`

```sql
CREATE TABLE module_registry (
    id BIGSERIAL PRIMARY KEY,
    module_id VARCHAR(100) UNIQUE NOT NULL,  -- e.g., "project-management"
    name VARCHAR(200) NOT NULL,
    version VARCHAR(20) NOT NULL,            -- e.g., "1.2.0"
    vendor VARCHAR(100),
    license VARCHAR(50),                     -- "MIT", "Proprietary"
    manifest_json JSONB NOT NULL,            -- Full manifest
    status VARCHAR(20) NOT NULL,             -- "available", "installed", "enabled", "disabled"
    installed_at TIMESTAMP,
    enabled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- Constraints
    CHECK (status IN ('available', 'installing', 'installed', 'enabled', 'disabled', 'error'))
);

CREATE INDEX idx_module_status ON module_registry(status);
CREATE INDEX idx_module_id_version ON module_registry(module_id, version);
```

**Table:** `module_dependencies`

```sql
CREATE TABLE module_dependencies (
    id BIGSERIAL PRIMARY KEY,
    module_id VARCHAR(100) NOT NULL,         -- "project-management"
    depends_on_module VARCHAR(100) NOT NULL, -- "task-management"
    version_constraint VARCHAR(50) NOT NULL, -- ">=1.2.0"
    
    FOREIGN KEY (module_id) REFERENCES module_registry(module_id) ON DELETE CASCADE
);

CREATE INDEX idx_module_deps ON module_dependencies(module_id);
```

---

### Backend Implementation

**Service:** `ModuleManifestLoader.java`

```java
package cz.muriel.core.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ModuleManifestLoader {
    
    private static final Logger log = LoggerFactory.getLogger(ModuleManifestLoader.class);
    
    private final ObjectMapper objectMapper;
    private final JsonSchema manifestSchema;
    private final ModuleRegistry moduleRegistry;
    
    private final Path modulesDirectory = Paths.get("modules");
    
    public ModuleManifestLoader(
        ObjectMapper objectMapper,
        ModuleRegistry moduleRegistry
    ) throws IOException {
        this.objectMapper = objectMapper;
        this.moduleRegistry = moduleRegistry;
        
        // Load JSON Schema for manifest validation
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        JsonNode schemaNode = objectMapper.readTree(
            getClass().getResourceAsStream("/schemas/module-manifest-schema.json")
        );
        this.manifestSchema = factory.getSchema(schemaNode);
    }
    
    /**
     * Scan modules directory and load all manifests.
     * Called at application startup.
     */
    public void loadAllManifests() {
        log.info("Scanning modules directory: {}", modulesDirectory);
        
        if (!Files.exists(modulesDirectory)) {
            log.warn("Modules directory does not exist: {}", modulesDirectory);
            return;
        }
        
        try (Stream<Path> paths = Files.walk(modulesDirectory, 2)) {
            paths
                .filter(path -> path.getFileName().toString().equals("manifest.json"))
                .forEach(this::loadManifest);
        } catch (IOException e) {
            log.error("Failed to scan modules directory", e);
        }
        
        log.info("Manifest loading complete. Loaded {} modules", 
                 moduleRegistry.getModuleCount());
    }
    
    /**
     * Load single manifest from file.
     */
    private void loadManifest(Path manifestPath) {
        try {
            log.debug("Loading manifest: {}", manifestPath);
            
            // Parse JSON
            JsonNode manifestNode = objectMapper.readTree(manifestPath.toFile());
            
            // Validate against schema
            Set<ValidationMessage> errors = manifestSchema.validate(manifestNode);
            if (!errors.isEmpty()) {
                log.error("Manifest validation failed: {}", manifestPath);
                errors.forEach(error -> log.error("  - {}", error.getMessage()));
                return;
            }
            
            // Convert to POJO
            ModuleManifest manifest = objectMapper.treeToValue(manifestNode, ModuleManifest.class);
            
            // Validate dependencies
            if (!validateDependencies(manifest)) {
                log.error("Dependency validation failed for module: {}", manifest.getId());
                return;
            }
            
            // Register in database
            moduleRegistry.registerManifest(manifest);
            
            log.info("✅ Loaded manifest: {} v{}", manifest.getId(), manifest.getVersion());
            
        } catch (IOException e) {
            log.error("Failed to load manifest: {}", manifestPath, e);
        }
    }
    
    /**
     * Validate module dependencies (version constraints).
     */
    private boolean validateDependencies(ModuleManifest manifest) {
        if (manifest.getRequires() == null) {
            return true; // No dependencies
        }
        
        for (Map.Entry<String, String> dep : manifest.getRequires().entrySet()) {
            String depModuleId = dep.getKey();
            String versionConstraint = dep.getValue();
            
            // Check if dependency exists
            Optional<ModuleInfo> depModule = moduleRegistry.getModule(depModuleId);
            if (depModule.isEmpty()) {
                log.error("Dependency not found: {} (required by {})", 
                         depModuleId, manifest.getId());
                return false;
            }
            
            // Check version constraint
            if (!satisfiesVersionConstraint(depModule.get().getVersion(), versionConstraint)) {
                log.error("Version constraint not satisfied: {} requires {} {}, but found version {}", 
                         manifest.getId(), depModuleId, versionConstraint, depModule.get().getVersion());
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if version satisfies constraint (semver).
     * Example: version="1.2.0", constraint=">=1.0.0" → true
     */
    private boolean satisfiesVersionConstraint(String version, String constraint) {
        // Simple implementation (use semver4j library in production)
        if (constraint.startsWith(">=")) {
            String minVersion = constraint.substring(2);
            return compareVersions(version, minVersion) >= 0;
        } else if (constraint.startsWith(">")) {
            String minVersion = constraint.substring(1);
            return compareVersions(version, minVersion) > 0;
        } else if (constraint.startsWith("=")) {
            String exactVersion = constraint.substring(1);
            return version.equals(exactVersion);
        }
        
        // Default: exact match
        return version.equals(constraint);
    }
    
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        
        return 0;
    }
}
```

**POJO:** `ModuleManifest.java`

```java
package cz.muriel.core.modules;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModuleManifest {
    private String id;
    private String name;
    private String version;
    private String vendor;
    private String license;
    
    private Map<String, String> requires; // moduleId → version constraint
    
    private Provides provides;
    
    private List<Migration> migrations;
    
    @Data
    public static class Provides {
        private List<EntityDefinition> entities;
        private List<WorkflowDefinition> workflows;
        private UiDefinition ui;
        private List<RoleDefinition> roles;
        private List<ConnectorDefinition> connectors;
    }
    
    @Data
    public static class EntityDefinition {
        private String name;
        private String namespace;
        private List<String> attributes;
        private List<RelationshipDefinition> relationships;
    }
    
    @Data
    public static class RelationshipDefinition {
        private String entity;
        private String type; // oneToOne, oneToMany, manyToOne, manyToMany
        private String name;
    }
    
    @Data
    public static class WorkflowDefinition {
        private String name;
        private List<String> states;
        private List<TransitionDefinition> transitions;
    }
    
    @Data
    public static class TransitionDefinition {
        private String from;
        private String to;
        private String action;
    }
    
    @Data
    public static class UiDefinition {
        private List<MenuItem> menu;
        private List<ViewDefinition> views;
    }
    
    @Data
    public static class MenuItem {
        private String label;
        private String route;
        private String icon;
        private List<String> roles;
    }
    
    @Data
    public static class ViewDefinition {
        private String entity;
        private String type; // list, detail, form, dashboard
        private String spec;
    }
    
    @Data
    public static class RoleDefinition {
        private String name;
        private List<String> scopes;
    }
    
    @Data
    public static class ConnectorDefinition {
        private String type;
        private String config;
    }
    
    @Data
    public static class Migration {
        private String version;
        private String file;
    }
}
```

---

## 🧪 Testing

### Unit Test

```java
@SpringBootTest
class ModuleManifestLoaderTest {
    
    @Autowired
    private ModuleManifestLoader loader;
    
    @Autowired
    private ModuleRegistry registry;
    
    @Test
    void shouldLoadValidManifest() throws IOException {
        // Given: Valid manifest file
        Path testManifest = Paths.get("src/test/resources/manifests/project-management-manifest.json");
        
        // When: Load manifest
        loader.loadAllManifests();
        
        // Then: Module registered
        Optional<ModuleInfo> module = registry.getModule("project-management");
        assertThat(module).isPresent();
        assertThat(module.get().getVersion()).isEqualTo("1.2.0");
        assertThat(module.get().getStatus()).isEqualTo(ModuleStatus.AVAILABLE);
    }
    
    @Test
    void shouldRejectInvalidManifest() {
        // Given: Manifest without required "id" field
        String invalidJson = """
        {
          "name": "Invalid Module",
          "version": "1.0.0"
        }
        """;
        
        // When/Then: Validation should fail
        // (logged error, module not registered)
    }
    
    @Test
    void shouldDetectVersionConflict() {
        // Given: Module requires "core>=2.0.0" but core is "1.5.0"
        // When: Load manifest
        // Then: Dependency validation fails, module not loaded
    }
}
```

---

## 📊 Success Metrics

- **Manifest load time:** <100ms per manifest
- **Validation errors:** 100% manifests validated before registration
- **Dependency resolution:** 100% conflicts detected
- **Hot-reload time:** <500ms for re-scan

---

## 🚀 Rollout Plan

1. **Schema definition** (0.5 day)
   - Create JSON Schema for manifest
   - Document manifest structure

2. **Database schema** (0.5 day)
   - Create `module_registry` table
   - Create `module_dependencies` table

3. **Loader implementation** (1 day)
   - Implement `ModuleManifestLoader`
   - JSON parsing + validation
   - Dependency resolution

4. **Testing** (0.5 day)
   - Unit tests for loader
   - Integration tests with sample manifests

5. **Documentation** (0.5 day)
   - Manifest creation guide
   - Example manifests

**Total:** 3 days

---

## 📚 References

- **JSON Schema:** https://json-schema.org/
- **Semver:** https://semver.org/
- **EPIC-017:** Modular Architecture & Licensing
- **MOD-002:** Module Registry (next story)

---

**Last Updated:** 9. listopadu 2025  
**Status:** ⏳ PENDING  
**Next Action:** Define JSON Schema + create sample manifests
