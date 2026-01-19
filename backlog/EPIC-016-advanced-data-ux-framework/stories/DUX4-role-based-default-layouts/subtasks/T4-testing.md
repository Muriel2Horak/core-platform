# T3: Testing

**Story:** [S4: Role-based Dashboard Defaults](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

E2E testy pro defaults a templates.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/dashboard/dashboard-defaults.spec.ts
test('admin gets admin dashboard on first login', async ({ page }) => {
  await loginAs('admin', page);
  await page.goto('/dashboard');
  
  await expect(page.getByText('Admin Overview')).toBeVisible();
  await expect(page.getByText('Total Tenants')).toBeVisible();
});

test('analyst gets analyst dashboard', async ({ page }) => {
  await loginAs('analyst', page);
  await page.goto('/dashboard');
  
  await expect(page.getByText('Reports')).toBeVisible();
  await expect(page.getByText('KPIs')).toBeVisible();
});

test('clones template dashboard', async ({ page }) => {
  await page.goto('/dashboard/templates');
  
  await page.getByText('Sales Dashboard').click();
  await page.getByRole('button', { name: 'Use Template' }).click();
  
  await expect(page).toHaveURL(/\/dashboard\/\d+/);
  await expect(page.getByText('Sales Dashboard (Copy)')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (3+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

## ✅ Acceptance Criteria

- [ ] Test cases for the described scope are implemented.
- [ ] Tests pass locally/CI without regressions.
- [ ] Reports or artifacts are stored when applicable.

**Estimated:** 5 hours