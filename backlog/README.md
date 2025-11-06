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
│       └── CORE-XXX-name/     # Adresář pro story
│           ├── README.md      # Story definice
│           ├── subtasks/      # Implementační tasky
│           └── attachments/   # Screenshots, mockupy
└── scripts/                   # Automation tooling
    ├── new_story.sh           # Vytvoření nové story
    ├── validate.py            # Validace story struktury
    ├── git_tracker.py         # Mapování commits → stories
    └── report.py              # Progress dashboard
```

## 🚀 Quick Start

### 1. Vytvoř novou story

```bash
# Interaktivní wizard
make backlog-new

# Nebo ručně
cp backlog/templates/story.md backlog/EPIC-001-backlog-system/stories/CORE-042-my-feature/README.md
vim backlog/EPIC-001-backlog-system/stories/CORE-042-my-feature/README.md
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
Najdi story CORE-042 v backlog/EPIC-001-backlog-system/stories/CORE-042-my-feature/README.md
a implementuj LogExporter podle definovaného path mappingu.
```

**Copilot vygeneruje:**
- `backend/src/main/java/cz/muriel/core/monitoring/LogExporter.java` (z code_paths)
- `backend/src/test/java/cz/muriel/core/monitoring/LogExporterTest.java` (z test_paths)
- `docs/features/monitoring/log-export.md` (z docs_paths)

### 4. Track progress

```bash
# Validace story před commitem
make backlog-validate STORY=CORE-042

# Git commit s story referencí
git commit -m "feat(042): Add log export functionality"

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

**CORE-002: Path Mapping Validator** (TODO)
```bash
make backlog-coverage STORY=CORE-042
# → Checks if files from code_paths/test_paths/docs_paths exist
# → Reports: "code_paths: 3/3 ✅, test_paths: 2/3 ⚠️"
```

**CORE-004: Git Commit Tracker** (TODO)
```bash
make backlog-track
# → Parses git log for commits referencing CORE-XXX
# → Auto-updates DoD checkboxes based on file changes
# → Maps commits to stories via path mapping
```

**CORE-005: Story Validator** (TODO)
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
