# Git-Native Backlog Management

> **Lightweight, Markdown-First Workflow pro GitHub Copilot**

## 🎯 Co je to?

Backlog systém pro `core-platform`, který:
- ✅ **Git je source of truth** - žádná JIRA, vše v Markdown souborech
- ✅ **GitHub Copilot optimized** - stories jsou prompty pro generování kódu
- ✅ **Path mapping** - story zná svůj kód/testy/dokumentaci
- ✅ **Lightweight tooling** - jen Python skripty + Makefile
- ✅ **Auto-tracking** - Git commits automaticky updatují stories

## 📁 Struktura

```
backlog/
├── index.md                    # Dashboard (tento soubor)
├── README.md                   # Dokumentace (tento soubor)
├── templates/                  # Šablony pro stories/epics
│   ├── story.md               # Template pro User Story
│   ├── subtask.md             # Template pro Subtask
│   ├── epic.md                # Template pro Epic
│   └── README.md              # Návod na použití
├── EPIC-XXX-name/             # Adresář pro epic
│   ├── README.md              # Epic definice
│   └── stories/               # Stories v rámci epicu
│       └── {PREFIX}{NUM}-descriptive-name/  # Story adresář (prefix = zkratka epicu)
│           ├── README.md      # Story definice
│           ├── subtasks/      # Implementační tasky
│           │   ├── T1-task-name.md
│           │   └── T2-task-name.md
│           └── attachments/   # Screenshots, mockupy
└── scripts/                   # Automation tooling
    ├── new_story.sh           # Vytvoření nové story
    ├── validate.py            # Validace story struktury
    ├── git_tracker.py         # Mapování commits → stories
    └── report.py              # Progress dashboard
```

## 📝 Story Naming Convention

Stories se pojmenovávají podle patternu: **`{PREFIX}{NUM}-{descriptive-slug}`**

| EPIC | PREFIX | Příklad |
|------|--------|---------|
| EPIC-001: Backlog System | `BL` | `BL1-markdown-structure-templates` |
| EPIC-002: E2E Testing | `E2E` | `E2E1-playwright-test-framework-setup` |
| EPIC-003: Monitoring | `MON` | `MON1-prometheus-metrics-instrumentation` |
| EPIC-004: Reporting | `REP` | `REP1-cube-js-data-modeling` |
| EPIC-005: Metamodel | `META` | `META1-schema-diff-detection` |
| EPIC-006: Workflow | `WF` | `WF1-json-workflow-model` |
| EPIC-018: Platform Hardening | `PH` | `PH1-naming-standards-linting` |
| EPIC-007: Infrastructure Deployment | `INF` | `INF-001-template-generator` |
| EPIC-008: DMS | `DMS` | `DMS1-file-upload-download-service` |
| EPIC-010: Agile Work Management | `AWM` | `AWM1-work-item-model` |
| EPIC-011: n8n | `N8N` | `N8N1-n8n-platform-deployment` |
| EPIC-012: Vault | `VLT` | `VLT1-vault-skeleton-staging` |
| EPIC-014: UX/UI | `UX` | `UX1-mui-theme-foundation` |
| EPIC-016: Data UX | `DUX` | `DUX1-universal-data-view-engine` |
| EPIC-016: AI Metamodel Collaboration | `AI` | `AI-001-ai-metamodel-designer` |
| EPIC-016: AI Metamodel Collaboration (ML track) | `ML` | `ML-001-model-serving` |
| EPIC-017: Modular Architecture | `MOD/LIC/ADM/FWK` | `MOD-001-module-manifest-loader` |
| EPIC-020: Secure SDLC | `SECQ` | `SECQ1-sonarqube-quality-gates` |

**Proč popisné názvy?**
- ✅ **Okamžitá čitelnost**: `PH1-naming-conventions` vs. `S1`
- ✅ **Lepší navigace**: Vidíš co story dělá bez otevření README
- ✅ **Git history**: Commity jasně ukazují kontext (`feat(PH1): Add naming linter`)
- ✅ **Grep-friendly**: `grep -r "naming" backlog/` najde relevantní stories


