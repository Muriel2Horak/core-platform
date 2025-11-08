# T3: Testing

**Story:** [S11: EPIC-014 Integration](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

E2E testy po migraci.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/integration/epic-014-integration.spec.ts
test('renders EPIC-014 tile', async ({ page }) => {
  await page.goto('/users');
  
  await expect(page.locator('[data-component="Tile"]')).toHaveCount(10);
  await expect(page.locator('[data-component="Tile"]').first()).toHaveAttribute('data-entity-type', 'User');
});

test('uses EPIC-014 dashboard layout', async ({ page }) => {
  await page.goto('/dashboard');
  
  await expect(page.locator('[data-component="DashboardLayout"]')).toBeVisible();
  await expect(page.locator('[data-component="DashboardLayout"]')).toHaveAttribute('data-grid-cols', '12');
});
```

---

## ✅ DELIVERABLES

- [ ] E2E regression tests

---

**Estimated:** 5 hours
