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

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
