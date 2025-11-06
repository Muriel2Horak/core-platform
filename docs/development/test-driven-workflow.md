# Test-Driven Development Workflow

> **Test-First Philosophy:** Write tests BEFORE implementation to ensure every feature is validated.

## 📚 Table of Contents

- [Why Test-First Development?](#why-test-first-development)
- [The Red-Green-Refactor Cycle](#the-red-green-refactor-cycle)
- [Story to Test Workflow](#story-to-test-workflow)
- [Bug Tracking & Regression Prevention](#bug-tracking--regression-prevention)
- [Test Tagging Conventions](#test-tagging-conventions)
- [CI/CD Integration](#cicd-integration)
- [Tools & Automation](#tools--automation)
- [Examples](#examples)

---

## 🎯 Why Test-First Development?

### The Problem

Without test-first approach:
- ❌ Tests written as afterthought (if at all)
- ❌ Implementation bias (tests match code, not requirements)
- ❌ Bugs discovered late (expensive to fix)
- ❌ No safety net for refactoring
- ❌ Coverage gaps (edge cases missed)

### The Solution

Test-first development ensures:
- ✅ **Requirements validated before coding** (tests = executable specs)
- ✅ **Every feature has tests** (100% AC coverage)
- ✅ **Bugs found early** (red tests catch issues immediately)
- ✅ **Safe refactoring** (green tests protect against regressions)
- ✅ **Better design** (testable code = well-designed code)

### Benefits

**For Developers:**
- Clear acceptance criteria (test = spec)
- Immediate feedback loop
- Confidence to refactor
- Documentation via tests

**For Business:**
- Fewer production bugs
- Faster time to market (less rework)
- Lower maintenance cost
- Quality gates enforced

---

## 🔄 The Red-Green-Refactor Cycle

### Overview

```
┌─────────────┐
│   1. RED    │  Write failing test (defines requirement)
│   ⬇        │
│  2. GREEN   │  Implement minimum code to pass
│   ⬇        │
│  3. CLEAN   │  Refactor without breaking tests
└─────────────┘
     ↻ Repeat
```

### Phase 1: RED (Write Failing Test)

**Goal:** Define WHAT should happen (requirements).

**Steps:**
1. Pick one Acceptance Criterion (AC)
2. Write test that verifies AC
3. Run test → should FAIL (red)

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
});
```

**Run test:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ❌ FAILED (button doesn't exist yet)
```

**Red phase checklist:**
- [ ] Test is runnable (no syntax errors)
- [ ] Test FAILS for right reason (feature not implemented)
- [ ] Test covers ONE AC (not multiple)
- [ ] Test is tagged with story ID (@CORE-XXX @ACN)

---

### Phase 2: GREEN (Make Test Pass)

**Goal:** Implement MINIMUM code to make test pass.

**Steps:**
1. Write simplest code that makes test green
2. Don't optimize yet (that's refactor phase)
3. Run test → should PASS (green)

**Example:**
```tsx
// frontend/src/features/export/ExportButton.tsx
export function ExportButton() {
  const handleExport = async () => {
    // Minimum implementation - just download a CSV
    const csv = "col1,col2,col3\n";  // Empty CSV for now
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `export-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
  };
  
  return <button onClick={handleExport}>Export Data</button>;
}
```

**Run test:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ✅ PASSED (test green!)
```

**Green phase checklist:**
- [ ] Test PASSES (green)
- [ ] Implementation covers AC requirement
- [ ] No over-engineering (KISS principle)
- [ ] Code is messy (that's OK - refactor next)

---

### Phase 3: CLEAN (Refactor)

**Goal:** Improve code quality WITHOUT breaking tests.

**Steps:**
1. Refactor code (remove duplication, improve readability)
2. Run test → should STAY green
3. Repeat until satisfied

**Example:**
```tsx
// frontend/src/features/export/ExportButton.tsx (refactored)
import { useExportData } from './hooks/useExportData';
import { generateCSV } from '@/utils/csv';
import { downloadFile } from '@/utils/download';

export function ExportButton() {
  const { data, isLoading } = useExportData();
  
  const handleExport = async () => {
    const csv = generateCSV(data, ['col1', 'col2', 'col3']);
    const filename = `export-${new Date().toISOString().split('T')[0]}.csv`;
    downloadFile(csv, filename, 'text/csv');
  };
  
  return (
    <button onClick={handleExport} disabled={isLoading}>
      Export Data
    </button>
  );
}
```

**Run test:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ✅ STILL PASSED (test green after refactor!)
```

**Refactor phase checklist:**
- [ ] Code is cleaner (DRY, SRP, readable)
- [ ] Tests STILL pass (green)
- [ ] No new features added (only refactoring)
- [ ] Performance improved (if applicable)

---

### Repeat Cycle

After refactor, move to NEXT AC:
1. Write failing test for AC2 (RED)
2. Implement AC2 (GREEN)
3. Refactor AC2 (CLEAN)
4. Continue until all AC covered

---

## 📋 Story to Test Workflow

### Step-by-Step Process

#### 1. Create Story with AC

```bash
# Generate story
make backlog-new STORY="Export Data Feature" EPIC="EPIC-002-export" PRIORITY="P1" ESTIMATE="3 days"

# Story created: CORE-012
# Edit README.md with AC
```

**Story AC example:**
```markdown
### AC1: Export CSV as admin
**Given** user is logged in as admin
**When** user clicks "Export Data" button
**Then** CSV file downloads with name "export-YYYY-MM-DD.csv"
**And** file contains headers: col1, col2, col3
**And** file contains all records from last 30 days
```

---

#### 2. Map AC to Tests

Fill out **"AC to Test Mapping"** section in story:

```markdown
### AC1: Export CSV as admin → Tests

| Test Type | Test Path | Status | Coverage | Last Run | Test ID |
|-----------|-----------|--------|----------|----------|---------|
| **Unit Test** | `frontend/src/utils/__tests__/csv.test.ts` | ⏳ Not Written | 0% | N/A | - |
| **Integration Test** | `backend/src/test/.../ExportServiceTest.java` | ⏳ Not Written | 0% | N/A | - |
| **E2E Test** | `e2e/specs/export/export-data.spec.ts` | ⏳ Not Written | 0% | N/A | `@CORE-012 @AC1` |
```

**Commit story:**
```bash
git add backlog/EPIC-002-export/stories/CORE-012-export-data-feature/README.md
git commit -m "feat(CORE-012): Add story with AC and test mapping"
```

---

#### 3. Validate Test Coverage

```bash
# Check test coverage BEFORE implementing
bash scripts/backlog/test_validator.sh --story CORE-012

# Output:
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 📊 Test Coverage Report: CORE-012
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# AC1: Export CSV as admin
#   ❌ Unit Test: [NOT DEFINED] (NOT FOUND)
#   ❌ Integration Test: [NOT DEFINED] (NOT FOUND)
#   ❌ E2E Test: [NOT DEFINED] (NOT FOUND)
#   Coverage: 0% (0/3 test types) ❌
#
# Overall Coverage: 0% ❌ TESTS REQUIRED!
```

---

#### 4. Write Tests FIRST (Red Phase)

**E2E Test:**
```typescript
// e2e/specs/export/export-data.spec.ts
import { test, expect } from '@playwright/test';

test('exports CSV as admin @CORE-012 @AC1', async ({ page }) => {
  // Arrange
  await page.goto('/login');
  await page.fill('input[name="email"]', 'admin@example.com');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL('/dashboard');
  
  // Act
  await page.click('button:has-text("Export Data")');
  
  // Assert
  const download = await page.waitForEvent('download');
  const filename = download.suggestedFilename();
  expect(filename).toMatch(/^export-\d{4}-\d{2}-\d{2}\.csv$/);
  
  // Verify CSV content
  const path = await download.path();
  const content = await fs.readFile(path, 'utf-8');
  const lines = content.split('\n');
  expect(lines[0]).toBe('col1,col2,col3'); // Headers
  expect(lines.length).toBeGreaterThan(1);  // Has data
});
```

**Run test:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ❌ FAILED
# Error: locator.click: Target closed
# (Button doesn't exist yet - RED phase ✅)
```

**Update story:**
```markdown
| **E2E Test** | `e2e/specs/export/export-data.spec.ts` | ✍️ Written | 0% | 2025-11-06 | `@CORE-012 @AC1` |
```

---

#### 5. Implement Feature (Green Phase)

**Backend:**
```java
// backend/src/main/java/cz/muriel/core/export/ExportController.java
@RestController
@RequestMapping("/api/export")
public class ExportController {
    
    @GetMapping("/data")
    public ResponseEntity<byte[]> exportData() {
        // Minimum implementation
        String csv = "col1,col2,col3\n";  // Headers only for now
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/csv");
        headers.set("Content-Disposition", 
            "attachment; filename=export-" + LocalDate.now() + ".csv");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csv.getBytes());
    }
}
```

**Frontend:**
```tsx
// frontend/src/features/export/ExportButton.tsx
export function ExportButton() {
  const handleExport = async () => {
    const response = await fetch('/api/export/data');
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `export-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
  };
  
  return <button onClick={handleExport}>Export Data</button>;
}
```

**Run test:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ✅ PASSED (test green!)
```

**Update story:**
```markdown
| **E2E Test** | `e2e/specs/export/export-data.spec.ts` | ✅ Passing | 100% | 2025-11-06 14:32 | `@CORE-012 @AC1` |
```

---

#### 6. Refactor (Clean Phase)

Extract reusable code, improve structure:

```tsx
// frontend/src/utils/download.ts
export function downloadFile(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

// frontend/src/features/export/ExportButton.tsx
import { downloadFile } from '@/utils/download';

export function ExportButton() {
  const handleExport = async () => {
    const response = await fetch('/api/export/data');
    const blob = await response.blob();
    const filename = `export-${new Date().toISOString().split('T')[0]}.csv`;
    downloadFile(blob, filename);
  };
  
  return <button onClick={handleExport}>Export Data</button>;
}
```

**Run test again:**
```bash
npx playwright test specs/export/export-data.spec.ts

# Expected: ✅ STILL PASSED (refactor didn't break anything!)
```

---

#### 7. Validate Final Coverage

```bash
bash scripts/backlog/test_validator.sh --story CORE-012

# Output:
# AC1: Export CSV as admin
#   ❌ Unit Test: [NOT DEFINED]
#   ❌ Integration Test: [NOT DEFINED]
#   ✅ E2E Test: e2e/specs/export/export-data.spec.ts
#   Coverage: 33% (1/3 test types) ⚠️
#
# Overall Coverage: 33% ⚠️ PARTIAL
```

**Write missing tests (Unit + Integration) to reach 100%.**

---

## 🐛 Bug Tracking & Regression Prevention

### When to Create a Bug

**Create bug report when:**
- ✅ Test fails unexpectedly (regression)
- ✅ Production issue reported by user
- ✅ Edge case not covered by AC

**DON'T create bug for:**
- ❌ AC not implemented yet (that's story work)
- ❌ Feature request (that's new story)

---

### Bug Workflow

#### 1. Report Bug

```bash
# Copy bug template
cp backlog/templates/bug.md backlog/bugs/BUG-042-email-validation.md

# Fill bug details
vim backlog/bugs/BUG-042-email-validation.md
```

**Bug YAML:**
```yaml
id: BUG-042
type: bug
severity: high
status: reported
caused_by_story: CORE-003
caused_by_commit: abc1234
regression_test: e2e/specs/auth/login-email-alias.spec.ts
```

---

#### 2. Write Regression Test (Red)

**Goal:** Reproduce bug with failing test.

```typescript
// e2e/specs/auth/login-email-alias.spec.ts
test('login with + character in email @BUG-042 @regression', async ({ page }) => {
  await page.goto('/login');
  await page.fill('input[name="email"]', 'user+test@example.com');
  await page.fill('input[name="password"]', 'ValidPassword123!');
  await page.click('button[type="submit"]');
  
  // Assert: Should succeed but currently fails
  await expect(page).toHaveURL('/dashboard');
});
```

**Run test:**
```bash
npx playwright test specs/auth/login-email-alias.spec.ts

# Expected: ❌ FAILED (bug reproduced)
```

**Update bug:**
```yaml
regression_test_status: written
status: investigating
```

---

#### 3. Fix Bug (Green)

Find root cause and fix:

```java
// backend/src/main/java/cz/muriel/core/auth/EmailValidator.java
public boolean isValid(String email) {
    // BEFORE (buggy):
    // return email.matches("^[a-zA-Z0-9@._-]+$");
    
    // AFTER (fixed - includes + character):
    return email.matches("^[a-zA-Z0-9@._+-]+$");
}
```

**Run test:**
```bash
npx playwright test specs/auth/login-email-alias.spec.ts

# Expected: ✅ PASSED (bug fixed!)
```

**Update bug:**
```yaml
regression_test_status: passing
status: fixed
fixed_in_version: v1.2.4
```

---

#### 4. Verify Regression Test Prevents Recurrence

**Delete fix temporarily:**
```java
// Revert to buggy code
return email.matches("^[a-zA-Z0-9@._-]+$");
```

**Run test:**
```bash
npx playwright test specs/auth/login-email-alias.spec.ts

# Expected: ❌ FAILED (regression test catches bug!)
```

**Restore fix:**
```java
return email.matches("^[a-zA-Z0-9@._+-]+$");
```

**Update bug:**
```yaml
status: verified
```

---

#### 5. Close Bug

**Bug Fix DoD checklist:**
- [x] Regression test written (@BUG-042 @regression)
- [x] Regression test passing
- [x] Original AC from story still passing
- [x] Bug verified by reporter
- [x] Fix merged to main
- [x] Version tagged (v1.2.4)

**Update bug:**
```yaml
status: closed
```

---

## 🏷️ Test Tagging Conventions

### Story Tags

```typescript
// Tag format: @STORY-ID @ACN [@additional-tags]

test('exports CSV @CORE-012 @AC1', async ({ page }) => {
  // AC1 test
});

test('validates permissions @CORE-012 @AC2 @security', async ({ page }) => {
  // AC2 test with security tag
});

test('performance check @CORE-012 @AC4 @performance', async ({ page }) => {
  // AC4 performance test
});
```

### Bug Tags

```typescript
// Tag format: @BUG-ID @regression [@related-story]

test('email with + works @BUG-042 @regression @CORE-003', async ({ page }) => {
  // Regression test for BUG-042 (related to CORE-003)
});
```

### Running Tagged Tests

```bash
# Run all tests for story
npx playwright test --grep @CORE-012

# Run specific AC
npx playwright test --grep "@CORE-012 @AC1"

# Run all regression tests
npx playwright test --grep @regression

# Run all bugs
npx playwright test --grep @BUG

# Run security tests
npx playwright test --grep @security
```

---

## 🚀 CI/CD Integration

### Pre-Merge Quality Gates

**GitHub Actions workflow:**
```yaml
name: Pre-Merge Validation

on: pull_request

jobs:
  validate-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # 1. Validate test coverage
      - name: Check AC Coverage
        run: |
          bash scripts/backlog/test_validator.sh \
            --story $(git branch --show-current | grep -oE 'CORE-[0-9]+') \
            --min-coverage 80
      
      # 2. Run all tests
      - name: Run Unit Tests
        run: cd backend && ./mvnw test
      
      - name: Run E2E Tests
        run: cd e2e && npx playwright test
      
      # 3. Check test results
      - name: Validate Tests Pass
        run: |
          if [ $? -ne 0 ]; then
            echo "❌ Tests failed - cannot merge"
            exit 1
          fi
```

---

## 🛠️ Tools & Automation

### Available Tools

| Tool | Purpose | Usage |
|------|---------|-------|
| `test_validator.sh` | Validate AC → Test coverage | `bash scripts/backlog/test_validator.sh --story CORE-XXX` |
| `path_validator.py` | Validate file paths exist | `python3 scripts/backlog/path_validator.py --story CORE-XXX` |
| `git_tracker.sh` | Track commits per story | `bash scripts/backlog/git_tracker.sh CORE-XXX` |

### Makefile Targets

```bash
# Run test validator
make test-coverage-story STORY=CORE-012

# Run all backlog validators
make validate-story STORY=CORE-012

# Run E2E tests with coverage
make test-e2e
```

---

## 📖 Examples

### Complete TDD Workflow Example

See [CORE-007 Implementation](../../backlog/EPIC-001-backlog-system/stories/CORE-007-test-first-development-bug-tracking-workflow/README.md) for complete real-world example.

### Bug Fix Workflow Example

See [Bug Template](../../backlog/templates/bug.md) for detailed bug tracking example.

---

## 📚 Further Reading

- [Backlog Workflow](./backlog-workflow.md) - Complete development workflow
- [Story Template](../../backlog/templates/story.md) - Story structure with AC → Test mapping
- [Bug Template](../../backlog/templates/bug.md) - Bug tracking with regression tests
- [Test Validator](../../scripts/backlog/test_validator.sh) - Coverage validation tool

---

**Last Updated:** 2025-11-06  
**Version:** 1.0  
**Maintained By:** Development Team
