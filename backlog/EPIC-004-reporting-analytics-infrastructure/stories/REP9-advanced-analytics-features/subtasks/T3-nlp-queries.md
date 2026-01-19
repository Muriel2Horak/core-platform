# T3: NLP Queries

**Story:** [S9: Advanced Analytics](README.md)  
**Effort:** 9 hours  
**Priority:** P2  
**Dependencies:** EPIC-009, EPIC-010

---

## 📋 OBJECTIVE

Umoznit uzivateli zadat dotaz v prirozenem jazyce a prelozit jej do Cube.js query.

---

## 🏗️ IMPLEMENTATION

```text
Input: "show top 10 customers by revenue last month"
Output: Cube.js query JSON + human-readable preview
```

---

## ✅ Acceptance Criteria

- [ ] Podporovane jsou top use cases (top N, trend, agregace).
- [ ] UI zobrazi prelozeny query preview pred spustenim.
- [ ] Nezname dotazy vraci jasny fallback a navod.

---

## ✅ DELIVERABLES

- [ ] NLP query endpoint + parser
- [ ] UI input + preview komponenta
- [ ] Telemetrie pro uspesnost dotazu

---

**Estimated:** 9 hours
