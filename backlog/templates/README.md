# Backlog Templates - Usage Guide

> **Šablony pro User Stories, Subtasks a Epics optimalizované pro GitHub Copilot**

## 📁 Templates

### Available Templates

| Template | Purpose | When to Use |
|----------|---------|-------------|
| [story.md](story.md) | User Story definition | For any user-facing feature or functionality |
| [subtask.md](subtask.md) | Implementation task | To break down stories into developer tasks |
| [epic.md](epic.md) | Large initiative | For features spanning multiple stories (1+ week) |

---

## 🚀 Quick Start

### Creating a New Story

```bash
# Option 1: Use Makefile (recommended)
make backlog-new STORY="Feature Name"

# Option 2: Manual copy
cp backlog/templates/story.md \
   backlog/EPIC-001-epic-name/stories/CORE-042-feature-name/README.md

# Edit the new file
vim backlog/EPIC-001-epic-name/stories/CORE-042-feature-name/README.md
```

### Creating a New Subtask

```bash
# Within a story directory
cp backlog/templates/subtask.md \
   backlog/EPIC-001-epic-name/stories/CORE-042-feature-name/subtasks/SUBTASK-001-task-name.md

vim backlog/EPIC-001-epic-name/stories/CORE-042-feature-name/subtasks/SUBTASK-001-task-name.md
```

### Creating a New Epic

```bash
# Option 1: Use Makefile (recommended)
make backlog-epic-new EPIC="Feature Area Name"

# Option 2: Manual copy
mkdir -p backlog/EPIC-042-feature-area
cp backlog/templates/epic.md backlog/EPIC-042-feature-area/README.md

vim backlog/EPIC-042-feature-area/README.md
```

---

## 📖 Template Anatomy

### Story Template Structure

```markdown
---
YAML Frontmatter (metadata)
---

# Story Title

## 👤 Role / Potřeba / Benefit
Who needs this and why?

## ✅ Definition of Ready (DoR)
Is story ready to implement?

## 🎯 Akceptační kritéria (AC)
What must be true when done?
Given/When/Then format

## 📂 Implementation Mapping
code_paths: Where to write code
test_paths: Where to write tests
docs_paths: Where to document

## ✅ Definition of Done (DoD)
Checklist for completion

## 📋 Subtasks
Developer task breakdown

## 🔗 Related Stories
Dependencies and relationships

## 📊 Metrics
Before/After comparison
```

### Why This Structure?

**For Developers:**
- Clear implementation path (code/test/docs paths)
- Checklist to track progress (DoD)
- Test cases built-in (AC in Given/When/Then)

**For GitHub Copilot:**
- Role/Need/Benefit = what code should do
- AC = test cases to generate
- Path mapping = where to put generated code
- DoD = completeness validation

**For Team:**
- DoR = ready to pull into sprint
- Metrics = business value validation
- Related Stories = dependencies clear

---

## 🎯 Best Practices

### 1. Naming Conventions

**Story IDs:**
```
Format: CORE-XXX
Examples:
  CORE-001 (first story)
  CORE-042 (42nd story)
  CORE-123 (123rd story)
```

**Epic IDs:**
```
Format: EPIC-XXX-short-name
Examples:
  EPIC-001-backlog-system
  EPIC-042-user-management
  EPIC-099-reporting-dashboard
```

**Subtask IDs:**
```
Format: SUBTASK-XXX (within story)
Examples:
  SUBTASK-001-setup
  SUBTASK-002-backend-api
  SUBTASK-003-frontend-ui
```

**Directory Names:**
```
Stories:
  backlog/EPIC-XXX-name/stories/CORE-YYY-short-description/

Subtasks:
  backlog/EPIC-XXX-name/stories/CORE-YYY-name/subtasks/SUBTASK-001-task.md
```

---

### 2. Writing Good Acceptance Criteria

**✅ GOOD - Měřitelné, testovatelné:**

```markdown
**AC1:** User can export data to CSV

**Given** user is logged in as admin
**When** user clicks "Export Data" button
**Then** CSV file downloads with filename "export-2025-11-06.csv"
**And** file contains headers: [id, name, email, created_at]
**And** file contains all active users (status='active')

**Test:**
```gherkin
Scenario: Admin exports user data
  Given I am logged in as admin
  When I click "Export Data"
  Then I should see download prompt
  And downloaded file should have 100 rows (for 100 active users)
```
```

**❌ BAD - Vágní, netestovatelné:**

```markdown
**AC1:** Export feature works

User can export data and it should be in CSV format.
```

---

### 3. Path Mapping Guidelines

**✅ GOOD - Specifické cesty:**

```yaml
code_paths:
  - backend/src/main/java/cz/muriel/core/export/ExportService.java
  - backend/src/main/java/cz/muriel/core/export/ExportController.java
  - backend/src/main/java/cz/muriel/core/export/dto/ExportRequest.java
  - frontend/src/features/export/ExportDialog.tsx
  - frontend/src/api/exportApi.ts

test_paths:
  - backend/src/test/java/cz/muriel/core/export/ExportServiceTest.java
  - frontend/src/features/export/__tests__/ExportDialog.test.tsx
  - e2e/specs/export/data-export.spec.ts

docs_paths:
  - docs/api/export.md
  - docs/features/data-export.md
```

