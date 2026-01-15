---
id: CORE-007
epic: EPIC-001-backlog-system
title: "Test-First Development & Bug Tracking Workflow"
priority: P1
status: ready
assignee: ""
created: 2025-11-06
updated: 2025-11-06
estimate: "2 days"
path_mapping:
  code_paths:
    - backlog/templates/bug.md
    - backlog/templates/story.md
    - scripts/backlog/test_validator.sh
    - docs/development/test-driven-workflow.md
  test_paths:
    - scripts/backlog/test_story_validator.py
  docs_paths:
    - backlog/README.md
    - docs/development/backlog-workflow.md
    - CHANGELOG.md
---

# CORE-007: Test-First Development & Bug Tracking Workflow

> **Epic:** [EPIC-001-backlog-system](../../README.md)  
> **Priority:** P1 | **Status:** ready | **Estimate:** 2 days

## 👤 Role / Potřeba / Benefit

Jako **developer** potřebuji **test-first workflow integrovaný do backlog systému** abych **zajistil že každá feature/bug fix má automatické testy, které ověřují správnou funkcionalitu**.

**Kontext:**
Současný backlog systém podporuje path mapping pro testy, ale chybí:
- ❌ Explicitní vazba AC (Acceptance Criteria) → konkrétní test soubor
- ❌ Bug template s regression test požadavkem
- ❌ Test execution tracking (failed → create bug)
- ❌ Workflow enforcement (nelze mergovat bez testů)

**Value Proposition:**
- ✅ 100% features mají testy (žádná story bez test_paths)
- ✅ Každý bug má regression test (prevents recurrence)
- ✅ Test failures automaticky trackovány
- ✅ Quality gate: No merge without passing tests

---

## ✅ Definition of Ready (DoR)

Tato story je připravená k implementaci pokud:

- [x] Role/Need/Benefit je jasně definovaný
- [x] Všechna Akceptační kritéria (AC) jsou měřitelná a testovatelná
- [x] Implementation Mapping je vyplněný (code_paths, test_paths, docs_paths)
- [x] Dependencies na jiné stories jsou identifikovány (CORE-001, 003, 005, 006)
- [x] Technical approach je diskutovaný (extend templates + add validator script)
- [x] Estimace je provedena (2 days)
- [x] Story je ready (EPIC-001 Phase 2)

---

## 🎯 Akceptační kritéria (AC)

### AC1: Story template má povinnou AC → Test mapping sekci

**Given** developer vytvořil story pomocí `make backlog-new`  
**When** otevře vygenerovanou story README.md  
**Then** story obsahuje sekci "AC to Test Mapping" s template pro každé AC

**Test:**
```bash
# Test existence sekce v template
grep -q "## 🧪 AC to Test Mapping" backlog/templates/story.md
echo $?  # Expected: 0 (found)

# Test struktury
cat backlog/templates/story.md | grep -A 10 "AC to Test Mapping"
# Expected output obsahuje:
# - AC1 → Unit Test: [file path]
# - AC1 → Integration Test: [file path]
# - AC1 → E2E Test: [file path]
```

**Template náhled:**
```markdown
## 🧪 AC to Test Mapping

### AC1: [Feature] works correctly
- **Unit Test:** `backend/src/test/java/.../FeatureTest.java`
  - Status: ⏳ Not written | ✅ Written | 🧪 Passing | ❌ Failing
  - Coverage: XX%
  - Last run: YYYY-MM-DD
  
- **Integration Test:** `backend/src/test/java/.../FeatureIntegrationTest.java`
  - Status: ⏳ Not written | ✅ Written | 🧪 Passing | ❌ Failing
  
- **E2E Test:** `e2e/specs/feature.spec.ts`
  - Status: ⏳ Not written | ✅ Written | 🧪 Passing | ❌ Failing
  - Test ID: @CORE-XXX @AC1
```

---

### AC2: Bug template existuje s regression test requirement

**Given** developer našel bug během implementace  
**When** vytvoří bug report  
**Then** použije `backlog/templates/bug.md` template s povinnou regression test sekcí

