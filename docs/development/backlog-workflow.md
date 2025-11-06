# Backlog Workflow Guide

> **Developer guide pro práci s Git-native backlog systémem**

## 📋 Přehled

Core Platform používá **Git-native backlog management** systém - všechny User Stories, Epics a Subtasks jsou verzované Markdown soubory v repository. Žádná závislost na JIRA nebo externích nástrojích.

**Výhody:**
- ✅ Verzování stories stejně jako kód (Git history)
- ✅ Code ↔ Story linking přes file paths
- ✅ GitHub Copilot optimalizované (stories = prompts)
- ✅ Offline práce možná
- ✅ Markdown = univerzální formát

---

## 🚀 Quick Start

### Vytvoření nové User Story

**Automaticky (DOPORUČENO):**
```bash
# Interaktivní wizard
make backlog-new

# Nebo s parametry
make backlog-new STORY="Feature Name" EPIC="EPIC-001-backlog-system" PRIORITY="P1"
```

**Manuálně:**
```bash
# 1. Copy template
cp backlog/templates/story.md backlog/EPIC-XXX/stories/CORE-YYY-feature-name/README.md

# 2. Edit metadata (id, title, epic, priority)
vim backlog/EPIC-XXX/stories/CORE-YYY-feature-name/README.md

# 3. Create feature branch
git checkout -b feature/CORE-YYY-feature-name
```

### Workflow krok za krokem

```mermaid
graph LR
    A[Create Story] --> B[Fill DoR]
    B --> C[Implement]
    C --> D[Fill DoD]
    D --> E[Review]
    E --> F[Merge]
```

---

## 📂 Directory Structure

```
backlog/
├── README.md                          # System overview
├── index.md                           # Dashboard (metrics, active stories)
├── templates/                         # Story/Epic/Subtask templates
│   ├── story.md                      # User Story template (485 lines)
│   ├── subtask.md                    # Subtask template (245 lines)
│   ├── epic.md                       # Epic template (445 lines)
│   └── README.md                     # Template usage guide
│
└── EPIC-XXX-epic-name/               # Epic directory
    ├── README.md                     # Epic definition
    └── stories/
        └── CORE-YYY-story-name/      # Story directory
            └── README.md             # Story file
```

---

## ✍️ Writing User Stories

### Story Template Sections

Každá User Story má **8 povinných sekcí**:

#### 1. **YAML Frontmatter** (Metadata)
```yaml
---
id: CORE-XXX
epic: EPIC-XXX-epic-name
title: "Short Story Title"
priority: P1  # P1 (Must Have) | P2 (Should Have) | P3 (Nice to Have)
status: ready  # ready | in-progress | blocked | done
assignee: ""
created: 2025-11-06
updated: 2025-11-06
estimate: "X days"
---
```

#### 2. **Role / Potřeba / Benefit**
```markdown
Jako **[role]** potřebuji **[funkci/feature]** abych **[business benefit]**.
```

**Příklad:**
> Jako **developer** potřebuji **automaticky vytvářet stories z template** abych **ušetřil 5-10 minut ruční práce**.

#### 3. **Definition of Ready (DoR)**
6-7 checklistů, co musí být splněno PŘED implementací:
- [ ] Role/Need/Benefit jasně definovaný
- [ ] AC měřitelná a testovatelná
- [ ] Implementation Mapping vyplněný
- [ ] Dependencies identified
- [ ] Technical approach diskutovaný

#### 4. **Acceptance Criteria (AC)**
Testovatelná kritéria ve formátu Given/When/Then:

```markdown
### AC1: Feature X works
**Given** user is logged in  
**When** user clicks button  
**Then** modal opens with form

**Test:**
```bash
curl -X POST /api/feature -d '...'
# Expected: 200 OK, response contains "..."
```
```

#### 5. **Implementation Mapping**
Linkuje story → code → testy → docs:

```yaml
code_paths:
  - backend/src/main/java/cz/muriel/core/MyService.java
  - frontend/src/components/MyComponent.tsx

test_paths:
  - backend/src/test/java/cz/muriel/core/MyServiceTest.java
  - e2e/specs/my-feature.spec.ts

docs_paths:
  - docs/api/my-feature.md
```

**Proč?** GitHub Copilot vidí kde generovat kód!

#### 6. **Definition of Done (DoD)**
20+ checklistů rozdělených do kategorií:
- 📝 Code Complete
- 🧪 Testing
- 📚 Documentation
- 🔒 Quality & Security
- 🤝 Team Alignment
- 🚀 Deployment & Git

#### 7. **Subtasks**
Breakdown story na implementační tasky (2-8 hodin každý):

```markdown
### Subtask 1: Setup (2 hours)
- [ ] Create Git branch
- [ ] Setup boilerplate
- [ ] Initial tests

**Files:** `backend/src/.../Service.java`
```

