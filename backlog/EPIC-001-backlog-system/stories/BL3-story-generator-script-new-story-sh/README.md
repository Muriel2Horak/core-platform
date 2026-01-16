---
id: CORE-003
epic: EPIC-001-backlog-system
title: "Story Generator Script (new_story.sh)"
priority: P1  # P1 (Must Have) | P2 (Should Have) | P3 (Nice to Have)
status: ready
assignee: "GitHub Copilot"
created: 2025-11-06
updated: 2025-11-06
estimate: "1 day"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-001-backlog-system/stories/BL3-story-generator-script-new-story-sh/README.md
    - backlog/EPIC-001-backlog-system/README.md
---


# CORE-003: Story Generator Script (new_story.sh)

> **Epic:** [EPIC-001: Backlog System](../../README.md)  
> **Priority:** P1 | **Status:** 🟡 **READY**| **Estimate:** 1 day

## 👤 Role / Potřeba / Benefit

Jako **developer** potřebuji **automaticky vytvářet nové stories z template** abych **ušetřil čas a měl konzistentní strukturu**.

**Kontext:**
Momentálně musím manuálně:
1. Vytvořit directory (`mkdir -p backlog/EPIC-XXX/stories/CORE-YYY-name`)
2. Kopírovat template (`cp templates/story.md ...`)
3. Editovat metadata (ID, title, dates, atd.)
4. Replacovat placeholders (CORE-XXX → real ID)
5. Vytvořit Git branch (`git checkout -b feature/CORE-YYY-name`)

To je 5-10 minut repetitivní práce pro každou story.

**Value Proposition:**
- **Před:** 5-10 minut manuální setup každé story
- **Po:** 30 sekund s interaktivním wizardem
- **ROI:** 80-90% time saving, 100% konzistence

---

## ✅ Definition of Ready (DoR)

- [x] Role/Need/Benefit jasně definovaný ✅
- [x] Všechna AC měřitelná a testovatelná ✅
- [x] Implementation Mapping vyplněný ✅
- [x] Dependencies: Závisí na CORE-001 (templates exist) ✅ DONE
- [x] Technical approach: Bash script s interaktivními prompty ✅
- [x] Estimace: 1 day (8 hours) ✅

---

## 🎯 Akceptační kritéria (AC)

### AC1: Interaktivní Story Creation

**Given** developer chce vytvořit novou story  
**When** spustí `bash scripts/backlog/new_story.sh` (nebo `make backlog-new`)  
**Then** script se zeptá na:
- Story title (povinné)
- Epic ID (default: EPIC-001)
- Priority (default: P1)
- Estimate (default: "1 day")

**And** vytvoří:
- Directory: `backlog/EPIC-XXX-epic-name/stories/CORE-YYY-title/`
- File: `backlog/EPIC-XXX-epic-name/stories/CORE-YYY-title/README.md`
- Zkopírovaný template s replacenými placeholders

**Test:**
```bash
# Test script execution
bash scripts/backlog/new_story.sh

# Input prompts:
# Title: Test Feature
# Epic: EPIC-001-backlog-system
# Priority: P2
# Estimate: 0.5 days

# Expected output:
# ✅ Created: backlog/EPIC-001-backlog-system/stories/CORE-006-test-feature/README.md
# ✅ Story ID: CORE-006
# ✅ Git branch: feature/CORE-006-test-feature
```

---

### AC2: Automatická ID Assignment

**Given** existující stories mají IDs CORE-001, CORE-002, CORE-003  
**When** vytvářím novou story  
**Then** script automaticky přiřadí next available ID (CORE-004)

**Test:**
```bash
# Find highest existing ID
ls backlog/EPIC-*/stories/CORE-* | sort | tail -1
# CORE-003-story-generator

# New story gets CORE-004
bash scripts/backlog/new_story.sh
# Output: ✅ Assigned ID: CORE-004
```

---

### AC3: Template Placeholder Replacement

**Given** story template obsahuje placeholders  
**When** script vytváří novou story  
**Then** nahradí všechny placeholders real values:

| Placeholder | Replacement | Example |
|-------------|-------------|---------|
| `CORE-XXX` | Next ID | `CORE-004` |
| `EPIC-XXX-epic-name` | User input | `EPIC-001-backlog-system` |
| `[Story Title]` | User input | `My New Feature` |
| `YYYY-MM-DD` | Today's date | `2025-11-06` |
| `X days` | User estimate | `2 days` |
| `P1` | User priority | `P2` |

**Test:**
```bash
# Verify placeholders replaced
grep "CORE-XXX" backlog/EPIC-001/.../CORE-004-.../README.md
# Expected: No matches (all replaced)

grep "CORE-004" backlog/EPIC-001/.../CORE-004-.../README.md
# Expected: Multiple matches (ID replaced correctly)
```

---

### AC4: Git Branch Creation

**Given** nová story vytvořena  
**When** script dokončí vytváření  
**Then** automaticky vytvoří Git branch: `feature/CORE-XXX-short-title`

**And** přepne na tento branch

**Test:**
```bash
bash scripts/backlog/new_story.sh
# Input: Title="User Export Feature"

# Check branch created
git branch | grep "feature/CORE-004-user-export"
# Expected: * feature/CORE-004-user-export (active)
```

---

### AC5: Makefile Integration

**Given** Makefile existuje  
**When** spustím `make backlog-new`  
**Then** zavolá `bash scripts/backlog/new_story.sh`

**Optional args:**
```bash
make backlog-new STORY="Feature Name"
make backlog-new STORY="Feature" EPIC="EPIC-002" PRIORITY="P2"
```

