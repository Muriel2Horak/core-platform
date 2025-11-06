---
id: CORE-XXX
epic: EPIC-XXX-epic-name
title: "Short Story Title"
priority: P1  # P1 (Must Have) | P2 (Should Have) | P3 (Nice to Have)
status: ready  # ready | in-progress | blocked | done
assignee: ""
created: YYYY-MM-DD
updated: YYYY-MM-DD
estimate: "X days"
---

# CORE-XXX: [Story Title]

> **Epic:** [EPIC-XXX: Epic Name](../README.md)  
> **Priority:** PX | **Status:** [Status] | **Estimate:** X days

## 👤 Role / Potřeba / Benefit

Jako **[role]** potřebuji **[funkci/feature]** abych **[business benefit/outcome]**.

**Kontext:**
[Volitelný - proč tuto story děláme? Jaký problém řešíme? Jaké jsou pain pointy?]

**Value Proposition:**
[Volitelný - jaká je hodnota pro uživatele/business? Metrics před/po?]

---

## ✅ Definition of Ready (DoR)

Tato story je připravená k implementaci pokud:

- [ ] Role/Need/Benefit je jasně definovaný
- [ ] Všechna Akceptační kritéria (AC) jsou měřitelná a testovatelná
- [ ] Implementation Mapping je vyplněný (code_paths, test_paths, docs_paths)
- [ ] Dependencies na jiné stories jsou identifikovány a resolved/tracked
- [ ] Design/UI mockupy jsou k dispozici (pokud UI změny)
- [ ] Technical approach je diskutovaný a schválený týmem
- [ ] Estimace je provedena (v story points nebo dnech)
- [ ] Story je na top backlogu a připravená k pull do sprintu

---

## 🎯 Akceptační kritéria (AC)

> **Formát:** Given [context/precondition], When [action/trigger], Then [expected outcome]

### AC1: [Kritérium #1 - hlavní happy path]

**Given** [počáteční stav, např. "uživatel je přihlášený jako admin"]  
**When** [akce, např. "klikne na tlačítko 'Export Data'"]  
**Then** [očekávaný výsledek, např. "CSV soubor se stáhne obsahující všechny záznamy za poslední měsíc"]

**Test:**
```gherkin
Scenario: Export data as admin
  Given user is logged in as admin
  When user clicks "Export Data" button
  Then CSV file downloads with name "export-YYYY-MM-DD.csv"
  And file contains headers: [col1, col2, col3]
  And file contains all records from last 30 days
```

---

### AC2: [Kritérium #2 - edge case nebo validace]

**Given** [kontext]  
**When** [akce]  
**Then** [výsledek]

**Test:**
```gherkin
Scenario: [Test scenario name]
  Given [precondition]
  When [action]
  Then [assertion]
```

---

### AC3: [Kritérium #3 - error handling nebo další funkce]

**Given** [kontext]  
**When** [akce]  
**Then** [výsledek]

---

### AC4: [Kritérium #4 - performance/security requirement]

[Pokud relevantní - performance requirements, security constraints, atd.]

**Příklad:**
- API response time < 500ms for 95th percentile
- Data encrypted at rest and in transit
- User permissions checked before every operation

---

## 🧪 AC to Test Mapping

> **MANDATORY:** Každé AC MUSÍ mít definované testy! Test-first development.

### AC1: [Kritérium #1] → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Unit Test** | `backend/src/test/.../[Class]Test.java` | ⏳ Not Written | 0% | N/A | - |
| **Integration Test** | `backend/src/test/.../[Class]IntegrationTest.java` | ⏳ Not Written | 0% | N/A | - |
| **E2E Test** | `e2e/specs/[feature]/[scenario].spec.ts` | ⏳ Not Written | 0% | N/A | `@CORE-XXX @AC1` |

**Test Status Legend:**
- ⏳ **Not Written** - Test neexistuje
- ✍️ **Written** - Test existuje, ale možná failuje
- ✅ **Passing** - Test prošel (green)
- ❌ **Failing** - Test failuje (red)

**Coverage:** % AC requirement pokrytý testy (0-100%)

**Test-First Workflow:**
1. Napsat failing test (RED) ← Start here!
2. Implementovat minimum pro pass (GREEN)
3. Refaktorovat (CLEAN)