## 🚀 Quick Start

### 1. Vytvoř novou story

```bash
# Interaktivní wizard
make backlog-new

# Nebo ručně
cp backlog/templates/story.md backlog/EPIC-018-platform-hardening/stories/PH11-new-feature/README.md
vim backlog/EPIC-018-platform-hardening/stories/PH11-new-feature/README.md
```

### 2. Naplň story s Copilot pomocí

```markdown
## 👤 Role / Potřeba / Benefit
Jako **developer** potřebuji **automatický export logů** abych **mohl debugovat produkční incidenty**.

## 📂 Implementation Mapping
code_paths:
  - backend/src/main/java/cz/muriel/core/monitoring/LogExporter.java
  - frontend/src/features/monitoring/LogExportDialog.tsx

test_paths:
  - backend/src/test/java/cz/muriel/core/monitoring/LogExporterTest.java
  - e2e/specs/monitoring/log-export.spec.ts

docs_paths:
  - docs/features/monitoring/log-export.md
```

### 3. Použij story pro generování kódu

**Příkaz pro Copilot v VS Code:**
```
Najdi story MON11 v backlog/EPIC-003-monitoring-observability/stories/MON11-log-export/README.md
a implementuj LogExporter podle definovaného path mappingu.
```

**Copilot vygeneruje:**
- `backend/src/main/java/cz/muriel/core/monitoring/LogExporter.java` (z code_paths)
- `backend/src/test/java/cz/muriel/core/monitoring/LogExporterTest.java` (z test_paths)
- `docs/features/monitoring/log-export.md` (z docs_paths)

### 4. Track progress

```bash
# Validace story před commitem
make backlog-validate STORY=MON11

# Git commit s story referencí
git commit -m "feat(MON11): Add log export functionality"

# Auto-update story checklist
make backlog-track
# → Přidá ✅ k DoD checklist položkám

# Progress report
make backlog-report
# → Zobrazí coverage (kolik files z path mappingu existuje)
```

## 📖 Workflow Krok za Krokem

### Fáze 1: Plánování

1. **Vytvoř Epic** (pro velké iniciativy):
   ```bash
   cp backlog/templates/epic.md backlog/EPIC-042-feature-name/README.md
   ```

2. **Rozděl na Stories**:
   ```bash
   # Pro každou user story
   mkdir -p backlog/EPIC-042-feature-name/stories/CORE-XXX-name
   cp backlog/templates/story.md backlog/EPIC-042-feature-name/stories/CORE-XXX-name/README.md
   ```

3. **Definuj DoR (Definition of Ready)**:
   - [ ] Story má jasný Role/Need/Benefit
   - [ ] Akceptační kritéria jsou měřitelná
   - [ ] Path mapping je vyplněný
   - [ ] Dependencies jsou identifikovány
   - [ ] Estimace je provedena

### Fáze 2: Implementace

4. **Vytvoř Git branch**:
   ```bash
   git checkout -b feature/CORE-XXX-short-name
   ```

5. **Implementuj s Copilot**:
   ```
   GitHub Copilot: Implementuj story CORE-XXX podle path mappingu v backlog/EPIC-042/.../CORE-XXX/README.md
   ```

6. **Checkuj DoD průběžně**:
   ```bash
   make backlog-validate STORY=CORE-XXX
   # → Ověří existence souborů z path mappingu
   # → Zkontroluje DoD checklist completeness
   ```

### Fáze 3: Review & Merge

7. **Validace před PR**:
   ```bash
   # DoD musí být 100% complete
   make backlog-validate STORY=CORE-XXX --strict

   # Coverage check
   make backlog-coverage STORY=CORE-XXX
   # code_paths: 3/3 ✅
   # test_paths: 2/3 ⚠️ (chybí E2E test)
   # docs_paths: 1/1 ✅
   ```

8. **Commit convention**:
   ```bash
   git commit -m "feat(XXX): Add feature according to CORE-XXX"
   git commit -m "test(XXX): Add E2E tests for CORE-XXX"
   git commit -m "docs(XXX): Document feature CORE-XXX"
   ```

