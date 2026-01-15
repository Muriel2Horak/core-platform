# T3: API Response Standardization

**Story:** [S8: Frontend-Backend Integration](README.md)  
**Effort:** 4 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Sjednotit format odpovedi pro reporting endpointy a sjednotit error payload.

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/reporting/dto/StandardQueryResponse.java
public class StandardQueryResponse<T> {
  private List<T> results;
  private QueryMetadata metadata;
  private PaginationInfo pagination;
}
```

---

## ✅ Acceptance Criteria

- [ ] Vsechny reporting endpointy vraci `results` + `metadata`.
- [ ] Error payload ma jednotny format `{ code, message, details }`.
- [ ] Frontend pouziva jednotny parser pro query odpovedi.
- [ ] Nepouzite endpointy jsou odstranene nebo zdokumentovane.

---

## ✅ DELIVERABLES

- [ ] StandardQueryResponse DTO + mapper
- [ ] Uprava controlleru pro jednotny format
- [ ] Aktualizace FE parseru + fallback handling

---

**Estimated:** 4 hours