**Example:**
```typescript
// e2e/specs/export/export-data.spec.ts
test('exports CSV as admin @CORE-012 @AC1', async ({ page }) => {
  // Arrange
  await page.goto('/login');
  await loginAsAdmin(page);
  
  // Act
  await page.click('button:has-text("Export Data")');
  
  // Assert
  const download = await page.waitForEvent('download');
  const filename = download.suggestedFilename();
  expect(filename).toMatch(/^export-\d{4}-\d{2}-\d{2}\.csv$/);
  
  // Verify CSV content
  const path = await download.path();
  const content = await fs.readFile(path, 'utf-8');
  expect(content).toContain('col1,col2,col3'); // Headers
});
```

---

### AC2: [Kritérium #2] → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Unit Test** | `path/to/unit/test` | ⏳ Not Written | 0% | N/A | - |
| **Integration Test** | `path/to/integration/test` | ⏳ Not Written | 0% | N/A | - |
| **E2E Test** | `path/to/e2e/test` | ⏳ Not Written | 0% | N/A | `@CORE-XXX @AC2` |

---

### AC3: [Kritérium #3] → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Unit Test** | `path/to/unit/test` | ⏳ Not Written | 0% | N/A | - |
| **E2E Test** | `path/to/e2e/test` | ⏳ Not Written | 0% | N/A | `@CORE-XXX @AC3` |

**Note:** Integration test může být vynechán pokud není relevantní.

---

### AC4: [Kritérium #4] → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Performance Test** | `e2e/specs/performance/[test].spec.ts` | ⏳ Not Written | 0% | N/A | `@CORE-XXX @AC4 @performance` |

**Performance Test Example:**
```typescript
test('API responds within 500ms @CORE-012 @AC4 @performance', async ({ request }) => {
  const start = Date.now();
  
  const response = await request.get('/api/data');
  
  const duration = Date.now() - start;
  expect(response.ok()).toBeTruthy();
  expect(duration).toBeLessThan(500); // 95th percentile requirement
});
```

---

### Test Coverage Summary

| AC | Unit | Integration | E2E | Total Coverage | Status |
|----|------|-------------|-----|----------------|--------|
| AC1 | 0% | 0% | 0% | 0% | ⏳ Not Started |
| AC2 | 0% | 0% | 0% | 0% | ⏳ Not Started |
| AC3 | 0% | 0% | 0% | 0% | ⏳ Not Started |
| AC4 | 0% | 0% | 0% | 0% | ⏳ Not Started |
| **TOTAL** | **0%** | **0%** | **0%** | **0%** | ⏳ **Tests Required!** |

**Target:** 100% AC coverage (každé AC má min. 1 passing test)

**Validation:**
```bash
# Validuj test coverage pomocí test_validator
bash scripts/backlog/test_validator.sh --story CORE-XXX

# Expected output:
# AC1: 0% coverage (0/3 test types) ⚠️
# AC2: 0% coverage (0/3 test types) ⚠️
# AC3: 0% coverage (0/2 test types) ⚠️
# AC4: 0% coverage (0/1 test types) ⚠️
# TOTAL: 0% coverage ❌ TESTS REQUIRED!
```

---

## 📂 Implementation Mapping

> **Účel:** Mapování story → kód/testy/dokumentace pro GitHub Copilot a git tracking

### Code Paths
Soubory které budou vytvořeny/změněny při implementaci:

```yaml
code_paths:
  - backend/src/main/java/cz/muriel/core/[module]/[ClassName].java
  - backend/src/main/java/cz/muriel/core/[module]/dto/[DtoName].java
  - frontend/src/features/[feature]/[ComponentName].tsx
  - frontend/src/features/[feature]/hooks/use[HookName].ts
  - frontend/src/api/[apiName].ts
```

**Copilot Prompt:**
```
Implementuj CORE-XXX podle:
- backlog/EPIC-XXX/stories/CORE-XXX/README.md
- Vytvoř soubory z code_paths
- Dodržuj všechna AC
```

---

### Test Paths
Testy které budou vytvořeny:

```yaml
test_paths:
  - backend/src/test/java/cz/muriel/core/[module]/[ClassName]Test.java
  - backend/src/test/java/cz/muriel/core/[module]/[ClassName]IntegrationTest.java
  - frontend/src/features/[feature]/__tests__/[ComponentName].test.tsx
  - e2e/specs/[feature]/[test-name].spec.ts
```

**Test Coverage Očekáváno:**
- Unit tests: >80% line coverage
- Integration tests: Happy path + error cases
- E2E tests: Všechna AC pokryta

---

### Docs Paths
Dokumentace která bude aktualizována:

```yaml
docs_paths:
  - docs/api/[module].md           # API endpoints dokumentace
  - docs/features/[feature].md     # Feature usage guide
  - docs/architecture/[decision].md # ADR pokud architectural změna
  - README.md                       # Pokud user-facing změna
```

---

## ✅ Definition of Done (DoD)

Tato story je COMPLETE pokud:

### 📝 Code Complete
- [ ] Všechny soubory z `code_paths` jsou implementovány
- [ ] Kód splňuje všechna Akceptační kritéria (AC1-ACX)
- [ ] Code review provedeno (min. 1 approver)
- [ ] Žádné compiler warnings nebo lint errors
- [ ] Code style guide dodržen (prettier, ESLint, Checkstyle)

### 🧪 Testing
- [ ] **AC to Test Mapping vyplněno** (každé AC má min. 1 test)
- [ ] **Test-first workflow dodržen** (testy napsány PŘED implementací)
- [ ] Unit testy napsány pro všechny `code_paths` (coverage >80%)
- [ ] Integration testy pokrývají happy path + error cases
- [ ] E2E testy pokrývají VŠECHNA Akceptační kritéria
- [ ] **Test validator passed:** `test_validator.sh --story CORE-XXX` shows 100% AC coverage
- [ ] Všechny testy PASSING (CI/CD green)
- [ ] Manual testing provedeno (smoke test na dev environmentu)
- [ ] **Performance tests** (pokud AC4 obsahuje performance requirements)
- [ ] **Regression tests tagged** (pokud fix bugu: @BUG-XXX @regression)

### 📚 Documentation
- [ ] API dokumentace aktualizována (Swagger/OpenAPI + Markdown)
- [ ] User guide / feature docs aktualizovány
- [ ] Inline code comments přidány (zejména pro komplexní logiku)
- [ ] CHANGELOG.md aktualizován s entry pro tuto story
- [ ] Architecture Decision Record (ADR) vytvořen pokud major design decision

### 🔒 Quality & Security
- [ ] Security review provedeno (pokud změny v auth/permissions)
- [ ] Data privacy compliance ověřeno (GDPR pokud relevantní)
- [ ] Performance testing provedeno (pokud kritická path)
- [ ] Accessibility (a11y) ověřeno (pokud UI změny)

### 🤝 Team Alignment
- [ ] Product Owner schválil implementaci (demo/review)
- [ ] UX/Design review provedeno (pokud UI změny)
- [ ] Team demo provedeno (v sprint review)
- [ ] Knowledge sharing / dokumentace sdílena s týmem

### 🚀 Deployment & Git
- [ ] Feature branch merged do `main` (nebo `develop`)
- [ ] Všechny commits referencují story ID (např. `feat(XXX): ...`)
- [ ] Git tags vytvořeny pokud release
- [ ] Deployment do DEV environment úspěšný
- [ ] Deployment do STAGING ověřen (pokud applicable)
- [ ] Rollback plan dokumentován

---

## 📋 Subtasks

> **Rozklad story na implementační tasky** (pro developer tracking)

### Subtask 1: [Setup & Design] (X hours)
- [ ] Vytvořit Git branch: `feature/CORE-XXX-short-name`
- [ ] Setup boilerplate (backend service/controller/dto)
- [ ] Setup frontend komponenty struktura
- [ ] Diskutovat technical approach s týmem

**Files:**
- [ ] `backend/src/.../[Class].java`
- [ ] `frontend/src/.../[Component].tsx`

---

### Subtask 2: [Backend Implementation] (X hours)
- [ ] Implementovat business logiku podle AC1-AC3
- [ ] Přidat error handling a validace
- [ ] Implementovat DB queries/repositories
- [ ] API endpoint + request/response DTOs