9. **Git tracker auto-update**:
   ```bash
   # Po merge do main, git hook updatuje story
   # DoD checklist items se označí ✅ automaticky
   ```

## 🎨 Story Template Anatomy

### Klíčové Sekce

```markdown
## 👤 Role / Potřeba / Benefit
Jako [role] potřebuji [funkci] abych [benefit].

## ✅ Definition of Ready (DoR)
- [ ] Akceptační kritéria jsou definovaná
- [ ] Path mapping je vyplněný
- [ ] Dependencies jsou jasné

## 🎯 Akceptační kritéria (AC)
1. **AC1:** Given [context], When [action], Then [outcome]
2. **AC2:** ...

## 📂 Implementation Mapping
code_paths:
  - backend/src/...
  - frontend/src/...

test_paths:
  - backend/src/test/...
  - e2e/specs/...

docs_paths:
  - docs/...

## ✅ Definition of Done (DoD)
**Code Complete:**
- [ ] Kód implementovaný podle AC
- [ ] Code review provedeno

**Testing:**
- [ ] Unit testy přidány a pass
- [ ] E2E testy přidány a pass

**Documentation:**
- [ ] API dokumentace aktualizována
- [ ] User guide aktualizován

**Git:**
- [ ] Všechny commits referencují story ID
- [ ] Branch merged do main
```

### GitHub Copilot Integration

**Story je prompt pro Copilot:**
- `Role/Need/Benefit` → Co má kód dělat
- `Akceptační kritéria` → Test cases a edge cases
- `Implementation Mapping` → Kde psát kód
- `DoD` → Checklist pro úplnost

**Příklad Copilot použití:**
```
Prompt v VS Code:
"Implementuj CORE-042 Log Export feature podle:
- backlog/EPIC-001-backlog-system/stories/CORE-042-log-export/README.md
- Dodržuj path mapping (code_paths, test_paths)
- Splň všechna AC
- Vygeneruj kód + unit testy + E2E testy"

Copilot vygeneruje:
✅ LogExporter.java (z AC1-AC3)
✅ LogExporterTest.java (z AC test cases)
✅ log-export.spec.ts (E2E z AC4)
✅ log-export.md (docs z DoD)
```

## 🔧 Tooling Reference

### Makefile Targets

```bash
# Story lifecycle
make backlog-new             # Vytvoř novou story (interaktivní)
make backlog-validate        # Validuj všechny stories
make backlog-validate STORY=CORE-042  # Validuj jednu story

# Coverage & tracking
make backlog-coverage STORY=CORE-042  # Zjisti coverage (code/test/docs)
make backlog-track           # Mapuj commits → stories, updatuj DoD
make backlog-report          # Vygeneruj progress dashboard

# Epic management
make backlog-epic-new EPIC="Feature Name"  # Vytvoř nový epic
make backlog-epic-status EPIC=EPIC-042     # Status všech stories v epicu
```

### Python Scripts

```bash
# Ruční použití (bez Make)
python scripts/backlog/new_story.py --epic EPIC-042 --name "Feature Name"
python scripts/backlog/validate.py --story CORE-042 --strict
python scripts/backlog/git_tracker.py --update
python scripts/backlog/report.py --format markdown > backlog/index.md
```

---

## 🤖 Automation (CORE-003)

### Story Generator

**Automatické vytváření stories z template:**

```bash
# Interactive mode (DOPORUČENO)
make backlog-new

# Wizard prompts:
# Story Title: My New Feature
# Epic ID (default: EPIC-001-backlog-system): EPIC-002
# Priority - P1/P2/P3 (default: P1): P2
# Estimate (default: 1 day): 3 days
# Assignee (default: empty): GitHub Copilot

# Output:
# ✅ Created: backlog/EPIC-002/stories/CORE-005-my-new-feature/README.md
# ✅ Story ID: CORE-005
# ✅ Git branch: feature/CORE-005-my-new-feature

# Non-interactive (s parametry)
make backlog-new STORY="Feature Name" EPIC="EPIC-002" PRIORITY="P2" ESTIMATE="3 days"

# Pomocí scriptu přímo
bash scripts/backlog/new_story.sh --title "Feature Name" --epic "EPIC-002" --priority "P2"
```