**Test:**
```bash
# Test existence bug template
test -f backlog/templates/bug.md
echo $?  # Expected: 0

# Test obsahuje regression test sekci
grep -q "## 🧪 Regression Test" backlog/templates/bug.md
echo $?  # Expected: 0

# Test obsahuje caused_by fields
grep -q "caused_by_story:" backlog/templates/bug.md
grep -q "caused_by_commit:" backlog/templates/bug.md
```

**Bug template YAML frontmatter:**
```yaml
---
id: BUG-XXX
type: bug
severity: critical | high | medium | low
caused_by_story: CORE-YYY
caused_by_commit: abc1234
found_in_version: v1.2.3
status: reported | investigating | fixed | verified | closed
regression_test: e2e/specs/regression/bug-xxx.spec.ts
---
```

---

### AC3: Test validator script ověřuje AC coverage

**Given** story má vyplněné AC1-AC3  
**When** developer spustí `bash scripts/backlog/test_validator.sh --story CORE-XXX`  
**Then** validator zkontroluje:
1. Každé AC má alespoň 1 test mapping
2. Všechny test soubory z mappingu existují
3. Report obsahuje % test coverage per AC

**Test:**
```bash
# Create test validator
test -f scripts/backlog/test_validator.sh
echo $?  # Expected: 0

# Executable
test -x scripts/backlog/test_validator.sh
echo $?  # Expected: 0

# Run on CORE-005 (has AC and test_paths)
bash scripts/backlog/test_validator.sh --story CORE-005

# Expected output:
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 🧪 Test Coverage Report: CORE-005
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# AC1: Git tracker shows commits per story
#   ✅ Unit test: scripts/backlog/test_integration.py (exists)
#   ⚠️  Integration test: MISSING
#   ⚠️  E2E test: MISSING
#   Coverage: 33% (1/3 test types)
#
# AC2: Git tracker outputs JSON format
#   ✅ Unit test: scripts/backlog/test_integration.py (exists)
#   Coverage: 33% (1/3 test types)
#
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📈 Overall: 33% AC test coverage (2/6 tests exist)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### AC4: Documentation obsahuje test-first workflow

**Given** developer čte workflow guide  
**When** hledá "how to write tests for story"  
**Then** najde sekci "Test-First Development Workflow" s kroky:
1. Write story with AC
2. Map each AC to test files
3. Write tests FIRST (red)
4. Implement feature (green)
5. Refactor (clean)
6. Validate with test_validator.sh

**Test:**
```bash
# Test existence sekce
grep -q "Test-First Development" docs/development/backlog-workflow.md
echo $?  # Expected: 0

# Test obsahuje TDD kroky
grep -A 20 "Test-First" docs/development/backlog-workflow.md | grep -q "Write tests FIRST"
echo $?  # Expected: 0
```

---

### AC5: DoD template má test execution checklist

**Given** story template je vygenerovaná  
**When** developer scrollne k DoD sekci  
**Then** vidí checklist:
- [ ] All AC have test mappings (min 1 test per AC)
- [ ] All tests written and passing
- [ ] Test coverage >80% for new code
- [ ] No failing tests in CI
- [ ] Regression tests added for any bugs found

**Test:**
```bash
grep -A 30 "Definition of Done" backlog/templates/story.md | grep -q "All AC have test mappings"
echo $?  # Expected: 0
```

---

### AC6: Bug template má Fix DoD s regression test

**Given** bug template existuje  
**When** developer vytvoří bug fix  
**Then** DoD obsahuje:
- [ ] Regression test written (prevents recurrence)
- [ ] Regression test tagged with @BUG-XXX @regression
- [ ] Original AC from story still passing
- [ ] Bug verified by reporter

**Test:**
```bash
grep -A 20 "Fix Definition of Done" backlog/templates/bug.md | grep -q "Regression test written"
echo $?  # Expected: 0
```

---

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

## 📂 Implementation Mapping

```yaml
code_paths:
  - backlog/templates/bug.md                          # NEW: Bug report template
  - backlog/templates/story.md                        # UPDATE: Add AC→Test mapping section
  - scripts/backlog/test_validator.sh                 # NEW: Test coverage validator
  - docs/development/test-driven-workflow.md          # NEW: TDD workflow guide

