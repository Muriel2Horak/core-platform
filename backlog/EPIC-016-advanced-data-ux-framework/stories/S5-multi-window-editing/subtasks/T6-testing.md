# T6: Testing

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T1-T5

---

## 📋 OBJECTIVE

E2E testy pro multi-window editing.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/multi-window/popup-windows.spec.ts
test('opens multiple entity instances', async ({ page }) => {
  await page.goto('/users');
  
  await page.getByText('User #1').click({ button: 'right' });
  await page.getByText('Open in Popup').click();
  
  await page.goto('/users');
  await page.getByText('User #2').click({ button: 'right' });
  await page.getByText('Open in Popup').click();
  
  await expect(page.locator('[data-testid="popup-User-1"]')).toBeVisible();
  await expect(page.locator('[data-testid="popup-User-2"]')).toBeVisible();
});

test('restores windows after reload', async ({ page }) => {
  await page.goto('/');
  
  // Open 2 popups
  await page.locator('[data-testid="open-popup-User-1"]').click();
  await page.locator('[data-testid="open-popup-Workflow-5"]').click();
  
  await page.reload();
  
  await expect(page.locator('[data-testid="popup-User-1"]')).toBeVisible();
  await expect(page.locator('[data-testid="popup-Workflow-5"]')).toBeVisible();
});

test('minimizes and restores window', async ({ page }) => {
  await page.goto('/');
  await page.locator('[data-testid="open-popup-User-1"]').click();
  
  await page.locator('[data-testid="minimize-User-1"]').click();
  await expect(page.locator('[data-testid="popup-User-1"]')).not.toBeVisible();
  
  await page.locator('[data-testid="taskbar-User-1"]').click();
  await expect(page.locator('[data-testid="popup-User-1"]')).toBeVisible();
});
```

---

## ✅ DELIVERABLES

- [ ] E2E tests (5+ scenarios)
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 10 hours