**Co dělá Story Generator:**

1. **Automatic ID Assignment** - Najde next available CORE-XXX ID (max + 1)
2. **Template Copy** - Zkopíruje `backlog/templates/story.md` do nové lokace
3. **Placeholder Replacement** - Nahradí 7 placeholders:
   - `CORE-XXX` → `CORE-005` (auto-detected ID)
   - `EPIC-XXX-epic-name` → User input (např. `EPIC-002-auth`)
   - `[Story Title]` → User input (např. "OAuth2 Login")
   - `YYYY-MM-DD` → Today's date (např. `2025-11-06`)
   - `P1` → User priority (P1/P2/P3)
   - `X days` → User estimate (např. "3 days")
   - `assignee: ""` → User assignee (optional)
4. **Git Branch Creation** - Vytvoří a checkoutne `feature/CORE-XXX-title` branch
5. **Directory Structure** - Vytvoří `backlog/EPIC-XXX/stories/CORE-YYY-title/`

**Time Savings:** 5-10 min manual work → 30 sec automated ✅ (80-90% faster)

### Placeholder Table

| Placeholder | Replacement | Example |
|-------------|-------------|---------|
| `CORE-XXX` | Next available ID | `CORE-005` |
| `EPIC-XXX-epic-name` | User input epic | `EPIC-002-auth` |
| `[Story Title]` | User input title | "OAuth2 Login" |
| `YYYY-MM-DD` | Today's date | `2025-11-06` |
| `P1` | User priority | `P2` |
| `X days` | User estimate | "3 days" |
| `assignee: ""` | User assignee | "GitHub Copilot" |

### Help & Examples

```bash
# Show generator help
make backlog-help

# Output:
# 📋 Backlog Management (EPIC-001)
#
# Commands:
#   backlog-new          - Create new story (interactive)
#   backlog-new STORY='Feature Name' - Quick create with title
#
# Options:
#   STORY='Feature Name'       - Story title (required)
#   EPIC='EPIC-XXX'            - Epic ID (default: EPIC-001-backlog-system)
#   PRIORITY='P1|P2|P3'        - Priority (default: P1)
#   ESTIMATE='X days'          - Estimate (default: 1 day)
#   ASSIGNEE='Name'            - Assignee name
#
# Examples:
#   make backlog-new
#   make backlog-new STORY='Git Commit Tracker' EPIC='EPIC-001' PRIORITY='P2'
#   make backlog-new STORY='User Login' EPIC='EPIC-002-auth' PRIORITY='P1'
```

### Future Automation (Roadmap)

**CORE-006: Path Mapping Validator** ✅ **IMPLEMENTED**
```bash
# Validate single story
python3 scripts/backlog/path_validator.py --story CORE-005

# Output (text format):
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📊 Path Mapping Coverage: CORE-005
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# ✅ code_paths   1/1 (100%)
#    scripts/backlog/git_tracker.sh
# 
# ✅ test_paths   1/1 (100%)
#    scripts/backlog/test_integration.py
# 
# ✅ docs_paths   3/3 (100%)
#    backlog/README.md, docs/development/backlog-workflow.md, CHANGELOG.md
# 
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📈 Overall: 100% (5/5 paths exist)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Validate entire epic
python3 scripts/backlog/path_validator.py --epic EPIC-001

# JSON output for automation
python3 scripts/backlog/path_validator.py --story CORE-005 --format json | jq .
# → {"story_id":"CORE-005","coverage":{...},"overall":{"total":5,"exist":5,"percentage":100.0}}

# Features:
# - Validates code_paths, test_paths, docs_paths from YAML frontmatter
# - Supports glob patterns (backend/**/*.java)
# - Story-level and epic-level aggregation
# - Text (human-readable) and JSON (machine-readable) outputs
# - Performance: <5s for 100 stories (actual ~130ms) ✅
```

