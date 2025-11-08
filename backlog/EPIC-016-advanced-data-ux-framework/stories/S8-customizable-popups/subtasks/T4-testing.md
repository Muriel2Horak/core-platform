# T4: Testing

**Story:** [S8: Customizable Entity Popups](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1-T3

---

## 📋 OBJECTIVE

E2E testy pro layout builder.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/layout-builder/customizable-layouts.spec.ts
test('builds custom layout', async ({ page }) => {
  await page.goto('/admin/layouts/User');
  
  await page.getByRole('button', { name: 'Add Section' }).click();
  await page.getByLabel('Section Title').fill('Contact Info');
  
  await page.getByText('email').dragTo(page.locator('[data-testid="section-1"]'));
  await page.getByText('phone').dragTo(page.locator('[data-testid="section-1"]'));
  
  await page.getByRole('button', { name: 'Save Layout' }).click();
  
  await page.goto('/users/1');
  await expect(page.getByText('Contact Info')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (3+ scenarios)
- [ ] Unit tests

---

**Estimated:** 5 hours
