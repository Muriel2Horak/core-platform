# T5: Testing

**Story:** [S6: Visual Query Builder](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1-T4

---

## 📋 OBJECTIVE

E2E testy pro query builder.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/query-builder/visual-query.spec.ts
test('builds query with drag & drop', async ({ page }) => {
  await page.goto('/query-builder');
  
  await page.getByText('Users.name').dragTo(page.locator('[data-testid="dimensions-dropzone"]'));
  await page.getByText('Users.count').dragTo(page.locator('[data-testid="measures-dropzone"]'));
  
  await expect(page.locator('[data-testid="dimensions-dropzone"]')).toContainText('Users.name');
  await expect(page.locator('[data-testid="measures-dropzone"]')).toContainText('Users.count');
});

test('shows live preview', async ({ page }) => {
  await page.goto('/query-builder');
  
  await page.getByText('Users.name').dragTo(page.locator('[data-testid="dimensions-dropzone"]'));
  await page.getByText('Users.count').dragTo(page.locator('[data-testid="measures-dropzone"]'));
  
  await expect(page.locator('[data-testid="preview-table"]')).toBeVisible();
  await expect(page.locator('table thead')).toContainText('name');
  await expect(page.locator('table thead')).toContainText('count');
});

test('adds query to dashboard', async ({ page }) => {
  await page.goto('/query-builder');
  
  // Build query
  await page.getByText('Users.name').dragTo(page.locator('[data-testid="dimensions-dropzone"]'));
  
  await page.getByRole('button', { name: 'Add to Dashboard' }).click();
  await page.getByText('My Dashboard').click();
  
  await page.goto('/dashboard/1');
  await expect(page.locator('[data-testid="widget-query"]')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (4+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 5 hours