**CORE-005: Git Commit Tracker** ✅ **IMPLEMENTED**
```bash
# Track commits for an epic
bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system

# Output (text format):
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📊 Git Activity Report: EPIC-001-backlog-system
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# ✅ CORE-001:     1 commit(s) (f6332b6)
# ✅ CORE-003:     2 commit(s) (f6332b6,0d523e7)
# ✅ CORE-005:     1 commit(s) (7699f33)
# 
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📈 Summary: 4 commits across 3 stories
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Track specific story
bash scripts/backlog/git_tracker.sh --story CORE-003
# → Shows only CORE-003: 2 commits (f6332b6,0d523e7)

# JSON output for automation
bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system --format json | jq .
# → {"epic":"EPIC-001-backlog-system","total_commits":4,"stories":[...]}

# Show stories without commits
bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system --show-zero
# → Includes stories with 0 commits (marked with 📋)

# Performance: <0.3s for EPIC-001 (target <2s) ✅
```

**CORE-006: Story Validator** (TODO)
```bash
make backlog-validate STORY=CORE-042
# → Schema validation (all 8 sections present?)
# → DoR completeness (X/Y items checked)
# → DoD completeness
# → Pre-merge validation hook
```

## 📊 Metrics & Reporting

### Story Metrics

**Completeness:**
```bash
make backlog-validate STORY=CORE-042
# DoR: 5/5 ✅
# AC:  4/4 ✅
# DoD: 12/15 ⚠️ (chybí 3 položky)
```

**Coverage:**
```bash
make backlog-coverage STORY=CORE-042
# code_paths:  3/3 ✅ (100%)
# test_paths:  2/3 ⚠️ (67% - chybí E2E test)
# docs_paths:  1/1 ✅ (100%)
# Total:       6/7 ⚠️ (86%)
```

**Git Tracking:**
```bash
make backlog-track
# CORE-042: 8 commits, 6 files changed
# Commits referencující story: 100% ✅
# DoD auto-update: 3 položky marked ✅
```

### Epic Metrics

```bash
make backlog-epic-status EPIC=EPIC-042
# Stories:     5 total
# Completed:   2 (40%)
# In Progress: 2 (40%)
# Blocked:     1 (20%)
# Timeline:    On track ✅
```

## 🔍 Validation Rules

### Schema Validation

**Povinné sekce v Story:**
- `## 👤 Role / Potřeba / Benefit`
- `## ✅ Definition of Ready (DoR)`
- `## 🎯 Akceptační kritéria (AC)`
- `## 📂 Implementation Mapping`
- `## ✅ Definition of Done (DoD)`

**Validační kritéria:**
```python
# scripts/backlog/validate.py checks:
1. All required sections present
2. DoR checklist has items
3. At least 1 AC defined
4. Path mapping has code_paths
5. DoD has at least 10 items
6. All file paths valid (no typos)
7. Related stories exist
8. Story ID format correct (CORE-XXX)
```

### DoD Pre-Merge Check

```bash
# Git hook: pre-push
make backlog-validate STORY=$(git branch --show-current | grep -oE 'CORE-[0-9]+') --strict

# Fail push if:
# - DoD není 100% complete
# - Coverage < 80%
# - Povinné sekce chybí
```

## 💡 Best Practices

### 1. Story Sizing
- **Small:** 1-2 days (1 story)
- **Medium:** 3-5 days (split do 2-3 stories)
- **Large:** 1+ week (create Epic, split do 5+ stories)

### 2. Path Mapping
```yaml
# ✅ GOOD - specifické paths
code_paths:
  - backend/src/main/java/cz/muriel/core/groups/GroupController.java
  - frontend/src/components/Groups/EditGroupDialog.tsx

# ❌ BAD - příliš obecné
code_paths:
  - backend/src/**
  - frontend/src/**
```

### 3. Akceptační Kritéria
```markdown
# ✅ GOOD - měřitelné, testovatelné
**AC1:** Given uživatel klikne "Export Logs", When systém vygeneruje CSV, Then soubor obsahuje všechny logy za posledních 24h.

# ❌ BAD - vágní, netestovatelné
**AC1:** Uživatel může exportovat logy.
```

