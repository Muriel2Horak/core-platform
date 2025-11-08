# S6: Visual Query Builder

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🟡 **P1** | **Effort:** ~45h | **Sprint:** 5

---

## 📋 USER STORY

**Jako** Analyst, **chci** visual query builder (drag & drop dimensions/measures), **abych** mohl vytvářet custom queries bez psaní kódu.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Cube.js Introspection**: Načte dostupné dimensions/measures z Cube.js schema
2. **Drag & Drop Query**: Přetáhnu `tenantId` → Rows, `count` → Values → vytvoří pivot
3. **Filter Builder UI**: Vizuální editor pro filters (`status = 'ACTIVE'`)
4. **Preview Results**: Real-time preview query výsledků

---

## 🏗️ TASK BREAKDOWN (~45h)

### T1: Schema Introspection (10h)
- GET /api/cube/schema → parse dimensions/measures
- Display in tree view

### T2: Query Builder UI (20h)
- Drag & drop dimensions → Rows/Columns
- Drag measures → Values
- Generate Cube.js query JSON

### T3: Filter Builder (10h)
- Visual filter editor (dropdowns, inputs)
- Support: equals, contains, range, in-list

### T4: Testing (5h)

---

## 📦 DEPENDENCIES

- **EPIC-004 S1**: Cube.js schemas ✅

---

## 📊 SUCCESS METRICS

- Query build < 2min (vs. 10min SQL writing)
- 90% queries buildable visually