test_paths:
  - scripts/backlog/test_story_validator.py            # Unit tests for validator
  - e2e/specs/backlog/test-first-workflow.spec.ts    # E2E test for workflow

docs_paths:
  - backlog/README.md                                 # Update with bug tracking info
  - docs/development/backlog-workflow.md              # Add TDD section
  - CHANGELOG.md                                      # Document CORE-007
```

---

## ✅ Definition of Done (DoD)

### 📝 Code Complete
- [ ] Bug template (`backlog/templates/bug.md`) vytvořen
- [ ] Story template aktualizován o "AC to Test Mapping" sekci
- [ ] Test validator script (`scripts/backlog/test_validator.sh`) implementován
- [ ] Validator umí parsovat AC sekce ze stories
- [ ] Validator ověřuje existence test souborů
- [ ] Validator generuje coverage report (text + JSON)

### 🧪 Testing
- [ ] **Test validator má vlastní unit testy**
  - [ ] Test parsování AC sekcí
  - [ ] Test file existence checking
  - [ ] Test coverage calculation
  - [ ] Test JSON output format
- [ ] **E2E test pro celý workflow**
  - [ ] Create story → Fill AC → Map tests → Validate
  - [ ] Create bug → Add regression test → Verify template
- [ ] **Integration test**
  - [ ] Test validator na real story (CORE-005)
  - [ ] Verify coverage report accuracy
- [ ] All AC have corresponding tests (AC1-AC6)
- [ ] All tests passing (unit + integration + E2E)
- [ ] Test coverage >80% for new code
- [ ] No failing tests in CI

### 📚 Documentation
- [ ] `docs/development/test-driven-workflow.md` vytvořen
  - [ ] TDD workflow steps (Red → Green → Refactor)
  - [ ] AC to Test mapping guide
  - [ ] Bug tracking lifecycle
  - [ ] Regression test tagging (@BUG-XXX @regression)
- [ ] `backlog/README.md` aktualizován
  - [ ] Bug tracking section added
  - [ ] Test-first workflow mentioned
  - [ ] Examples of bug template usage
- [ ] `docs/development/backlog-workflow.md` aktualizován
  - [ ] "Test-First Development" section
  - [ ] Bug lifecycle workflow
  - [ ] Integration with CI/CD
- [ ] `CHANGELOG.md` entry pro CORE-007
- [ ] Bug template má clear usage instructions
- [ ] Test validator has `--help` documentation

### 🔒 Quality & Security
- [ ] Code review done (templates + validator logic)
- [ ] Test validator handles edge cases:
  - [ ] Story without AC
  - [ ] AC without test mapping
  - [ ] Non-existent test files
  - [ ] Malformed YAML frontmatter
- [ ] Bug template prevents sensitive data in public repos
- [ ] Regression test tagging convention documented

### 🤝 Team Alignment
- [ ] Team review of bug template format
- [ ] Agreement on test-first workflow enforcement
- [ ] Decision: Soft vs hard requirement (warning vs error)
- [ ] CI integration plan discussed

### 🚀 Deployment & Git
- [ ] Templates committed to `backlog/templates/`
- [ ] Test validator committed to `scripts/backlog/`
- [ ] Documentation committed
- [ ] Changes merged to main
- [ ] EPIC-001 updated with CORE-007 completion

---

## 📋 Subtasks

### Subtask 1: Create Bug Template (3 hours)
- [ ] Create `backlog/templates/bug.md`
- [ ] YAML frontmatter with all required fields:
  - [ ] id, type, severity, status
  - [ ] caused_by_story, caused_by_commit
  - [ ] found_in_version, regression_test
- [ ] Sections:
  - [ ] Bug Description (expected vs actual)
  - [ ] Reproduction Steps
  - [ ] AC for Fix (Given/When/Then)
  - [ ] Regression Test section (mandatory)
  - [ ] Related Stories/Commits
  - [ ] Fix DoD checklist
- [ ] Usage instructions in template comments

**Files:**
- [ ] `backlog/templates/bug.md`

---

### Subtask 2: Update Story Template (2 hours)
- [ ] Add "AC to Test Mapping" section after AC section
- [ ] Template for each AC:
  ```markdown
  ### AC1 Tests
  - Unit Test: [path] | Status: ⏳/✅/❌ | Coverage: XX%
  - Integration Test: [path] | Status: ⏳/✅/❌
  - E2E Test: [path] | Status: ⏳/✅/❌ | Test ID: @STORY @AC1
  ```
- [ ] Update DoD with test-specific items:
  - [ ] "All AC have test mappings (min 1 test per AC)"
  - [ ] "All tests written and passing"
  - [ ] "Test coverage >80%"
  - [ ] "No failing tests"

**Files:**
- [ ] `backlog/templates/story.md`

---

### Subtask 3: Implement Test Validator Script (5 hours)
- [ ] Create `scripts/backlog/test_validator.sh`
- [ ] Functions:
  - [ ] `parse_ac_sections()` - Extract AC1, AC2, ... from story
  - [ ] `extract_test_mappings()` - Parse test paths from AC sections
  - [ ] `validate_test_existence()` - Check if test files exist
  - [ ] `calculate_coverage()` - % of AC with tests
  - [ ] `generate_report()` - Text output with emojis
  - [ ] `generate_json()` - JSON output for automation
- [ ] CLI options:
  - [ ] `--story CORE-XXX` - Validate single story
  - [ ] `--epic EPIC-XXX` - Validate all stories in epic
  - [ ] `--format text|json` - Output format
  - [ ] `--min-coverage NN` - Fail if coverage < NN%
- [ ] Integration with path_validator.py

**Files:**
- [ ] `scripts/backlog/test_validator.sh`

---

### Subtask 4: Write Validator Unit Tests (3 hours)
- [ ] Create `scripts/backlog/test_story_validator.py`
- [ ] Test scenarios:
  - [ ] Parse AC sections from CORE-005
  - [ ] Extract test mappings
  - [ ] Validate existing vs missing files
  - [ ] Coverage calculation (33% for CORE-005)
  - [ ] JSON output format
  - [ ] Edge cases (no AC, malformed YAML)
- [ ] All tests must pass

**Files:**
- [ ] `scripts/backlog/test_story_validator.py`

---

### Subtask 5: Write TDD Workflow Documentation (3 hours)
- [ ] Create `docs/development/test-driven-workflow.md`
- [ ] Sections:
  - [ ] **Test-First Philosophy** (why TDD?)
  - [ ] **Red → Green → Refactor** cycle
  - [ ] **Story to Test Workflow:**
    1. Write story with AC
    2. Map each AC to test files
    3. Write failing tests (red)
    4. Implement feature (green)
    5. Refactor code (clean)
    6. Validate with test_validator
  - [ ] **Bug Tracking:**
    - [ ] When to create bug vs fix directly
    - [ ] Bug template usage
    - [ ] Regression test requirement
    - [ ] Tagging convention (@BUG-XXX @regression)
  - [ ] **CI/CD Integration:**
    - [ ] Pre-merge validation
    - [ ] Test coverage gates
    - [ ] Automated bug creation on test failure
  - [ ] **Examples:**
    - [ ] Complete TDD workflow example
    - [ ] Bug fix workflow example
- [ ] Code samples and CLI commands

**Files:**
- [ ] `docs/development/test-driven-workflow.md`

---

### Subtask 6: Update Existing Documentation (2 hours)
- [ ] Update `backlog/README.md`:
  - [ ] Add "Bug Tracking" section
  - [ ] Link to bug template
  - [ ] Example bug creation workflow
- [ ] Update `docs/development/backlog-workflow.md`:
  - [ ] Add "Test-First Development" section
  - [ ] Reference test_validator.sh tool
  - [ ] Link to test-driven-workflow.md
- [ ] Update `CHANGELOG.md`:
  - [ ] CORE-007 entry with features
  - [ ] List new templates and tools

**Files:**
- [ ] `backlog/README.md`
- [ ] `docs/development/backlog-workflow.md`
- [ ] `CHANGELOG.md`

---

### Subtask 7: Create E2E Test for Workflow (2 hours)
- [ ] Create `e2e/specs/backlog/test-first-workflow.spec.ts`
- [ ] Test scenario:
  1. Generate new story with `make backlog-new`
  2. Verify story has "AC to Test Mapping" section
  3. Fill AC1 with test mapping
  4. Run `test_validator.sh --story CORE-XXX`
  5. Verify coverage report shows 0% (no tests yet)
  6. Create test files
  7. Re-run validator
  8. Verify coverage report shows 100%
- [ ] Bug creation scenario:
  1. Verify `backlog/templates/bug.md` exists
  2. Create bug from template
  3. Verify regression_test field present
  4. Verify Fix DoD has regression test item

**Files:**
- [ ] `e2e/specs/backlog/test-first-workflow.spec.ts`

---

## 🔗 Related Stories

### Depends On (Blokovači)
- [x] [CORE-001: Markdown Structure](../CORE-001-markdown-structure/README.md) - Templates must exist
- [x] [CORE-003: Story Generator](../CORE-003-story-generator/README.md) - Generator creates stories from templates
- [x] [CORE-005: Git Commit Tracker](../CORE-005-git-commit-tracker/README.md) - Track commits that introduced bugs
- [x] [CORE-006: Path Validator](../CORE-006-path-mapping-validation-coverage-reporting/README.md) - Validate test paths exist

### Blocks (Blokuje tyto stories)
- [ ] **CORE-009: CI/CD Test Gates** - Automated test validation before merge
- [ ] **CORE-010: Automated Bug Creation** - Create bugs from test failures

### Related (Související)
- [ ] **Platform Features** - All future stories will use test-first workflow

---

## 📊 Metrics & Success Criteria

### Before (Current State)
- **Test Coverage:** Varies per story (some 0%, some 100%)
- **Bug Tracking:** Ad-hoc (no standard template)
- **Test-First:** Optional (developers may skip tests)
- **Regression:** No systematic regression test tagging

### After (Target State)
- **Test Coverage:** 100% stories have AC → Test mappings
- **Bug Tracking:** Standardized bug template used for all bugs
- **Test-First:** Enforced workflow (validator catches missing tests)
- **Regression:** All bugs have tagged regression tests (@BUG-XXX @regression)

### Success Criteria
- ✅ Bug template created and documented
- ✅ Story template has AC → Test mapping section
- ✅ Test validator script working (CORE-005 validates to 33%)
- ✅ TDD workflow documented with examples
- ✅ All DoD items checked
- ✅ Team agrees to use test-first workflow

---

## 📝 Implementation Notes

### Technical Decisions

**Test Validator Implementation:**
- Bash script (consistent with git_tracker.sh)
- Parses Markdown (grep/sed/awk)
- Integrates with existing path_validator.py
- Text + JSON output for automation

**Bug Template Design:**
- YAML frontmatter (consistent with story template)
- Mandatory regression_test field
- Traceability: caused_by_story + caused_by_commit
- Severity classification (critical → low)

**Test-First Enforcement:**
- Soft requirement initially (warning, not error)
- Can evolve to hard requirement (CI gate)
- Developer education via docs

### GitHub Copilot Optimization

**Copilot Prompts:**
```
# For test validator
"Create bash function that parses AC sections from Markdown story file and extracts test file paths"

# For bug template
"Generate bug report template with YAML frontmatter including severity, caused_by_story, regression_test fields"

# For TDD workflow
"Write developer guide for Test-Driven Development workflow with Red-Green-Refactor cycle and AC to Test mapping"
```

### Known Issues / Tech Debt
- TODO: Integrate test_validator with CI/CD (CORE-009)
- TODO: Automated bug creation from test failures (CORE-010)
- TODO: Test execution status tracking (which tests ran when)

---

## 🏷️ Tags

`testing` `tdd` `quality` `bug-tracking` `workflow` `automation` `documentation`

---

**Story Version:** 1.0  
**Last Updated:** 2025-11-06  
**Author:** Development Team

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
- [ ] Unit testy napsány pro všechny `code_paths` (coverage >80%)
- [ ] Integration testy pokrývají happy path + error cases
- [ ] E2E testy pokrývají všechna Akceptační kritéria
- [ ] Všechny testy PASSING (CI/CD green)
- [ ] Manual testing provedeno (smoke test na dev environmentu)

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
