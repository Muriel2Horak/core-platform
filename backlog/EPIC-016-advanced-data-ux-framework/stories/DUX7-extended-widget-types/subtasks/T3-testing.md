# T3: Testing

**Story:** [S7: Extended Data Widgets](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

E2E testy pro charty.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/charts/advanced-charts.spec.ts
test('renders heatmap chart', async ({ page }) => {
  await page.goto('/dashboard');
  
  await page.getByRole('button', { name: 'Add Widget' }).click();
  await page.getByText('Heatmap').click();
  
  await expect(page.locator('svg[data-testid="heatmap-chart"]')).toBeVisible();
});

test('configures chart colors', async ({ page }) => {
  await page.goto('/dashboard/widget/1/edit');
  
  await page.getByLabel('Color Palette').click();
  await page.getByText('Greens').click();
  
  await expect(page.locator('svg rect[fill*="green"]')).toHaveCount(5);
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