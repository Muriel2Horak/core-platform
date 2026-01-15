---
id: MOD-003
epic: EPIC-017-modular-architecture
title: "Entity Extension API"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "3 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/MOD-003-entity-extension-api/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---

# MOD-003: Entity Extension API

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** Metamodel Engine, MOD-001  
**Category:** Module System

---

## 📖 User Story

**As a module**,  
I want to extend existing entities with new fields and relationships,  
So that I can add functionality without modifying core entities.

---

## 🎯 Acceptance Criteria

- ⏳ Module can add fields to existing entity (e.g., `User.department`)
- ⏳ Module can add relationships (e.g., `User.projects`)
- ⏳ Namespace collision prevented (`ivg.User.customField` vs `core.User.name`)
- ⏳ Extensions isolated per module (uninstall removes only module's fields)
- ⏳ Schema migrations generated automatically for extensions
- ⏳ UI metadata updated (new fields appear in forms/lists)

---

## 💻 Implementation

### Extension Manifest

```json
{
  "id": "project-management",
  "provides": {
    "entityExtensions": [
      {
        "entity": "core.User",
        "namespace": "pm",
        "addFields": [
          {
            "name": "defaultProject",
            "type": "reference",
            "targetEntity": "pm.Project"
          },
          {
            "name": "hourlyRate",
            "type": "decimal"
          }
        ],
        "addRelationships": [
          {
            "name": "projects",
            "type": "manyToMany",
            "targetEntity": "pm.Project"
          }
        ]
      }
    ]
  }
}
```

### Database Schema

```sql
-- Extension fields stored in separate table
CREATE TABLE entity_extensions (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(200) NOT NULL,     -- "core.User"
    module_id VARCHAR(100) NOT NULL,        -- "project-management"
    namespace VARCHAR(50) NOT NULL,         -- "pm"
    field_name VARCHAR(100) NOT NULL,       -- "defaultProject"
    field_type VARCHAR(50) NOT NULL,        -- "reference"
    field_config JSONB,                     -- {"targetEntity": "pm.Project"}
    
    UNIQUE(entity_name, module_id, field_name)
);

-- Actual data stored in JSON column
ALTER TABLE users ADD COLUMN extensions JSONB DEFAULT '{}';

-- Index for fast lookups
CREATE INDEX idx_user_extensions ON users USING gin(extensions);
```

---

## 📊 Success Metrics

- Extension registration: <100ms
- Query performance: No degradation with extensions
- Collision detection: 100%

---

**Last Updated:** 9. listopadu 2025
