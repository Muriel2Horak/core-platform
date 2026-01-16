---
id: META-020
epic: EPIC-005-metamodel-generator-studio
title: "Search & Filter DSL"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "80 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META20-search-filter-dsl/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-020: Search & Filter DSL

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~1,000 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Advanced query builder, full-text search, faceted search, filter UI components.

---

## 🎯 Acceptance Criteria

### AC1: Advanced Query DSL

```
?filter=price>100 AND (category='electronics' OR category='books')
&search=laptop
&facets=brand,price_range
```

### AC2: Full-Text Search (PostgreSQL tsvector)

```sql
SELECT *, ts_rank(to_tsvector('english', name || ' ' || description), to_tsquery('english', 'laptop & dell')) AS rank
FROM products
WHERE to_tsvector('english', name || ' ' || description) @@ to_tsquery('english', 'laptop & dell')
ORDER BY rank DESC;
```

### AC3: Faceted Search

Response:
```json
{
  "data": [...],
  "facets": {
    "brand": [
      {"value": "Dell", "count": 42},
      {"value": "HP", "count": 38}
    ],
    "price_range": [
      {"value": "0-500", "count": 120},
      {"value": "500-1000", "count": 85}
    ]
  }
}
```

### AC4: Filter UI Components
- **GIVEN** metamodel pole `filterable/searchable`
- **THEN** UI umi vygenerovat filter builder + ulozit query preset

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | DSL grammar + parser (AND/OR, nesting) | 25h | META-001 |
| 2 | Query builder + full-text search integration | 25h | T1 |
| 3 | Facet aggregation + response schema | 15h | T2 |
| 4 | UI filter builder + presets | 10h | T1, META-009 |
| 5 | Testy + docs (examples) | 5h | T1, T2, T3 |

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