### 4. DoD Checklist
```markdown
# ✅ GOOD - akční položky s own-ership
- [ ] Unit testy přidány (coverage >80%)
- [ ] E2E test `log-export.spec.ts` pass
- [ ] API docs aktualizovány v `docs/api/monitoring.md`

# ❌ BAD - vágní, nemeasurable
- [ ] Testy napsány
- [ ] Docs hotové
```

### 5. Git Commit Messages
```bash
# ✅ GOOD - reference story ID
git commit -m "feat(042): Add log export API endpoint"
git commit -m "test(042): Add E2E tests for log export"

# ❌ BAD - bez story reference
git commit -m "Add export feature"
```

## 🚫 Co NENÍ v Backlogu

### Out of Scope (záměrně)
- ❌ **JIRA import/export** - Git je truth, JIRA optional
- ❌ **Web UI** - VS Code + GitHub je naše UI
- ❌ **External dependencies** - žádné DB, žádné API calls
- ❌ **AI analysis** - GitHub Copilot je naše AI
- ❌ **Složitá metadata** - jen Markdown + YAML frontmatter

### Proč tyto věci NEJSOU
- **JIRA:** "Git je source of truth" - nechceme duplicitu
- **Web UI:** VS Code + GitHub render je dost, žádný overhead
- **External deps:** Lightweight = rychlé, spolehlivé
- **AI analysis:** Copilot to dělá lépe (generování kódu z stories)
- **Complex metadata:** Markdown je čitelný pro lidi i Copilota

---

## � Story Quality Validation (CORE-008)

### Why Quality Gates?

**Problem:** Low-quality stories → ambiguous implementation → wasted time  
**Solution:** Automated validation BEFORE coding starts

### Quality Validator

```bash
# Validate story quality (0-100% score)
python3 scripts/backlog/story_validator.py --story CORE-012 --score

# Output:
# Schema:        40/40 ✅ (100%)
# DoR:           13/15 ⚠️  (87%)
# DoD:           12/15 ⚠️  (80%)
# AC Testability: 15/15 ✅ (100%)
# Path Mapping:  10/10 ✅
# YAML:           5/5 ✅
# ──────────────────────────────────
# TOTAL: 95/100 ✅ EXCELLENT
```

### Quality Scoring Formula

| Component | Points | What It Checks |
|-----------|--------|----------------|
| **Schema** | 40 | 8 required sections present |
| **DoR** | 15 | Definition of Ready completeness % |
| **DoD** | 15 | Definition of Done completeness % |
| **AC Testability** | 15 | Given/When/Then format + test mapping |
| **Path Mapping** | 10 | Code, test, docs paths defined |
| **YAML** | 5 | Valid frontmatter syntax |
| **TOTAL** | **100** | Overall story quality |

### Quality Levels

- **90-100%** = ✅ **EXCELLENT** - Ready to implement
- **70-89%** = ⚠️ **GOOD** - Can implement, minor issues
- **50-69%** = ⚠️ **FAIR** - Needs improvement
- **0-49%** = ❌ **POOR** - Cannot start, critical issues

### Pre-Implementation Check

```bash
# Enforce minimum 80% quality before starting work
python3 scripts/backlog/story_validator.py --story CORE-012 --score --min-score 80

# Exit code:
# 0 = passed (score >= 80)
# 1 = failed (score < 80) - FIX STORY FIRST!
```

### JSON Output for CI/CD

```bash
# Get quality data as JSON
python3 scripts/backlog/story_validator.py --story CORE-012 --score --format json | jq .

# Output:
{
  "story_id": "CORE-012",
  "quality_score": {
    "total": 95.0,
    "level": "EXCELLENT",
    "breakdown": {
      "schema": { "score": 40, "percentage": 100 },
      "dor": { "score": 13, "percentage": 87 },
      ...
    }
  }
}
```

### Complete Guide

**Detailed Documentation:** [Story Quality Guide](../docs/development/story-quality-guide.md)

