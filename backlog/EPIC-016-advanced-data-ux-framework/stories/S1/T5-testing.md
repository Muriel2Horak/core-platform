# T5: Testing

**Story:** [S1: Universal Data View Engine](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3, T4

---

## 📋 OBJECTIVE

E2E a unit testy pro DataView component - všechny view modes, Cube.js integration.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/dataview/data-view.spec.ts
test('switches between view modes', async ({ page }) => {
  await page.goto('/users');
  
  await page.getByRole('button', { name: 'Chart' }).click();
  await expect(page.locator('svg.recharts-surface')).toBeVisible();
  
  await page.getByRole('button', { name: 'Table' }).click();
  await expect(page.locator('[data-testid="data-grid"]')).toBeVisible();
});

test('loads data from Cube.js', async ({ page }) => {
  await page.goto('/users');
  await expect(page.getByText('John Doe')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (5+ scenarios)
- [ ] Unit tests (70%+ coverage)
- [ ] Cube.js integration tests

---

**Estimated:** 10 hours
