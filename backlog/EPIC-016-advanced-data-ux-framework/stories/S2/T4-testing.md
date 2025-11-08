# T4: Testing

**Story:** [S2: Advanced Filtering & Search](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3

---

## 📋 OBJECTIVE

E2E testy pro filtering - multi-select, filter builder, saved filters.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/filters/advanced-filtering.spec.ts
test('applies multi-select filters', async ({ page }) => {
  await page.goto('/users');
  
  await page.locator('[data-testid="filter-status"]').click();
  await page.getByRole('option', { name: 'Active' }).click();
  await page.getByRole('option', { name: 'Pending' }).click();
  
  await expect(page.locator('[data-testid="user-row"]')).toHaveCount(15);
});

test('saves and loads filter', async ({ page }) => {
  await page.goto('/users');
  
  // Apply filters
  await page.locator('[data-testid="filter-status"]').click();
  await page.getByRole('option', { name: 'Active' }).click();
  
  // Save
  await page.getByRole('button', { name: 'Save Filters' }).click();
  await page.getByLabel('Filter Name').fill('Active Users');
  await page.getByRole('button', { name: 'Save' }).click();
  
  // Load
  await page.getByRole('button', { name: 'Saved Filters' }).click();
  await page.getByText('Active Users').click();
  
  await expect(page.locator('[data-testid="filter-chip-status"]')).toHaveText('Status: Active');
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (4+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 5 hours
