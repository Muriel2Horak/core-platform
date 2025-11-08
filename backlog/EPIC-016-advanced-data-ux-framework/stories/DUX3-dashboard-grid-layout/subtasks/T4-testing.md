# T4: Testing

**Story:** [S3: Dashboard Grid Layout](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3

---

## 📋 OBJECTIVE

E2E testy pro dashboard - drag & drop, widgets, save/load.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/dashboard/dashboard-grid.spec.ts
test('drags widget to new position', async ({ page }) => {
  await page.goto('/dashboard');
  
  const widget = page.locator('[data-testid="widget-kpi-1"]');
  await widget.dragTo(page.locator('[data-testid="grid-cell-2-3"]'));
  
  await expect(widget).toHaveCSS('grid-column-start', '2');
});

test('saves and loads dashboard', async ({ page }) => {
  await page.goto('/dashboard');
  
  await page.getByRole('button', { name: 'Save Dashboard' }).click();
  await page.getByLabel('Dashboard Name').fill('My Dashboard');
  await page.getByRole('button', { name: 'Save' }).click();
  
  await page.reload();
  
  await page.getByRole('button', { name: 'Load Dashboard' }).click();
  await page.getByText('My Dashboard').click();
  
  await expect(page.locator('[data-testid="widget-kpi-1"]')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (4+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 5 hours
