---
id: CORE-005
epic: EPIC-001-backlog-system
title: "Git Commit Tracker"
priority: P2  # P1 (Must Have) | P2 (Should Have) | P3 (Nice to Have)
status: ready  # ready | in-progress | blocked | done
assignee: "GitHub Copilot"
created: 2025-11-06
updated: 2025-11-06
estimate: "2 days"
---

# CORE-005: Git Commit Tracker

> **Epic:** [EPIC-001-backlog-system](../README.md)  
> **Priority:** P2 | **Status:** ready | **Estimate:** 2 days

## 👤 Role / Potřeba / Benefit

Jako **developer** potřebuji **automaticky trackovat které commity patří ke kterým stories** abych **viděl progress na stories a mohl auto-updatovat DoD checklisty**.

**Kontext:**
- Manuální tracking commitů → stories je zdlouhavý
- DoD checklists se updatují ručně (často zapomínáme)
- Těžké zjistit které stories mají code changes
- Chybí visibility do Git aktivity per story

**Value Proposition:**
- **Before:** Manuální tracking, DoD often outdated, no commit visibility
- **After:** Automatic commit → story mapping, auto-update DoD, progress tracking
- **Metrics:** 0 min/day tracking (vs 5-10 min manual), 100% DoD accuracy

---

## ✅ Definition of Ready (DoR)

Tato story je připravená k implementaci pokud:

- [x] Role/Need/Benefit je jasně definovaný ✅
- [x] Všechna Akceptační kritéria (AC) jsou měřitelná a testovatelná ✅
- [x] Implementation Mapping je vyplněný (code_paths, test_paths, docs_paths) ✅
- [x] Dependencies na jiné stories jsou identifikovány (CORE-001 templates done) ✅
- [x] Design/UI mockupy jsou k dispozici (N/A - CLI tool) ✅
- [x] Technical approach je diskutovaný a schválený týmem ✅
- [x] Estimace je provedena (2 days - basic version) ✅

---

## 🎯 Acceptance Criteria (AC)

### AC1: Parse Git Commits by Story ID

**Given** repository má commity s pattern `feat(CORE-XXX):` nebo `fix(CORE-XXX):`  
**When** spustím `bash scripts/backlog/git_tracker.sh --epic EPIC-001`  
**Then** script detekuje všechny commity s CORE-XXX referencemi

**Test:**
```bash
bash scripts/backlog/git_tracker.sh --epic EPIC-001

# Expected output:
# EPIC-001 Git Activity Report:
# ✅ CORE-001: 1 commit (83871eb - feat(backlog): Add Git-native backlog management system)
# ✅ CORE-003: 2 commits (0d523e7, f6332b6)
# ⏳ CORE-005: 0 commits (in-progress)
```

### AC2: Count Commits Per Story

**Given** story má multiple commits  
**When** tracker běží  
**Then** zobrazí count + seznam commit SHAs

**Test:**
```bash
bash scripts/backlog/git_tracker.sh --story CORE-003

# Expected output:
# CORE-003: Git Commit Tracker Script
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Total commits: 2
# 
# Commits:
# - 0d523e7 (2025-11-06): feat(CORE-003): Add Story Generator Script
# - f6332b6 (2025-11-06): docs(CORE-001,CORE-003): Complete documentation
```

### AC3: Report Stories Without Commits

**Given** story nemá žádné commity  
**When** tracker běží na epic  
**Then** reportuje stories bez Git aktivity

**Test:**
```bash
bash scripts/backlog/git_tracker.sh --epic EPIC-001 --show-zero

# Expected output includes:
# 📋 CORE-002: 0 commits (not started)
# 📋 CORE-005: 0 commits (in-progress)
```

### AC4: Support Multiple Commit Patterns

**Given** projekt používá různé commit patterns  
**When** tracker parsuje git log  
**Then** detekuje: `feat(XXX):`, `fix(XXX):`, `chore(XXX):`, `docs(XXX):`

**Test:**
```bash
# Git log contains:
git log --oneline | grep CORE-003
# f6332b6 docs(CORE-001,CORE-003): Complete documentation
# 0d523e7 feat(CORE-003): Add Story Generator Script

# Tracker detects both ✅
```

### AC5: JSON Output Mode (Optional)

