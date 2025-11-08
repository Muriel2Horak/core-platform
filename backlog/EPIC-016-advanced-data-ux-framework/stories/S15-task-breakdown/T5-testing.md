# T5: Testing

**Story:** [S15: Task Breakdown](README.md)  
**Effort:** 3 hours  
**Priority:** P1  
**Dependencies:** T1-T4

---

## 📋 TASK DESCRIPTION

E2E testy pro task breakdown - expand/collapse, add subtask, progress, drag & drop.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/breakdown/task-breakdown.spec.ts
test('expands task hierarchy', async ({ page }) => {
  await page.goto('/breakdown');
  
  await page.locator('[data-testid="task-EPIC-1"]').locator('[data-testid="expand-button"]').click();
  
  await expect(page.locator('[data-testid="task-STORY-1"]')).toBeVisible();
});

test('adds subtask inline', async ({ page }) => {
  await page.goto('/breakdown');
  
  await page.getByText('+ Add subtask').click();
  await page.getByPlaceholder('Subtask title...').fill('New subtask');
  await page.locator('[data-testid="save-subtask"]').click();
  
  await expect(page.getByText('New subtask')).toBeVisible();
});

test('shows progress bar for parent tasks', async ({ page }) => {
  await page.goto('/breakdown');
  
  const epic = page.locator('[data-testid="task-EPIC-1"]');
  
  await expect(epic.locator('[data-testid="progress-bar"]')).toBeVisible();
  await expect(epic.getByText('67%')).toBeVisible();
});

test('reparents task via drag & drop', async ({ page }) => {
  await page.goto('/breakdown');
  
  await page.locator('[data-testid="task-STORY-2"]').dragTo(page.locator('[data-testid="task-EPIC-2"]'));
  
  // Verify STORY-2 is now child of EPIC-2
  await page.locator('[data-testid="task-EPIC-2"]').locator('[data-testid="expand-button"]').click();
  await expect(page.locator('[data-testid="task-EPIC-2"]').locator('[data-testid="task-STORY-2"]')).toBeVisible();
});
```

---

## 📦 DELIVERABLES

- [ ] E2E tests (4+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 3 hours