**❌ BAD - Příliš obecné:**

```yaml
code_paths:
  - backend/src/**
  - frontend/src/**
```

**Proč specifické?**
- Copilot ví PŘESNĚ kam psát kód
- Git tracker může mapovat commits → stories
- Coverage validator ověří existence souborů

---

### 4. DoD Checklist Granularity

**✅ GOOD - Akční položky:**

```markdown
### Testing
- [ ] Unit testy pro ExportService (coverage >80%)
- [ ] Integration test pro /api/export endpoint
- [ ] E2E test `e2e/specs/export/data-export.spec.ts` passing
- [ ] Manual test: Export 10,000 rows (performance check)

### Documentation
- [ ] Swagger annotations na ExportController
- [ ] User guide updated: docs/features/data-export.md
- [ ] API reference: docs/api/export.md
```

**❌ BAD - Vágní checklist:**

```markdown
- [ ] Tests done
- [ ] Docs updated
```

---

### 5. Subtask Breakdown

**When to create subtasks:**
- Story estimate >1 day
- Multiple developers working on story
- Complex technical implementation
- Need to track parallel work streams

**How to break down:**

```
Story: CORE-042 - User Export Feature (5 days)
│
├─ SUBTASK-001: API Design & Setup (4 hours)
│  └─ Define API contract, setup boilerplate
│
├─ SUBTASK-002: Backend Service (1 day)
│  └─ ExportService with CSV generation
│
├─ SUBTASK-003: Backend Controller (4 hours)
│  └─ REST endpoint /api/export
│
├─ SUBTASK-004: Frontend UI (1 day)
│  └─ Export dialog, download handling
│
├─ SUBTASK-005: Testing (1 day)
│  └─ Unit + Integration + E2E tests
│
└─ SUBTASK-006: Documentation (4 hours)
   └─ API docs, user guide
```

---

## 🤖 GitHub Copilot Integration

### Using Stories as Prompts

**Example Story Structure:**
```markdown
# CORE-042: User Export Feature

## 👤 Role / Potřeba / Benefit
Jako **admin** potřebuji **exportovat uživatele do CSV** abych **mohl analyzovat data v Excelu**.

## 🎯 Akceptační kritéria
**AC1:** Export obsahuje všechny aktivní uživatele
**AC2:** CSV má columns: id, name, email, created_at
**AC3:** Filename format: export-YYYY-MM-DD.csv

## 📂 Implementation Mapping
code_paths:
  - backend/src/main/java/cz/muriel/core/export/ExportService.java
  - frontend/src/features/export/ExportDialog.tsx
```

**Copilot Prompt v VS Code:**

```
Implementuj CORE-042 podle:
- backlog/EPIC-001/stories/CORE-042-user-export/README.md
- Vytvoř ExportService v backend/src/.../export/ExportService.java
- Splň AC1-AC3 (všichni aktivní users, správné columns, správný filename)
- Přidej unit testy
```

**Copilot vygeneruje:**
```java
@Service
public class ExportService {
    
    @Autowired
    private UserRepository userRepository;
    
    public byte[] exportActiveUsers() {
        List<User> activeUsers = userRepository.findByStatus("active");
        
        String filename = "export-" + LocalDate.now() + ".csv";
        
        // Generate CSV with columns: id, name, email, created_at
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,email,created_at\n");
        
        for (User user : activeUsers) {
            csv.append(user.getId()).append(",")
               .append(user.getName()).append(",")
               .append(user.getEmail()).append(",")
               .append(user.getCreatedAt()).append("\n");
        }
        
        return csv.toString().getBytes();
    }
}
```

---

### Copilot Test Generation

**From AC to Tests:**

```markdown
**AC1:** Given admin clicks export, When CSV generated, Then file contains all active users

**Test:**
```gherkin
Scenario: Export active users
  Given 100 active users exist
  When admin clicks "Export"
  Then CSV file contains 100 rows
  And each row has columns: id, name, email, created_at
```
```

**Copilot Prompt:**
```
Vygeneruj E2E test pro CORE-042 AC1 using Playwright.
Story: backlog/EPIC-001/stories/CORE-042/README.md
```

**Copilot vygeneruje:**
```typescript
test('should export all active users to CSV', async ({ page }) => {
  // Setup: Create 100 active users
  await setupTestData({ activeUsers: 100 });
  
  // Given: Admin is logged in
  await loginAsAdmin(page);
  
  // When: Click export button
  await page.click('button:has-text("Export Data")');
  
  // Then: CSV file downloads
  const download = await page.waitForEvent('download');
  expect(download.suggestedFilename()).toMatch(/export-\d{4}-\d{2}-\d{2}\.csv/);
  
  // And: File contains 100 rows + header
  const content = await download.path().then(p => fs.readFileSync(p, 'utf-8'));
  const lines = content.split('\n');
  expect(lines).toHaveLength(101); // 100 data + 1 header
  
  // And: Header is correct
  expect(lines[0]).toBe('id,name,email,created_at');
});
```

