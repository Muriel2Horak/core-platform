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

---

**Story Owner:** Backend Team  
**Priority:** P3  
**Effort:** 2 týdny