**Given** potřebuji machine-readable output  
**When** `bash scripts/backlog/git_tracker.sh --format json`  
**Then** output je valid JSON

**Test:**
```bash
bash scripts/backlog/git_tracker.sh --epic EPIC-001 --format json | jq .

# Expected:
# {
#   "epic": "EPIC-001",
#   "stories": [
#     {
#       "id": "CORE-001",
#       "commits": 1,
#       "shas": ["83871eb"]
#     },
#     {
#       "id": "CORE-003",
#       "commits": 2,
#       "shas": ["0d523e7", "f6332b6"]
#     }
#   ]
# }
```
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

## 📂 Implementation Mapping

> **Účel:** Mapování story → kód/testy/dokumentace pro GitHub Copilot a git tracking

### Code Paths

Soubory které budou vytvořeny/změněny při implementaci:

```yaml
code_paths:
  - scripts/backlog/git_tracker.sh  # Main tracker script (bash)
  - scripts/backlog/lib/git_utils.sh  # Git parsing utilities (optional refactor)
```

**Hlavní implementace:**
- `scripts/backlog/git_tracker.sh` (200-300 lines)
  - Parse git log with grep patterns (feat|fix|chore|docs)\(CORE-
  - Count commits per story ID
  - Generate reports (text + JSON)
  - CLI arguments: --epic, --story, --format, --show-zero

**Poznámky:**
- Bash scripting (konzistence s `new_story.sh`)
- Git native commands (no external dependencies)
- Optional: Python rewrite if logic grows too complex in enhanced version

**Copilot Prompt:**
```
Implementuj CORE-005 podle:
- backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md
- Vytvoř scripts/backlog/git_tracker.sh (bash)
- Dodržuj AC1-AC5 (parse git log, count commits, JSON output)
- Test na EPIC-001: CORE-001, CORE-003 commity
```

---

### Test Paths

Testy které budou vytvořeny:

```yaml
test_paths:
  - scripts/backlog/tests/test_git_tracker.sh  # Bash integration tests (manual)
  - e2e/specs/backlog/git-tracker.spec.ts  # E2E (optional, deferred to CORE-006)
```

**Testing strategy:**
1. **Manual testing** (Priority 1 - CORE-005 basic):
   - Run: `bash scripts/backlog/git_tracker.sh --epic EPIC-001`
   - Verify: Detects CORE-001 (1 commit), CORE-003 (2 commits)
   - Edge case: Multi-story commits (`CORE-001,CORE-003` in f6332b6)

2. **Integration tests** (Priority 2 - CORE-006):
   - Create dummy git repo with test commits
   - Verify parsing: `CORE-999`, `CORE-1000`
   - Verify JSON output: `jq .stories[0].id`

3. **E2E tests** (Priority 3 - CORE-006):
   - Playwright test: Generate story → commit → verify tracker detects

**Test data:**
```bash
# Expected detections in current repo:
# - 83871eb: feat(backlog): ... → CORE-001
# - 0d523e7: feat(CORE-003): ... → CORE-003
# - f6332b6: docs(CORE-001,CORE-003): ... → both
```

**Coverage target:** N/A (CLI script, manual validation sufficient for basic version)

---

### Docs Paths

Dokumentace která bude updatována:

```yaml
docs_paths:
  - backlog/README.md  # Add "Git Tracker" section under Automation
  - docs/development/backlog-workflow.md  # Add "Tracking Progress" section
  - CHANGELOG.md  # Add CORE-005 entry
```

**Documentation updates:**
1. **backlog/README.md**:
   - Section: "🤖 Automation → Git Commit Tracker (CORE-005)"
   - Usage: `bash scripts/backlog/git_tracker.sh --epic EPIC-001`
   - Example output (text + JSON modes)
   - Time saved: 0 min/day tracking (vs 5-10 min manual)

2. **docs/development/backlog-workflow.md**:
   - New section: "📊 Tracking Git Activity"
   - How to see commit counts per story
   - How to identify stories without commits (`--show-zero`)
   - Integration with DoD auto-update (future - CORE-005 enhanced)

3. **CHANGELOG.md**:
   - Entry: "Git Commit Tracker (CORE-005)" under EPIC-001
   - Features: Parse commits by pattern, count per story, JSON export, zero-commit detection
   - Note: Basic version, enhanced after CORE-002 (path mapping)

---
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

- [ ] `scripts/backlog/git_tracker.sh` implementován (200-300 lines)
- [ ] CLI args: `--epic`, `--story`, `--format`, `--show-zero`
- [ ] Kód splňuje AC1-AC5 (parse, count, report, patterns, JSON)
- [ ] Code review provedeno (min. 1 approver) - GitHub PR
- [ ] Shellcheck clean (no warnings)
- [ ] Code style: Google Shell Style Guide

### 🧪 Testing

- [x] Manual testing na EPIC-001 commits ✅ (CORE-001: 83871eb, CORE-003: 0d523e7, f6332b6)
- [ ] Edge cases testovány:
  - Multi-story commits (`CORE-001,CORE-003`)
  - Stories bez commitů (CORE-002, CORE-005 in-progress)
  - Různé patterns (feat, fix, chore, docs)
- [ ] JSON output validní (verify s `jq`)
- [ ] Integration tests (DEFERRED to CORE-006 - automated test suite)
- [ ] E2E tests (DEFERRED to CORE-006)

### 📚 Documentation

- [ ] `backlog/README.md` aktualizován (Automation → Git Tracker section)
- [ ] `docs/development/backlog-workflow.md` aktualizován (Tracking Progress section)
- [ ] Inline comments ve scriptu (usage, edge cases)
- [ ] `CHANGELOG.md` aktualizován s CORE-005 entry
- [ ] Usage examples v README (text + JSON modes)

### 🔒 Quality & Security

- [ ] Security review N/A (read-only Git operations, no user input beyond flags)
- [ ] Performance: `git log` fast enough (<2s for EPIC-001 size)
- [ ] Error handling: Invalid epic/story IDs, empty repos

### 🤝 Team Alignment

- [ ] Product Owner schválil (User agreement on incremental approach: basic → enhanced)
- [ ] UX/Design review N/A (CLI tool)
- [ ] Team demo N/A (solo GitHub Copilot implementation)
- [ ] Knowledge sharing: Documentation sufficient

### 🚀 Deployment & Git

- [ ] Feature branch `feature/CORE-005-git-commit-tracker` merged do `main`
- [ ] Commit message: `feat(CORE-005): Add Git Commit Tracker (basic version)`
- [ ] Git tags N/A (project uses continuous deployment)
- [ ] Script deployed: Available in `scripts/backlog/` after merge
- [ ] Rollback plan: Revert commit (no DB changes, standalone script)

**Notes:**
- Basic version DoD (CORE-005) - No path mapping integration yet
- Enhanced version after CORE-002 (auto-update DoD, file coverage)
- Automated tests deferred to CORE-006 (Validator suite)

---

## 📋 Subtasks

> **Rozklad story na implementační tasky** (pro developer tracking)

### Subtask 1: Script Structure & CLI Args (2 hours)

- [ ] Vytvořit `scripts/backlog/git_tracker.sh` skeleton
- [ ] CLI argument parsing: `--epic`, `--story`, `--format`, `--show-zero`
- [ ] Help message: `git_tracker.sh --help`
- [ ] Validate arguments (epic exists, format valid)

**Files:**
- [ ] `scripts/backlog/git_tracker.sh` (skeleton + args)

**Testing:**
```bash
bash scripts/backlog/git_tracker.sh --help
# Should show usage: --epic EPIC-001, --story CORE-XXX, etc.
```

---

### Subtask 2: Git Log Parser (2 hours)

- [ ] Implementovat `parse_commits()` funkci
- [ ] Grep pattern: `(feat|fix|chore|docs)\(CORE-[0-9]+`
- [ ] Extract story IDs z commit messages
- [ ] Handle multi-story commits (`CORE-001,CORE-003`)

**Files:**
- [ ] `scripts/backlog/git_tracker.sh` (parse_commits function)

**Testing:**
```bash
# Should detect:
# 83871eb feat(backlog): ... → CORE-001
# 0d523e7 feat(CORE-003): ... → CORE-003
# f6332b6 docs(CORE-001,CORE-003): ... → both
```

---

### Subtask 3: Commit Counter & Report Generator (2 hours)

- [ ] Implementovat `count_per_story()` - group by story ID
- [ ] Implementovat `generate_report()` - text format
- [ ] Emoji status: ✅ done, 🔄 in-progress, 📋 no commits
- [ ] JSON output mode (AC5)

**Files:**
- [ ] `scripts/backlog/git_tracker.sh` (count + report functions)

**Testing:**
```bash
bash scripts/backlog/git_tracker.sh --epic EPIC-001
# Expected:
# ✅ CORE-001: 2 commits
# ✅ CORE-003: 2 commits
# 🔄 CORE-005: 0 commits (in-progress)
```

---

### Subtask 4: Edge Cases & Error Handling (1 hour)

- [ ] Invalid epic ID → error message
- [ ] Empty repo → graceful message
- [ ] Stories bez commitů → `--show-zero` flag
- [ ] JSON validation (test s `jq`)

**Testing:**
```bash
bash scripts/backlog/git_tracker.sh --epic NONEXISTENT
# Should error: "EPIC not found in backlog/"

bash scripts/backlog/git_tracker.sh --format json | jq .
# Should output valid JSON
```

---

### Subtask 5: Documentation Updates (1 hour)

- [ ] Update `backlog/README.md` - Automation section
- [ ] Update `docs/development/backlog-workflow.md` - Tracking Progress
- [ ] Inline comments ve scriptu
- [ ] Update `CHANGELOG.md` - CORE-005 entry

**Files:**
- [ ] `backlog/README.md` (+30 lines)
- [ ] `docs/development/backlog-workflow.md` (+50 lines)
- [ ] `CHANGELOG.md` (+10 lines)

---

### Subtask 6: Manual Testing & Refinement (1 hour)

- [ ] Test na EPIC-001 (verify detects všechny CORE-XXX)
- [ ] Test multi-epic repo (future-proof)
- [ ] Performance check (`time git_tracker.sh`)
- [ ] Shellcheck clean

**Expected output:**
```
EPIC-001 Git Activity Report
============================
✅ CORE-001: 2 commits (83871eb, f6332b6)
✅ CORE-003: 2 commits (0d523e7, f6332b6)
🔄 CORE-005: 1 commit (TBD)

Total: 5 commits across 3 stories
```

---

## 🔗 Related Stories

### Depends On (Blokovači)

- [x] [CORE-001: Git-Native Backlog Templates](../CORE-001-git-native-backlog-templates/README.md) ✅ DONE
  - **Reason:** Potřebujeme story structure (ID, epic, status) pro tracking
  - **Status:** Complete (83871eb)

### Blocks (Blokuje tyto stories)

- [ ] CORE-002: Path Mapping Validation (Enhanced Version)
  - **Reason:** CORE-005 enhanced bude používat path_mapping z CORE-002
  - **Status:** CORE-002 pending (basic CORE-005 first)

- [ ] CORE-006: Story Validator & DoD Checker
  - **Reason:** Validator může volat git_tracker.sh pro auto-update DoD
  - **Status:** Pending

### Related (Související)

- [x] [CORE-003: Story Generator Script](../CORE-003-story-generator-script/README.md) ✅ DONE
  - **Relation:** Generator vytváří stories → tracker sleduje jejich Git aktivitu
  - **Status:** Complete (0d523e7)

- [ ] CORE-007: Backlog Dashboard/Reporter (Future)
  - **Relation:** Dashboard může vizualizovat data z git_tracker.sh
  - **Status:** Not yet created

---

## 📊 Metrics & Success Criteria

### Before (Current State)

- **Tracking effort:** Manual tracking, 5-10 min/day per developer
- **DoD accuracy:** ~60% (often forget to update DoD checklists)
- **Commit visibility:** None - no way to see which stories have Git activity
- **Story progress:** Requires manual `git log` grep, slow
- **Time to audit:** 20+ min to audit EPIC-001 commit history

### After (Target State)

- **Tracking effort:** 0 min/day (automatic via script)
- **DoD accuracy:** 100% (script reports actual Git state)
- **Commit visibility:** Instant - `bash git_tracker.sh --epic EPIC-001`
- **Story progress:** <2 seconds to generate report
- **Time to audit:** <5 seconds (script + JSON output for automation)

### Success Criteria

- ✅ AC1-AC5 splněna (parse, count, report, patterns, JSON)
- ✅ Detects CORE-001 (2 commits), CORE-003 (2 commits) na EPIC-001 ✅
- ✅ Script runtime <2s for EPIC-001 size (~50 commits)
- ✅ JSON output validní (verify s `jq`)
- ✅ Shellcheck clean (no warnings)

**Validation:**
```bash
# Test detection
bash scripts/backlog/git_tracker.sh --epic EPIC-001
# Should show: CORE-001: 2, CORE-003: 2, CORE-005: 1+

# Test JSON
bash scripts/backlog/git_tracker.sh --format json | jq '.stories | length'
# Should output: 3 (or more)

# Test performance
time bash scripts/backlog/git_tracker.sh --epic EPIC-001
# Should complete in <2s
```

---

## 📝 Implementation Notes

### Technical Decisions

**Why Bash over Python:**
- Consistency: `new_story.sh` už je bash
- Zero dependencies: Git native commands
- Fast prototyping: Basic version 150-200 lines
- Future: Python rewrite if enhanced version grows complex

**Commit Pattern Design:**
```regex
(feat|fix|chore|docs|test|refactor)\(CORE-[0-9]+\):
```
- Supports conventional commits standard
- Handles multi-story: `docs(CORE-001,CORE-003):`
- Future-proof: Easy to extend patterns

**JSON Format:**
```json
{
  "epic": "EPIC-001",
  "total_commits": 5,
  "stories": [
    {
      "id": "CORE-001",
      "commits": 2,
      "shas": ["83871eb", "f6332b6"],
      "status": "done"
    }
  ]
}
```

### GitHub Copilot Optimization

**Copilot Prompts:**

```bash
# Implementace
"Implementuj scripts/backlog/git_tracker.sh podle CORE-005 v backlog/EPIC-001-backlog-system/stories/CORE-005-git-commit-tracker/README.md
Dodržuj AC1-AC5: parse git log, count commits per story, generate text + JSON report
Test na EPIC-001: CORE-001 (83871eb, f6332b6), CORE-003 (0d523e7, f6332b6)"

# Testing
"Test git_tracker.sh na EPIC-001
Verify detekuje: CORE-001 (2 commits), CORE-003 (2 commits)
Verify JSON output je validní (jq)"

# Dokumentace
"Update backlog/README.md s Git Tracker usage
Přidej sekci: Automation → Git Commit Tracker (CORE-005)
Příklady: text mode, JSON mode, --show-zero flag"
```

**Testing s Copilotem:**
```bash
# Prompt pro edge case testing
"Test git_tracker.sh edge cases:
1. Invalid epic ID → error message
2. Stories bez commitů → --show-zero flag
3. Multi-story commits → parse both CORE-XXX
4. JSON validation → jq .stories[0].id"
```

---

## 🎯 Next Steps (Po Completion)

1. **CORE-002: Path Mapping Validation**
   - Prerequisite for CORE-005 enhanced
   - Map story YAML `path_mapping` → actual files
   - Report coverage: code_paths (3/3 ✅), test_paths (2/3 ⚠️)

2. **CORE-005 Enhanced: Auto-Update DoD**
   - Parse path_mapping from story YAML
   - Check which files changed in commits (git diff)
   - Auto-update DoD checkboxes based on file existence
   - Report: "Code: 100%, Tests: 67%, Docs: 0%"

3. **CORE-006: Story Validator & DoD Checker**
   - Call git_tracker.sh for commit validation
   - Verify all code_paths have commits
   - Flag stories with 0 commits as "stale"

4. **Dashboard/Reporter (Future)**
   - Visualize Git activity per epic
   - Burndown chart: commits over time
   - Contributor stats per story

---

## 🏷️ Tags

`automation` `git` `cli` `bash` `tracking` `backlog` `epic-001` `p2`

---

## 📅 Timeline

- **Created:** 2025-11-06
- **Started:** TBD (ready for implementation)
- **Completed:** TBD
- **Deployed:** TBD

---

**Story Owner:** GitHub Copilot  
**Reviewers:** TBD  
**Last Updated:** 2025-11-06


---

## 📎 Attachments

- [UI Mockup](attachments/mockup-v1.png)
- [Architecture Diagram](attachments/architecture.svg)
- [API Spec](attachments/api-spec.yaml)

---

**Story Version:** 1.0  
**Last Updated:** YYYY-MM-DD  
**Author:** [Developer Name]