---

## ✅ Validation Rules

### Required Sections (Story)

Každá story MUSÍ obsahovat:
- [ ] YAML frontmatter (id, epic, title, priority, status)
- [ ] `## 👤 Role / Potřeba / Benefit`
- [ ] `## ✅ Definition of Ready (DoR)`
- [ ] `## 🎯 Akceptační kritéria (AC)` (min. 1 AC)
- [ ] `## 📂 Implementation Mapping` (min. code_paths)
- [ ] `## ✅ Definition of Done (DoD)` (min. 10 položek)

### Optional Sections (Story)
- `## 📋 Subtasks` (pokud story >1 den)
- `## 🔗 Related Stories` (pokud dependencies)
- `## 📊 Metrics` (pokud měřitelný impact)
- `## 📝 Implementation Notes` (pokud tech decisions)

### Validation Command

```bash
# Validate single story
make backlog-validate STORY=CORE-042

# Validate all stories
make backlog-validate

# Strict mode (fail on warnings)
make backlog-validate STORY=CORE-042 --strict
```

**Validator checks:**
1. All required sections present
2. At least 1 Acceptance Criterion
3. DoR has checklist items
4. DoD has min. 10 items
5. code_paths not empty
6. Story ID format correct (CORE-XXX)
7. Epic reference exists
8. File paths valid (no typos in paths)

---

## 📊 Examples

### Real Story Examples

**Simple Story (1 day):**
- [CORE-001: Markdown Structure](../EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md)
- Single developer, straightforward implementation
- 5 AC, 11 DoD items, 6 subtasks

**Complex Story (3-5 days):**
- [CORE-042: User Export](examples/CORE-042-user-export-example.md) (hypothetical)
- Multiple components (backend + frontend + tests)
- 8 AC, 20+ DoD items, 10 subtasks

**Epic Example:**
- [EPIC-001: Backlog System](../EPIC-001-backlog-system/README.md)
- 9 stories across 3 priorities
- 2-week timeline, phased approach

---

## 🎓 Training & Onboarding

### For New Team Members

**Step 1: Read Examples**
1. Read [EPIC-001](../EPIC-001-backlog-system/README.md) - See epic structure
2. Read [CORE-001](../EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md) - See story structure
3. Compare with this README - Understand why

**Step 2: Create Practice Story**
1. Copy story template: `cp templates/story.md practice/my-first-story.md`
2. Fill in all sections (use fake feature)
3. Validate: `make backlog-validate STORY=practice/my-first-story`
4. Get team review

**Step 3: Real Story**
1. Create real story from backlog
2. Use Copilot to generate code from story
3. Track DoD completion
4. Retrospective: What worked, what didn't

### Copilot Training

**Exercise 1: Story to Code**
- Write story with clear AC
- Use Copilot to generate Service class
- Verify generated code matches AC

**Exercise 2: Story to Tests**
- Write story with Given/When/Then AC
- Use Copilot to generate E2E tests
- Run tests, verify they pass

**Exercise 3: Full Flow**
- Create story with path mapping
- Generate backend (Service + Controller)
- Generate frontend (Component + API)
- Generate tests (Unit + E2E)
- All from single Copilot prompt referencing story

---

## 🔧 Troubleshooting

### Common Issues

**Issue: Copilot doesn't understand my story**
- ✅ Fix: Make AC more specific (Given/When/Then)
- ✅ Fix: Add code examples in story
- ✅ Fix: Reference similar existing code

**Issue: Path mapping is wrong**
- ✅ Fix: Use absolute paths from project root
- ✅ Fix: Verify paths exist (create directories first)
- ✅ Fix: Run validator: `make backlog-validate`

**Issue: DoD too vague**
- ✅ Fix: Make checklist items actionable
- ✅ Fix: Add specific file references
- ✅ Fix: Include measurable criteria (>80% coverage)

**Issue: Story too big**
- ✅ Fix: Split into multiple stories
- ✅ Fix: Create epic if >1 week
- ✅ Fix: Break down into subtasks

---

## 📚 Additional Resources

### Internal Docs
- [Backlog System Overview](../README.md)
- [Developer Workflow](../../docs/development/backlog-workflow.md)
- [Git Tracking Guide](../../docs/development/git-tracking.md)

### External Resources
- [User Story Best Practices](https://www.mountaingoatsoftware.com/agile/user-stories)
- [Given-When-Then](https://martinfowler.com/bliki/GivenWhenThen.html)
- [GitHub Copilot Docs](https://docs.github.com/en/copilot)

---

**Templates Version:** 1.0  
**Last Updated:** 2025-11-06  
**Maintained By:** Core Platform Team
