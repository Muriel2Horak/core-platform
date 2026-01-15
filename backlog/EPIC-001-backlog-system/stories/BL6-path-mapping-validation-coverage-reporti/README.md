---
id: CORE-006
epic: EPIC-001-backlog-system
title: "Path Mapping Validation & Coverage Reporting"
priority: P1  # Must Have - blocks CORE-005 enhanced version
status: ready
assignee: "GitHub Copilot"
created: 2025-11-06
updated: 2025-11-06
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-001-backlog-system/stories/BL6-path-mapping-validation-coverage-reporti/README.md
    - backlog/EPIC-001-backlog-system/README.md
---

# CORE-006: Path Mapping Validation & Coverage Reporting

> **Epic:** [EPIC-001-backlog-system](../README.md)  
> **Priority:** P1 | **Status:** ready | **Estimate:** 2 days

## 👤 Role / Potřeba / Benefit

Jako **developer** potřebuji **automatickou validaci path mappingu v stories** abych **zajistil že všechny deklarované soubory existují a coverage reporting je přesný**.

**Kontext:**
Každá story má `path_mapping` sekci deklarující:
- `code_paths`: Implementační soubory (`.java`, `.ts`, `.tsx`)
- `test_paths`: Test soubory (`.spec.ts`, `Test.java`)
- `docs_paths`: Dokumentační soubory (`.md`)

**Problém:**
- Paths mohou být zastaralé (soubory přejmenovány/smazány)
- Typo v cestách (copy-paste errors)
- Neúplný mapping (chybí testy nebo docs)
- Nelze měřit coverage (kolik % deklarovaných souborů má změny)

**Value Proposition:**
- **Před:** Manuální validace path mappingu (error-prone)
- **Po:** Automatický validator + coverage reporting
- **Metrics:** Path accuracy 95%+ (per EPIC-001 success criteria)

---

## ✅ Definition of Ready (DoR)

Tato story je připravená k implementaci pokud:

- [x] Role/Need/Benefit je jasně definovaný ✅
- [x] Všechna Akceptační kritéria (AC) jsou měřitelná a testovatelná ✅
- [x] Implementation Mapping je vyplněný ✅
- [x] Dependencies identifikovány: Depends on CORE-001 (templates) ✅
- [x] Design/UI mockupy: CLI tool - žádné UI ✅
- [x] Technical approach: Python script pro YAML parsing + path validation ✅
- [x] Estimace: 2 dny (1 den script, 0.5 dne tests, 0.5 dne docs) ✅
- [x] Story je na top backlogu (Priority 1 Must Have) ✅

---

## 🎯 Akceptační kritéria (AC)

> **Formát:** Given [context/precondition], When [action/trigger], Then [expected outcome]

### AC1: Read Path Mapping from Story YAML

**Given** story má `path_mapping` sekci s `code_paths`, `test_paths`, `docs_paths`  
**When** validator čte story README.md  
**Then** extrahuje všechny paths do strukturovaných dat

**Test:**
```gherkin
Scenario: Parse path mapping from CORE-001 story
  Given story CORE-001 has path_mapping section
  When validator runs: path_validator.py --story CORE-001
  Then extracts:
    - code_paths: ["backlog/templates/story.md", "backlog/templates/subtask.md", "backlog/templates/epic.md"]
    - test_paths: []
    - docs_paths: ["backlog/README.md", "backlog/templates/README.md"]
```

---

### AC2: Validate File Existence

**Given** paths jsou extrahovány z story  
**When** validator checks filesystem  
**Then** reportuje které soubory existují a které chybí

**Test:**
```gherkin
Scenario: Check file existence for all paths
  Given story has code_paths: ["backend/src/Main.java", "frontend/src/App.tsx"]
  And story has test_paths: ["backend/test/MainTest.java", "frontend/src/App.spec.tsx"]
  When validator checks existence
  Then reports:
    - code_paths: 2/2 exist ✅
    - test_paths: 1/2 exist ⚠️ (frontend/src/App.spec.tsx MISSING)
    - overall: 75% (3/4 files exist)
```

---

### AC3: Coverage Reporting (Text Output)

**Given** validator checked all paths  
**When** user requests text report  
**Then** outputs human-readable coverage summary

