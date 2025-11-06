---
id: SUBTASK-XXX
story: CORE-XXX
title: "Subtask Short Title"
status: todo  # todo | in-progress | done
assignee: ""
estimate: "X hours"
created: YYYY-MM-DD
updated: YYYY-MM-DD
---

# SUBTASK-XXX: [Subtask Title]

> **Parent Story:** [CORE-XXX: Story Name](../README.md)  
> **Status:** [todo/in-progress/done] | **Estimate:** X hours

## 🎯 Subtask Goal

[Stručný popis co má tento subtask dosáhnout - 1-2 věty]

**Příklad:**
Implementovat backend API endpoint pro export dat včetně validace vstupů a error handlingu.

---

## ✅ Acceptance Criteria

Tento subtask je hotový pokud:

- [ ] [Konkrétní deliverable #1, např. "Service class implementována s metodou exportData()"]
- [ ] [Konkrétní deliverable #2, např. "Controller endpoint /api/export vrací 200 pro validní request"]
- [ ] [Konkrétní deliverable #3, např. "Error handling pokrývá 3 error cases: invalid input, permission denied, server error"]
- [ ] [Testovací kritérium, např. "Unit testy pro Service mají >80% coverage"]

---

## 📂 Files to Modify/Create

### Create New Files
```
- path/to/new/File1.java
- path/to/new/File2.tsx
- path/to/new/Test.spec.ts
```

### Modify Existing Files
```
- path/to/existing/Config.java (add new property)
- path/to/existing/Router.tsx (add new route)
```

---

## 🔧 Implementation Steps

### Step 1: Setup
- [ ] Vytvořit Git branch (pokud ještě neexistuje): `feature/CORE-XXX-name`
- [ ] Vytvořit boilerplate soubory z "Files to Create"
- [ ] Import dependencies (Maven, npm)

### Step 2: Core Implementation
- [ ] [Konkrétní krok #1, např. "Implementovat ExportService.exportData() metodu"]
  ```java
  // Příklad kódu nebo pseudokód
  public ExportResult exportData(ExportRequest request) {
    // 1. Validate input
    // 2. Fetch data from repository
    // 3. Transform to export format
    // 4. Return result
  }
  ```

- [ ] [Konkrétní krok #2, např. "Přidat Controller endpoint"]
  ```java
  @PostMapping("/api/export")
  public ResponseEntity<ExportResult> export(@RequestBody ExportRequest request) {
    return ResponseEntity.ok(exportService.exportData(request));
  }
  ```

- [ ] [Konkrétní krok #3, např. "Implementovat error handling"]

### Step 3: Integration
- [ ] Připojit novou funkcionalitu k existujícímu kódu
- [ ] Aktualizovat konfigurace (pokud nutné)
- [ ] Verify kompilace (no errors/warnings)

### Step 4: Testing
- [ ] Napsat unit testy pro novou funkcionalitu
  ```bash
  # Test files:
  - path/to/ServiceTest.java
  - path/to/ControllerTest.java
  ```

- [ ] Spustit testy: `make test-backend` nebo `npm test`
- [ ] Verify coverage >80%

### Step 5: Documentation
- [ ] Přidat inline code comments (complex logic)
- [ ] Aktualizovat API docs (pokud public API)
- [ ] Aktualizovat README (pokud user-facing)

---

## ✅ Testing Checklist

### Unit Tests
- [ ] Happy path test (základní funkcionalita funguje)
- [ ] Edge cases (boundary conditions, null values, empty lists)
- [ ] Error cases (invalid input, exceptions)
- [ ] Mock dependencies properly (DB, external APIs)

### Integration Tests (pokud applicable)
- [ ] Test komunikaci mezi komponentami
- [ ] Verify DB interactions
- [ ] Test API endpoints end-to-end

### Manual Testing
- [ ] Spustit aplikaci lokálně: `make dev-up`
- [ ] Otestovat funkcionalitu v browseru/Postman
- [ ] Verify UI vypadá správně (pokud frontend)
- [ ] Zkontrolovat logy (žádné errors/warnings)

---

## 📖 References

### Related Subtasks
- [ ] SUBTASK-YYY: [Related subtask] - How it's related

### Documentation
- [Parent Story](../README.md) - Celkový kontext
- [API Spec](../attachments/api-spec.yaml) - API design
- [UI Mockup](../attachments/mockup.png) - UI design

### Code References
```
// Podobná implementace:
- backend/src/main/java/cz/muriel/core/other/SimilarService.java

// Patterns použité:
- Repository pattern: see GroupRepository.java
- DTO mapping: see GroupMapper.java
```

### External Links
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [React Docs](https://react.dev/)
- [Testing Best Practices](https://martinfowler.com/testing/)

---

## 🐛 Known Issues / Blockers

### Blockers
- [ ] [Blokující issue, např. "Čekáme na SUBTASK-YYY - potřebujeme DB migration"]
- [ ] [External dependency, např. "Backend API endpoint ještě není ready"]

### Tech Debt / Follow-ups
- [ ] [Věc k vylepšení later, např. "TODO: Add caching layer (CORE-ZZZ)"]
- [ ] [Performance optimization, např. "TODO: Optimize query performance"]

---

## 📝 Implementation Notes

### Decisions Made
[Technické rozhodnutí během implementace]

**Příklad:**
- Použili jsme `CompletableFuture` pro async processing místo `@Async` - lepší error handling
- Frontend používá `react-query` pro caching - konzistentní s ostatními features

### Challenges Encountered
[Problémy na které jsme narazili a jak jsme je vyřešili]

**Příklad:**
- Problem: PostgreSQL connection timeout při velkých exportech
- Solution: Přidali jsme batch processing (500 records per batch)

### Time Tracking
- **Estimated:** X hours
- **Actual:** Y hours
- **Variance:** +/- Z hours (explain if significant)

---

## ✅ Definition of Done (Subtask)

- [ ] Všechny "Files to Create/Modify" jsou hotové
- [ ] Všechny "Implementation Steps" jsou complete
- [ ] Všechny "Acceptance Criteria" jsou splněná
- [ ] Testing Checklist je 100% done
- [ ] Unit tests pass (coverage >80%)
- [ ] Code compiles bez warnings
- [ ] Code review requested (pokud ready)
- [ ] Inline code comments přidány
- [ ] Subtask status updated: `todo` → `in-progress` → `done`

---

**Subtask Version:** 1.0  
**Last Updated:** YYYY-MM-DD  
**Completed By:** [Developer Name]  
**Time Spent:** X hours