**What It Validates:**
- ✅ All 8 required sections (YAML, Role, DoR, AC, Tests, Mapping, DoD, Subtasks)
- ✅ DoR/DoD completeness (minimum thresholds)
- ✅ AC format (Given/When/Then + test section)
- ✅ YAML frontmatter validity
- ✅ Path mapping presence

**Workflow:**
1. Create story: `make backlog-new`
2. Validate quality: `python3 scripts/backlog/story_validator.py --story CORE-XXX --score`
3. Fix issues if score < 70%
4. Start implementation when quality ≥ 70%

---

## �🐛 Bug Tracking & Regression Prevention

> **CORE-007 Feature:** Integrated bug tracking with regression test requirements

### Bug Template

**Location:** `backlog/templates/bug.md`

**Purpose:** Standardized bug reporting with full traceability to stories and commits.

**Key Features:**
- 🔗 **Traceability:** Links to story (`caused_by_story`) and commit (`caused_by_commit`)
- 🧪 **Regression Test:** MANDATORY test preventing bug recurrence
- 📊 **Severity Classification:** critical | high | medium | low
- ⏱️ **Timeline Tracking:** Time to detect, time to fix
- ✅ **Fix DoD:** Checklist before closing bug

### Creating a Bug Report

```bash
# Copy bug template
cp backlog/templates/bug.md backlog/bugs/BUG-042-email-validation.md

# Fill bug details
vim backlog/bugs/BUG-042-email-validation.md
```

**YAML Frontmatter Example:**
```yaml
id: BUG-042
type: bug
severity: high
status: reported
caused_by_story: CORE-003          # Which story introduced this?
caused_by_commit: abc1234          # Which commit caused it?
regression_test: e2e/specs/auth/login-email-alias.spec.ts
regression_test_status: not-written
```

### Bug Workflow

1. **Report Bug** → Fill bug template with reproduction steps
2. **Write Regression Test** → Test MUST reproduce bug (red phase)
3. **Fix Bug** → Implement fix (green phase)
4. **Verify Regression Test** → Test must prevent recurrence
5. **Close Bug** → Update status, link fix commit

### Regression Test Requirements

**Every bug MUST have regression test:**
- ✅ Test tagged with `@BUG-XXX @regression`
- ✅ Test reproduces bug before fix
- ✅ Test passes after fix
- ✅ Test prevents future recurrence

**Example:**
```typescript
// e2e/specs/auth/login-email-alias.spec.ts
test('login with + in email @BUG-042 @regression @CORE-003', async ({ page }) => {
  await page.goto('/login');
  await page.fill('input[name="email"]', 'user+test@example.com');
  await page.fill('input[name="password"]', 'ValidPassword123!');
  await page.click('button[type="submit"]');
  
  // Should succeed (was failing before fix)
  await expect(page).toHaveURL('/dashboard');
});
```

### Running Regression Tests

```bash
# Run all regression tests
npx playwright test --grep @regression

# Run specific bug tests
npx playwright test --grep @BUG-042

# Run regression tests for story
npx playwright test --grep "@regression @CORE-003"
```

### Bug Fix DoD

- [ ] Regression test written (@BUG-XXX @regression)
- [ ] Regression test passing
- [ ] Original AC from story still passing
- [ ] Bug verified by reporter
- [ ] Fix merged to main
- [ ] Bug status: closed

### Bug → Story → Commit Traceability

**Full Audit Trail:**
```
Story CORE-003: User Authentication
  ↓ introduced by
Commit abc1234: "feat(CORE-003): Add email validation"
  ↓ caused
Bug BUG-042: Email with + character fails
  ↓ fixed by
Commit def5678: "fix(BUG-042): Allow + in email validation"
  ↓ verified by
Regression Test: @BUG-042 @regression
```

**Track commits:**
```bash
# Find commits that introduced bug
bash scripts/backlog/git_tracker.sh CORE-003

# Example output:
# CORE-003: User Authentication
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Commits:
#   abc1234 - feat(CORE-003): Add email validation (2025-11-01)
#   def5678 - fix(BUG-042): Allow + in email validation (2025-11-06)
```

