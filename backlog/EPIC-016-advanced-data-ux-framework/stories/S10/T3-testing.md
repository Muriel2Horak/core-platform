# T3: Testing

**Story:** [S10: Dashboard & View Sharing](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

E2E testy pro sharing.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/sharing/dashboard-sharing.spec.ts
test('shares dashboard with user', async ({ page }) => {
  await page.goto('/dashboard/1');
  
  await page.getByRole('button', { name: 'Share' }).click();
  await page.getByLabel('Users').fill('analyst@example.com');
  await page.getByText('analyst@example.com').click();
  await page.getByRole('button', { name: 'Share' }).click();
  
  await expect(page.getByText('Shared with analyst@example.com')).toBeVisible();
});

test('view-only user cannot edit', async ({ page, context }) => {
  // Owner shares with view-only
  await page.goto('/dashboard/1');
  await page.getByRole('button', { name: 'Share' }).click();
  await page.getByLabel('Users').fill('viewer@example.com');
  await page.getByLabel('Permission').selectOption('VIEW');
  
  // Login as viewer
  const viewerPage = await context.newPage();
  await loginAs('viewer@example.com', viewerPage);
  await viewerPage.goto('/dashboard/1');
  
  await expect(viewerPage.getByRole('button', { name: 'Edit' })).toBeDisabled();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (3+ scenarios)

---

**Estimated:** 5 hours