**Test:**
```bash
make backlog-new STORY="Test Feature"
# Should call script with pre-filled title
```
- Data encrypted at rest and in transit
- User permissions checked before every operation

---

## 📂 Implementation Mapping

> **Účel:** Mapování story → kód/testy/dokumentace pro GitHub Copilot a git tracking

### Code Paths
Soubory které budou vytvořeny/změněny při implementaci:

```yaml
code_paths:
  - scripts/backlog/new_story.sh           # Main bash script (interactive wizard)
  - Makefile                                # Add 'backlog-new' target
```

**Copilot Prompt:**
```
Implementuj CORE-003 podle:
- backlog/EPIC-001-backlog-system/stories/CORE-003-story-generator/README.md
- Vytvoř bash script scripts/backlog/new_story.sh
- Dodržuj všechna AC (AC1-AC5)
- Interactive prompts, template copy, placeholder replacement, Git branch creation
```

---

### Test Paths
Testy které budou vytvořeny:

```yaml
test_paths:
  - scripts/backlog/test_new_story.sh     # Bash unit tests for script
  - .github/workflows/backlog-test.yml    # CI test for story generator
```

**Test Coverage Očekáváno:**
- Manual test: Run script and verify output ✅
- Automated test: Bash test script checks placeholders replaced
- CI test: GitHub Actions runs script in clean env

---

### Docs Paths
Dokumentace která bude aktualizována:

```yaml
docs_paths:
  - backlog/README.md                      # Add usage example for `make backlog-new`
  - backlog/templates/README.md            # Add automation section
  - docs/development/backlog-workflow.md   # Developer guide update
```

---

## ✅ Definition of Done (DoD)

Tato story je COMPLETE pokud:

### 📝 Code Complete
- [x] Všechny soubory z `code_paths` jsou implementovány (scripts/backlog/new_story.sh ✅, Makefile ✅)
- [x] Kód splňuje všechna Akceptační kritéria (AC1-AC5 ✅)
- [ ] Code review provedeno (min. 1 approver) - PENDING
- [x] Žádné compiler warnings nebo lint errors (bash script clean ✅)
- [x] Code style guide dodržen (shellcheck compliance ✅)

### 🧪 Testing
- [x] Manual testing provedeno (CORE-004 successfully created ✅)
- [x] Všechna AC ověřena:
  - [x] AC1: Interactive wizard WORKS ✅
  - [x] AC2: Auto ID assignment (CORE-004 assigned correctly) ✅
  - [x] AC3: Placeholder replacement (all 7 replacements verified) ✅
  - [x] AC4: Git branch creation (feature/CORE-004-git-commit-tracker ✅)
  - [x] AC5: Makefile integration (make backlog-new target added) ✅
- [ ] Unit testy napsány (scripts/backlog/test_new_story.sh) - DEFERRED to CORE-006
- [ ] Integration testy (CI/CD validation) - DEFERRED to CORE-006
- [ ] E2E testy (backlog workflow) - DEFERRED to CORE-006

### 📚 Documentation
- [ ] backlog/README.md updated with automation section - IN PROGRESS
- [ ] backlog/templates/README.md updated with generator usage - IN PROGRESS
- [x] Inline code comments přidány (bash script fully documented ✅)
- [x] CORE-003 story self-documented (this file ✅)
- [ ] CHANGELOG.md entry - PENDING

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

### Subtask 1: Bash Script Core Functions (2 hours) ✅ DONE
- [x] Create scripts/backlog/new_story.sh file
- [x] Implement find_next_story_id() function (find max CORE-XXX + 1)
- [x] Implement sanitize_title() function (lowercase, hyphens, remove special chars)
- [x] Implement prompt_user_input() function (interactive prompts)
- [x] Implement validate_epic() function (check epic directory exists)
- [x] Add colored output helpers (print_success, print_error, print_info)

### Subtask 2: Template Copy & Placeholder Replacement (3 hours) ✅ DONE
- [x] Implement create_story() function
- [x] Copy template to story directory
- [x] Replace YAML frontmatter placeholders (id, epic, title, priority, assignee, dates, estimate)
- [x] Replace Markdown content placeholders (heading, references)
- [x] Handle edge cases (existing directory, missing template)

### Subtask 3: Git Integration (1 hour) ✅ DONE
- [x] Implement create_git_branch() function
- [x] Generate branch name: feature/CORE-XXX-title-slug
- [x] Check if branch already exists (handle gracefully)
- [x] Checkout new branch automatically

### Subtask 4: Command-Line Interface & Makefile (2 hours) ✅ DONE
- [x] Add argument parsing (--title, --epic, --priority, --estimate, --assignee)
- [x] Add --help flag with usage documentation
- [x] Set executable permissions (chmod +x)
- [x] Add Makefile target: backlog-new
- [x] Add backlog-help target with examples
- [x] Update main Makefile help to show backlog commands

### Subtask 5: Testing & Validation (2 hours) ✅ DONE
- [x] Manual test: Create CORE-004 story
- [x] Verify placeholders replaced correctly
- [x] Verify directory structure created
- [x] Verify Git branch created and checked out
- [x] Test both interactive mode and CLI args mode
- [ ] Create automated test script: scripts/backlog/test_new_story.sh (DEFERRED)

### Subtask 6: Documentation & Story Completion (1 hour) ⏳ IN PROGRESS
- [x] Fill CORE-003 Subtasks section (this section)
- [ ] Update backlog/README.md with automation section
- [ ] Update backlog/templates/README.md with generator usage
- [ ] Mark CORE-003 DoD items as complete
- [ ] Git commit CORE-003 + script
- [ ] Close story, update status to done

**Total Estimate:** 11 hours (~1.5 days)  
**Actual:** ~11 hours (matching estimate)
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
