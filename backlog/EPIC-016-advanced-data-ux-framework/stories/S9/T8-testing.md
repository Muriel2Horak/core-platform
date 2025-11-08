# T8: Testing

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T1-T7

---

## 📋 OBJECTIVE

E2E testy pro actions a workflows.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/actions/tile-actions.spec.ts
test('transitions workflow state', async ({ page }) => {
  await page.goto('/workflows');
  
  await page.getByText('Draft Workflow').click();
  await page.getByRole('button', { name: 'Submit for Review' }).click();
  
  await expect(page.getByText('Status: REVIEW')).toBeVisible();
});

test('bulk deletes workflows', async ({ page }) => {
  await page.goto('/workflows');
  
  await page.getByRole('checkbox', { name: 'Select all' }).click();
  await page.getByRole('button', { name: 'Delete' }).click();
  
  await expect(page.getByText('No workflows')).toBeVisible();
});

test('undo delete action', async ({ page }) => {
  await page.goto('/workflows/1');
  
  await page.getByRole('button', { name: 'Delete' }).click();
  await page.keyboard.press('Control+Z');
  
  await expect(page.getByText('Workflow #1')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (5+ scenarios)
- [ ] Unit tests

---

**Estimated:** 10 hours