**Test:**
```gherkin
Scenario: Generate text coverage report
  Given story CORE-005 validated
  When user runs: path_validator.py --story CORE-005 --format text
  Then output contains:
    """
    📊 Path Mapping Coverage: CORE-005
    
    ✅ code_paths:  1/1 (100%) - scripts/backlog/git_tracker.sh
    ✅ test_paths:  1/1 (100%) - scripts/backlog/test_integration.py
    ✅ docs_paths:  3/3 (100%) - backlog/README.md, docs/development/backlog-workflow.md, CHANGELOG.md
    
    📈 Overall: 100% (5/5 paths exist)
    """
```

---

### AC4: Coverage Reporting (JSON Output)

**Given** validator checked all paths  
**When** user requests JSON report  
**Then** outputs machine-readable JSON for automation

**Test:**
```gherkin
Scenario: Generate JSON coverage report
  Given story CORE-003 validated
  When user runs: path_validator.py --story CORE-003 --format json
  Then JSON output is valid and contains:
    {
      "story_id": "CORE-003",
      "coverage": {
        "code_paths": {"total": 1, "exist": 1, "missing": [], "percentage": 100},
        "test_paths": {"total": 1, "exist": 1, "missing": [], "percentage": 100},
        "docs_paths": {"total": 3, "exist": 3, "missing": [], "percentage": 100}
      },
      "overall": {"total": 5, "exist": 5, "percentage": 100}
    }
```

---

### AC5: Epic-Level Aggregation

**Given** multiple stories v epic mají path mappings  
**When** user requests epic-level report  
**Then** agreguje coverage přes všechny stories

**Test:**
```gherkin
Scenario: Aggregate coverage for EPIC-001
  Given EPIC-001 has 3 stories with path mappings
  When user runs: path_validator.py --epic EPIC-001
  Then output shows:
    - CORE-001: code 3/3, test 0/0, docs 2/2 (100%)
    - CORE-003: code 1/1, test 1/1, docs 3/3 (100%)
    - CORE-005: code 1/1, test 0/1, docs 3/3 (80%)
    - Epic total: 93% (13/14 paths exist)
```

---

### AC6: Performance & Error Handling

**Performance:**
- Validator processes 100 stories < 5 seconds
- No external dependencies (pure Python stdlib)

**Error handling:**
- Invalid YAML → clear error message with line number
- Missing path_mapping section → returns 0% coverage (not error)
- Relative vs absolute paths → normalized to repo root
- Glob patterns supported: `backend/src/**/*.java`

---

## 📂 Implementation Mapping

> **Účel:** Mapování story → kód/testy/dokumentace pro GitHub Copilot a git tracking

### Code Paths
Soubory které budou vytvořeny při implementaci:

```yaml
code_paths:
  - scripts/backlog/path_validator.py          # Main validator script
  - scripts/backlog/lib/yaml_parser.py         # YAML frontmatter parser
  - scripts/backlog/lib/path_checker.py        # File existence validator
  - scripts/backlog/lib/coverage_reporter.py   # Text/JSON reporting
```

**Copilot Prompt:**
```
Create Python CLI tool for path mapping validation:
- Parse YAML frontmatter from story README.md (path_mapping section)
- Check file existence for code_paths, test_paths, docs_paths
- Generate coverage reports (text and JSON formats)
- Support --story, --epic, --format flags
- Pure Python stdlib (no external dependencies)
```

---

### Test Paths
Test soubory pro validaci funkce:

```yaml
test_paths:
  - scripts/backlog/test_path_validator.py     # Unit tests for validator
  - scripts/backlog/test_yaml_parser.py        # YAML parser tests
  - scripts/backlog/test_coverage_reporter.py  # Reporter tests
```

**Testing Strategy:**
- Unit tests: >80% line coverage
- Integration tests: Real story files (CORE-001, CORE-003, CORE-005)
- Edge cases: Missing sections, invalid YAML, glob patterns

---

### Docs Paths
Dokumentace která bude aktualizována:

```yaml
docs_paths:
  - backlog/README.md                          # Add "Path Validation" section
  - docs/development/backlog-workflow.md       # Usage examples
  - CHANGELOG.md                               # Add CORE-006 entry
  - scripts/backlog/README.md                  # Tool documentation (NEW)
```

**Documentation Requirements:**
- CLI usage examples (text/JSON reports)
- Integration with git_tracker.sh
- Coverage metrics interpretation
- Troubleshooting guide