**Files:**
- [ ] `backend/src/.../Service.java`
- [ ] `backend/src/.../Controller.java`
- [ ] `backend/src/.../Repository.java`

---

### Subtask 3: [Frontend Implementation] (X hours)
- [ ] Vytvořit UI komponenty podle mockupů
- [ ] Implementovat state management (hooks/context)
- [ ] Připojit na backend API
- [ ] Error handling a loading states

**Files:**
- [ ] `frontend/src/.../Component.tsx`
- [ ] `frontend/src/.../useHook.ts`
- [ ] `frontend/src/api/api.ts`

---

### Subtask 4: [Testing] (X hours)
- [ ] Unit testy pro backend (Service, Controller)
- [ ] Unit testy pro frontend (Component, hooks)
- [ ] Integration testy (API + DB)
- [ ] E2E testy podle AC

**Files:**
- [ ] `backend/src/test/.../Test.java`
- [ ] `frontend/src/.../__tests__/Test.tsx`
- [ ] `e2e/specs/.../spec.ts`

---

### Subtask 5: [Documentation] (X hours)
- [ ] API docs (Swagger annotations)
- [ ] Feature documentation
- [ ] Inline code comments
- [ ] Update CHANGELOG

**Files:**
- [ ] `docs/api/[module].md`
- [ ] `docs/features/[feature].md`
- [ ] `CHANGELOG.md`

---

### Subtask 6: [Review & Polish] (X hours)
- [ ] Code review feedback addressed
- [ ] Manual testing on dev environment
- [ ] Performance testing (pokud kritické)
- [ ] Final polish (UI tweaks, error messages)

---

## 🔗 Related Stories

### Depends On (Blokovači)
- [ ] [CORE-YYY: Dependency Story Name](../CORE-YYY-name/README.md) - Reason why this blocks us

### Blocks (Blokuje tyto stories)
- [ ] [CORE-ZZZ: Blocked Story Name](../CORE-ZZZ-name/README.md) - Reason why we block this

### Related (Související)
- [ ] [CORE-AAA: Related Story](../CORE-AAA-name/README.md) - How it's related

---

## 📊 Metrics & Success Criteria

### Before (Current State)
- **Metric 1:** [Current value, např. "Manual export trvá 15 minut"]
- **Metric 2:** [Current problem, např. "Error rate: 5% při exportu"]
- **Metric 3:** [Current pain point, např. "User complaints: 10/měsíc"]

### After (Target State)
- **Metric 1:** [Target value, např. "Automatický export za <30 sekund"]
- **Metric 2:** [Target improvement, např. "Error rate: <0.5%"]
- **Metric 3:** [Target outcome, např. "User complaints: 0"]

### Success Criteria
- ✅ Všechna AC splněna (AC1-AC4)
- ✅ Code coverage >80%
- ✅ Zero critical bugs po deployment
- ✅ User satisfaction score >4/5

---

## 📝 Implementation Notes

### Technical Decisions
[Volitelné - architektonické rozhodnutí, design patterns použité, atd.]

**Příklad:**
- Použili jsme Repository pattern pro DB access
- Frontend používá React Query pro API caching
- Validace na backend i frontend (defense in depth)

### GitHub Copilot Optimization
[Tipy jak nejlépe použít story s Copilotem]

**Copilot Prompts:**
```
# Pro backend
"Implementuj Service class podle CORE-XXX AC1-AC3 v backlog/.../README.md"

# Pro frontend
"Vytvoř React komponentu podle CORE-XXX UI mockupu a AC v backlog/.../README.md"

# Pro testy
"Vygeneruj E2E test pro CORE-XXX AC1 using Playwright"
```

### Known Issues / Tech Debt
[Volitelné - věci které chceme adresovat later]

**Příklad:**
- TODO: Přidat caching layer (CORE-XXX follow-up)
- TODO: Optimize DB query performance (track in CORE-YYY)

---

## 🏷️ Tags

`feature` `backend` `frontend` `api` `ui` `testing` `documentation`

---

## 📎 Attachments

- [UI Mockup](attachments/mockup-v1.png)
- [Architecture Diagram](attachments/architecture.svg)
- [API Spec](attachments/api-spec.yaml)

---

**Story Version:** 1.0  
**Last Updated:** YYYY-MM-DD  
**Author:** [Developer Name]