#### 8. **Related Stories**
```markdown
### Dependencies (Blocked By)
- **CORE-001** - Needs templates

### Blocks
- **CORE-004** - Git Tracker needs story structure

### Related
- **CORE-002** - Path Mapping
```

---

## 🤖 GitHub Copilot Integration

### Prompt z Story

Story je **optimalizovaná jako Copilot prompt**:

```markdown
Copilot Prompt:
"Implementuj CORE-003 podle specifikace v:
backlog/EPIC-001-backlog-system/stories/CORE-003-story-generator/README.md

Vytvoř bash script scripts/backlog/new_story.sh který:
- AC1: Interaktivně se ptá na title, epic, priority
- AC2: Najde next available CORE-XXX ID
- AC3: Zkopíruje template a nahradí placeholders
- AC4: Vytvoří git branch feature/CORE-XXX-title

Použij funkce podle Implementation Mapping v story."
```

### Code Generation Workflow

1. **Write Story** (define AC, DoR, path mapping)
2. **Ask Copilot**: "Implementuj CORE-XXX podle story"
3. **Copilot reads** story → generates code in correct paths
4. **Developer reviews** → runs tests → commits

---

## 🔄 Story Lifecycle

### 1. Ready → In Progress

```bash
# 1. Přesuň story do in-progress
vim backlog/EPIC-XXX/stories/CORE-YYY/README.md
# status: ready → in-progress

# 2. Create feature branch
git checkout -b feature/CORE-YYY-name

# 3. Commit story status change
git add backlog/
git commit -m "chore(CORE-YYY): Mark story as in-progress"
```

### 2. Implementation

```bash
# Implement podle AC a path mapping
vim backend/src/.../Service.java

# Reference story in commits
git commit -m "feat(CORE-YYY): Implement AC1 - Feature X"
```

### 3. Testing & DoD

```bash
# Mark DoD items as done
vim backlog/EPIC-XXX/stories/CORE-YYY/README.md
# - [x] Unit tests written
# - [x] Code review done

# Commit DoD updates
git commit -m "chore(CORE-YYY): Update DoD checklist"
```

### 4. Done → Merge

```bash
# 1. Mark story as done
# status: in-progress → done

# 2. Push branch
git push origin feature/CORE-YYY-name

# 3. Create PR (link story in description)
gh pr create --title "feat(CORE-YYY): Story Title" \
  --body "Resolves CORE-YYY\n\nSee: backlog/EPIC-XXX/stories/CORE-YYY/README.md"

# 4. After review → merge to main
```

---

## 🧪 Testing Stories

### Template Validation

```bash
# Check story má všechny required sections
grep -E "^## " backlog/EPIC-XXX/stories/CORE-YYY/README.md

# Expected output (8 sections):
# ## 👤 Role / Potřeba / Benefit
# ## ✅ Definition of Ready (DoR)
# ## 🎯 Acceptance Criteria (AC)
# ## 📂 Implementation Mapping
# ## ✅ Definition of Done (DoD)
# ## 📋 Subtasks
# ## 🔗 Related Stories
# ## 📊 Metrics & Success Criteria
```

### DoR Check (Before Starting)

```bash
# All DoR items checked?
grep -A10 "## ✅ Definition of Ready" backlog/EPIC-XXX/stories/CORE-YYY/README.md | grep "\[ \]"
# If output = empty → DoR complete ✅
# If output has items → DoR incomplete ❌
```

### DoD Check (Before Merge)

```bash
# Count unchecked DoD items
grep -A50 "## ✅ Definition of Done" backlog/EPIC-XXX/stories/CORE-YYY/README.md | grep -c "\[ \]"
# If 0 → DoD complete ✅
# If >0 → DoD incomplete ❌
```

---

## 📊 Reporting & Metrics

### Active Stories

```bash
# Find all in-progress stories
grep -r "status: in-progress" backlog/EPIC-*/stories/*/README.md
```

### Blocked Stories

```bash
# Find blocked stories
grep -r "status: blocked" backlog/EPIC-*/stories/*/README.md
```

### Epic Progress

```bash
# Count done stories in epic
grep -r "status: done" backlog/EPIC-001-backlog-system/stories/*/README.md | wc -l

# Count total stories
ls -d backlog/EPIC-001-backlog-system/stories/*/ | wc -l
```

### Story Velocity

```bash
# Stories completed per week
git log --since="1 week ago" --grep="status.*done" --oneline | wc -l
```

---

## 🛠️ Automation Tools

### Story Generator (CORE-003)

```bash
# Create new story automatically
make backlog-new

# Or with args
make backlog-new STORY="My Feature" EPIC="EPIC-002" PRIORITY="P2" ESTIMATE="3 days"
```

**What it does:**
1. Finds next available CORE-XXX ID
2. Creates directory: `backlog/EPIC-XXX/stories/CORE-YYY-name/`
3. Copies template
4. Replaces placeholders (ID, title, dates, priority)
5. Creates Git branch: `feature/CORE-YYY-name`

**Time savings:** 5-10 min → 30 sec (80-90%)