---
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
- [ ] Všechny soubory z `code_paths` jsou implementovány (4 Python files)
- [ ] CLI funguje s --story, --epic, --format flags
- [ ] Kód splňuje všechna AC1-AC6
- [ ] Žádné lint errors (flake8, mypy type checking)
- [ ] Python code style (black, isort)

### 🧪 Testing
- [ ] Unit testy pro všechny moduly (>80% coverage)
- [ ] Integration testy na reálných stories (CORE-001, CORE-003, CORE-005)
- [ ] Edge case testy (missing sections, invalid YAML, glob patterns)
- [ ] Manual testing: `path_validator.py --epic EPIC-001` works
- [ ] Performance: 100 stories validated < 5 seconds

### � Documentation
- [ ] CLI usage v `scripts/backlog/README.md`
- [ ] Examples v `backlog/README.md` (text + JSON output)
- [ ] Integration guide v `docs/development/backlog-workflow.md`
- [ ] CHANGELOG.md updated s CORE-006 entry
- [ ] Inline docstrings (Google style)

### 🚀 Deployment & Git
- [ ] Feature branch merged do `main`
- [ ] Commits referencují CORE-006 (feat/docs/test)
- [ ] Script executable: `chmod +x scripts/backlog/path_validator.py`
- [ ] Verified on main branch (smoke test)

---

## 📋 Subtasks

> **Rozklad story na implementační tasky** (celkem ~16 hours = 2 dny)

### Subtask 1: Project Setup & YAML Parser (3 hours)
- [ ] Vytvořit Git branch: `feature/CORE-006-path-mapping-validation-coverage-reporting`
- [ ] Setup Python project struktura: `scripts/backlog/lib/`
- [ ] Implementovat YAML frontmatter parser
- [ ] Unit testy pro parser (edge cases: missing sections, invalid YAML)

**Files:**
- [ ] `scripts/backlog/lib/yaml_parser.py`
- [ ] `scripts/backlog/test_yaml_parser.py`

**AC covered:** AC1 (Read Path Mapping)

---

### Subtask 2: Path Existence Validator (3 hours)
- [ ] Implementovat file existence checker
- [ ] Support pro glob patterns (`backend/src/**/*.java`)
- [ ] Normalize relative vs absolute paths
- [ ] Unit testy na reálných stories (CORE-001, CORE-003, CORE-005)

**Files:**
- [ ] `scripts/backlog/lib/path_checker.py`
- [ ] `scripts/backlog/test_path_checker.py`

**AC covered:** AC2 (Validate File Existence)

---

### Subtask 3: Coverage Reporter (Text + JSON) (3 hours)
- [ ] Implementovat text reporter (human-readable s emojis)
- [ ] Implementovat JSON reporter (machine-readable)
- [ ] Story-level reporting (--story flag)
- [ ] Epic-level aggregation (--epic flag)

**Files:**
- [ ] `scripts/backlog/lib/coverage_reporter.py`
- [ ] `scripts/backlog/test_coverage_reporter.py`

**AC covered:** AC3 (Text Output), AC4 (JSON Output), AC5 (Epic Aggregation)

---

### Subtask 4: CLI Tool Integration (2 hours)
- [ ] Main CLI script s argparse
- [ ] Flags: --story, --epic, --format (text|json)
- [ ] Help message s usage examples
- [ ] Error handling (invalid story ID, missing files)

**Files:**
- [ ] `scripts/backlog/path_validator.py`
- [ ] `scripts/backlog/test_path_validator.py`

**AC covered:** AC6 (Performance & Error Handling)

---

### Subtask 5: Integration Testing & Performance (2 hours)
- [ ] Integration tests na všech EPIC-001 stories
- [ ] Performance test: 100 stories < 5 seconds
- [ ] Edge case tests (missing path_mapping, empty epic)
- [ ] Manual testing: `path_validator.py --epic EPIC-001`

**Files:**
- [ ] `scripts/backlog/test_integration.py`

**AC covered:** All AC1-AC6 end-to-end validation

---

### Subtask 6: Documentation & Finalization (3 hours)
- [ ] CLI usage v `scripts/backlog/README.md`
- [ ] Examples v `backlog/README.md` (text + JSON)
- [ ] Integration guide v `docs/development/backlog-workflow.md`
- [ ] CHANGELOG.md entry s features a examples
- [ ] Script executable + Git commit

