---
id: META-019
epic: EPIC-005-metamodel-generator-studio
title: "Versioning & Migrations"
priority: P3
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "80 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META19-versioning-migrations/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---


# META-019: Versioning & Migrations

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟢 **NICE TO HAVE**  
**Priorita:** P3 (Low)  
**Estimated LOC:** ~800 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Entity version history, automatic Flyway migration generation, schema rollback support.

---

## 🎯 Acceptance Criteria

### AC1: Version History

```yaml
entity: Product
version: 2
history:
  - version: 1
    date: 2025-01-01
    changes:
      - added field: description
  - version: 2
    date: 2025-02-01
    changes:
      - added field: category_id
```

### AC2: Flyway Generation

- **GIVEN** schema change (add column)
- **THEN** generuje Flyway migration:

```sql
-- V2__add_product_category.sql
ALTER TABLE products ADD COLUMN category_id BIGINT;
CREATE INDEX idx_products_category ON products(category_id);
```

### AC3: Rollback Support
- **GIVEN** posledni migration selze
- **THEN** system umi vratit schema na predchozi verzi (down script)

### AC4: Version Metadata
- **GIVEN** publikace nove verze metamodelu
- **THEN** ulozi se metadata (author, timestamp, diff summary)

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Version storage + diff metadata | 20h | META-001 |
| 2 | Flyway migration generator (up/down) | 30h | T1 |
| 3 | Rollback strategy + validation | 20h | T2 |
| 4 | Testy + docs (migration lifecycle) | 10h | T2, T3 |

---

**Story Owner:** Backend Team  
**Priority:** P3  
**Effort:** 2 týdny