### Path Mapping Validator (CORE-002 - TODO)

```bash
# Validate code_paths exist
make backlog-validate STORY=CORE-XXX

# Expected output:
# ✅ code_paths: 3/3 files exist
# ⚠️ test_paths: 2/3 files missing
# ✅ docs_paths: 1/1 files exist
```

### Git Commit Tracker (CORE-004 - TODO)

```bash
# Link commits to stories
make backlog-tracker

# Expected: Updates DoD checkboxes based on git commits
# - [x] All files from code_paths committed
# - [x] Tests written (test_paths exist)
```

---

## 🎯 Best Practices

### ✅ DO

1. **Fill DoR before starting** - Prevents rework later
2. **Reference story ID in commits** - `feat(CORE-XXX): ...`
3. **Update DoD as you go** - Don't wait until end
4. **Keep stories small** - 1-3 days max (split larger work)
5. **Use path mapping** - Helps Copilot generate code
6. **Link related stories** - Track dependencies

### ❌ DON'T

1. **Skip DoR** - Leads to unclear requirements
2. **Commit without story ID** - Breaks traceability
3. **Leave DoD empty until end** - Hard to track progress
4. **Create huge stories** - Split into subtasks or separate stories
5. **Hardcode paths in AC** - Use variables/placeholders
6. **Forget to link dependencies** - Causes blocked work

---

## 🔗 Links & Resources

### Internal Docs
- [Backlog System Overview](../../backlog/README.md)
- [Template Usage Guide](../../backlog/templates/README.md)
- [Epic Dashboard](../../backlog/index.md)

### Templates
- [Story Template](../../backlog/templates/story.md)
- [Subtask Template](../../backlog/templates/subtask.md)
- [Epic Template](../../backlog/templates/epic.md)

### Examples
- [EPIC-001: Backlog System](../../backlog/EPIC-001-backlog-system/README.md)
- [CORE-001: Templates](../../backlog/EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md)
- [CORE-003: Story Generator](../../backlog/EPIC-001-backlog-system/stories/CORE-003-story-generator/README.md)

### External
- [GitHub Markdown Spec](https://github.github.com/gfm/)
- [GitHub Copilot Best Practices](https://github.blog/2023-06-20-how-to-write-better-prompts-for-github-copilot/)

---

## 💡 FAQ

### Q: Jak přidat novou Epic?

```bash
# 1. Copy epic template
cp backlog/templates/epic.md backlog/EPIC-002-new-epic/README.md

# 2. Create stories directory
mkdir -p backlog/EPIC-002-new-epic/stories/

# 3. Fill epic definition (goal, scope, timeline)
vim backlog/EPIC-002-new-epic/README.md

# 4. Commit
git add backlog/EPIC-002-new-epic/
git commit -m "epic(EPIC-002): Add new epic for ..."
```

### Q: Jak zjistit next story ID?

```bash
# Manual
ls backlog/EPIC-*/stories/ | grep -oE 'CORE-[0-9]+' | sort -V | tail -1

# Automatic (story generator finds it)
make backlog-new
```

### Q: Co když story blokuje jinou story?

1. Přidej do **Dependencies** sekce:
```markdown
### Blocks
- **CORE-XXX** - Feature Y depends on this
```

2. Ve blokované story přidej:
```markdown
### Dependencies (Blocked By)
- **CORE-YYY** - Needs Feature X first
```

### Q: Jak trackovat progress epic?

V epic README.md:

```markdown
## 📊 Progress

- ✅ CORE-001: Templates (Done)
- ✅ CORE-003: Story Generator (Done)
- 🔄 CORE-002: Path Mapping (In Progress)
- 📋 CORE-004: Git Tracker (Todo)
- 📋 CORE-005: Validator (Todo)

**Overall:** 2/5 stories done (40%)
```

### Q: Mohu story editovat po merge?

✅ **ANO** - Story je živý dokument:
- Update DoD když najdeš bugs
- Add related stories
- Update metrics/lessons learned
- **Commit changes:** `chore(CORE-XXX): Update story with findings`

---

## 🎓 Training & Onboarding

### New Developer Checklist

- [ ] Přečti [Backlog README](../../backlog/README.md)
- [ ] Přečti tento workflow guide
- [ ] Review existující stories (CORE-001, CORE-003)
- [ ] Vytvoř test story pomocí `make backlog-new`
- [ ] Zkus použít GitHub Copilot s story jako prompt
- [ ] Zeptej se týmu na otázky

### Workshop Agenda (90 min)

1. **Overview** (15 min) - Why Git-native? Benefits vs JIRA
2. **Templates** (20 min) - Walkthrough story.md sections
3. **Hands-on** (30 min) - Create story together
4. **Copilot Integration** (15 min) - Demo code generation
5. **Q&A** (10 min)

---

**Last Updated:** 2025-11-06  
**Maintainer:** Core Platform Team  
**Version:** 1.0 (EPIC-001)