**Files:**
- [ ] `scripts/backlog/README.md` (NEW)
- [ ] `backlog/README.md`
- [ ] `docs/development/backlog-workflow.md`
- [ ] `CHANGELOG.md`

**AC covered:** Documentation for all features

---

## 🔗 Related Stories

### Depends On (Blokovači)
- [x] [CORE-001: Markdown Structure & Templates](../CORE-001-markdown-structure-templates/README.md) - Potřebujeme story template s path_mapping sekcí ✅
- [ ] CORE-003 (Story Generator) - Ne blocker, ale validátor může najít chyby v generovaných stories

### Blocks (Blokuje tyto stories)
- [ ] **CORE-005 Enhanced** - Git tracker bude potřebovat path mapping pro auto-update DoD checkboxes
- [ ] **CORE-007** (Validator & DoD Checker) - Bude integrovat path validation do pre-merge validation

### Related (Kontext)
- [x] [CORE-005: Git Commit Tracker](../CORE-005-git-commit-tracker/README.md) - Současná verze funguje bez path mappingu, enhanced verze bude integrovat

### Related (Související)
- [x] [CORE-003: Story Generator](../CORE-003-story-generator/README.md) - Generuje stories s path_mapping, validator může najít chyby

---

## 📊 Metrics & Success Criteria

### Before (Current State)
- **Path Accuracy:** Unknown - žádná validace path mappingu
- **Coverage Tracking:** Manual - musím ručně kontrolovat které soubory změnit
- **Story Completeness:** Subjektivní - nevím kolik % DoD je splněno
- **Time to validate:** 5-10 minut manuální review per story

### After (Target State)
- **Path Accuracy:** >95% (per EPIC-001 success criteria)
- **Coverage Tracking:** Automatický - `path_validator.py --story CORE-XXX` instant report
- **Story Completeness:** Měřitelné - "code_paths: 100%, test_paths: 67%, docs_paths: 100%"
- **Time to validate:** <1 sekunda per story (<5s pro celý epic)

### Success Criteria
- ✅ Všechna AC1-AC6 splněna
- ✅ Validator najde >95% missing/incorrect paths (tested on EPIC-001 stories)
- ✅ Performance: 100 stories < 5 seconds
- ✅ Zero false positives (paths which exist but reported as missing)
- ✅ Integration ready pro CORE-005 enhanced (Git tracker + path mapping)

---

## 📝 Implementation Notes

### Technical Decisions

**Why Python instead of Bash?**
- YAML parsing - Python má `yaml` lib, Bash by potřeboval `yq` external dependency
- JSON output - Python `json` module, Bash je verbose
- Glob patterns - Python `pathlib.Path.glob()` robust
- Future extensibility - easier než Bash pro complex logic

**YAML Parser Strategy:**
- Use frontmatter parsing (lines between `---` markers)
- Parse as YAML with `yaml.safe_load()`
- Extract `path_mapping` section (dict with code_paths/test_paths/docs_paths)
- Fallback: missing section → empty lists (not error)

**Path Normalization:**
- All paths relative to repo root (`/Users/martinhorak/Projects/core-platform`)
- Support glob patterns: `backend/src/**/*.java`
- Absolute paths converted to relative (strip repo root prefix)

### GitHub Copilot Optimization

**Copilot Prompts:**
```bash
# Generate YAML parser
"Create Python function to parse YAML frontmatter from Markdown file. Extract path_mapping section with code_paths, test_paths, docs_paths arrays. See CORE-006 story in backlog/EPIC-001-backlog-system/stories/CORE-006-path-mapping-validation-coverage-reporting/README.md"

# Generate path validator
"Implement path existence checker for files in path_mapping. Support glob patterns. Return dict with total/exist/missing counts per category (code/test/docs). See CORE-006 AC2."

# Generate coverage reporter
"Create coverage report generator (text and JSON formats). Text uses emojis (✅/⚠️), JSON is machine-readable. See CORE-006 AC3-AC4 examples."
```

### Known Issues / Tech Debt
- **TODO:** Glob pattern performance - for large repos (10k+ files), caching may be needed
- **TODO:** Path mapping schema validation - currently accepts any YAML, could enforce structure
- **Future:** IDE integration (VS Code extension showing real-time coverage in editor)

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