---

## 🧪 Test-First Development

> **CORE-007 Feature:** AC → Test mapping with coverage validation

### AC to Test Mapping

**Every story MUST map AC to tests** in `README.md`:

```markdown
### AC1: Export CSV as admin → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Unit Test** | `utils/__tests__/csv.test.ts` | ✅ Passing | 100% | 2025-11-06 | - |
| **Integration Test** | `backend/.../ExportServiceTest.java` | ✅ Passing | 100% | 2025-11-06 | - |
| **E2E Test** | `e2e/specs/export/export-data.spec.ts` | ✅ Passing | 100% | 2025-11-06 | `@CORE-012 @AC1` |
```

### Test Validator

**Validate test coverage before merge:**

```bash
# Validate single story
bash scripts/backlog/test_validator.sh --story CORE-012

# Output:
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📊 Test Coverage Report: CORE-012
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# AC1: Export CSV as admin
#   ✅ Unit Test: utils/__tests__/csv.test.ts
#   ✅ Integration Test: backend/.../ExportServiceTest.java
#   ✅ E2E Test: e2e/specs/export/export-data.spec.ts
#   Coverage: 100% (3/3 test types) ✅
# 
# Overall Coverage: 100% ✅ COMPLETE
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

# Validate epic
bash scripts/backlog/test_validator.sh --epic EPIC-002

# Require minimum coverage
bash scripts/backlog/test_validator.sh --story CORE-012 --min-coverage 80

# JSON output for CI/CD
bash scripts/backlog/test_validator.sh --story CORE-012 --format json | jq .
```

### Test-First Workflow

**Red-Green-Refactor Cycle:**

1. **RED:** Write failing test (define requirement)
2. **GREEN:** Implement minimum code to pass
3. **CLEAN:** Refactor without breaking tests

**Complete Guide:** [Test-Driven Workflow](../docs/development/test-driven-workflow.md)

### DoD Test Requirements

**Story cannot be merged without:**
- [ ] AC to Test Mapping filled (min 1 test per AC)
- [ ] Test-first workflow followed (tests written BEFORE code)
- [ ] All tests passing (CI/CD green)
- [ ] Test validator: 100% AC coverage
- [ ] Regression tests for bugs (@BUG-XXX @regression)

---

## 📚 Reference Documentation

### Templates
- [Story Template Guide](templates/README.md) - Jak psát stories
- [Epic Template Guide](templates/README.md#epic-template) - Jak strukturovat epics

### Guides
- [Developer Workflow](../docs/development/backlog-workflow.md) - Denní použití
- [Copilot Integration](../docs/development/copilot-backlog.md) - Jak používat s Copilotem
- [Git Tracking Setup](../docs/development/git-tracking.md) - Automatizace

### Examples
- [EPIC-001](EPIC-001-backlog-system/README.md) - Meta-epic (backlog system itself)
- [CORE-001](EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md) - Meta-story (template creation)

## 🎓 Learning Resources

### Tutorials
1. **Quick Start:** [5-Minute Tutorial](tutorials/quickstart.md)
2. **Path Mapping:** [Path Mapping Guide](tutorials/path-mapping.md)
3. **Copilot Usage:** [Copilot Best Practices](tutorials/copilot-usage.md)

### Video Walkthroughs (budoucí)
- Creating Your First Story
- Using Path Mapping with Copilot
- Git Tracking & Auto-Update

## 🤝 Contributing

### Zlepšení Backlog Systému
1. Vytvoř story v `EPIC-001-backlog-system/stories/`
2. Implementuj podle CORE-001 pattern
3. Aktualizuj templates pokud potřeba
4. Přidej do `backlog/CHANGELOG.md`

### Feedback
- 🐛 Bugy: Vytvoř story v EPIC-001
- 💡 Nápady: Diskuze v PR
- 📖 Docs: Přímo edituj Markdown, vytvoř PR

---

**Version:** 1.0.0  
**Last Updated:** 2025-11-06  
**Maintained By:** Core Platform Team
