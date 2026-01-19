# T2: Geographic Maps

**Story:** [S11: Advanced Visualization](README.md)  
**Effort:** 7 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Pridat mapovou vizualizaci (choropleth) pro geograficka data.

---

## 🏗️ IMPLEMENTATION

```typescript
<ChoroplethMap data={rows} dimension="country" metric="Revenue.total" />
```

---

## ✅ Acceptance Criteria

- [ ] Mapa zobrazuje data podle zeme/regionu.
- [ ] Tooltips zobrazuji hodnotu + jednotku.
- [ ] Zoom/pan funguje na desktop i mobile.

---

## ✅ DELIVERABLES

- [ ] Map component + theming
- [ ] Geocoding/lookup mapping
- [ ] UI konfigurace pro map widget

---

**Estimated:** 7 hours
