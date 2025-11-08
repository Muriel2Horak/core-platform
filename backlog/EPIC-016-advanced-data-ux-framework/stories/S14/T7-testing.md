# T7: Testing

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 4 hours  
**Priority:** P1  
**Dependencies:** T1-T6

---

## 📋 TASK DESCRIPTION

E2E testy pro Miro board - drag entities, arrows, export.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/board/miro-board.spec.ts
test('drags entity onto canvas', async ({ page }) => {
  await page.goto('/board');
  
  const entity = page.locator('[data-testid="entity-WORKFLOW-1"]');
  const canvas = page.locator('[data-testid="board-canvas"]');
  
  await entity.dragTo(canvas, { targetPosition: { x: 200, y: 200 } });
  
  await expect(canvas.locator('[data-testid="entity-card-WORKFLOW-1"]')).toBeVisible();
});

test('creates arrow between cards', async ({ page }) => {
  await page.goto('/board');
  
  await page.getByRole('button', { name: 'Add Arrow' }).click();
  await page.locator('[data-testid="entity-card-1"]').click();
  await page.locator('[data-testid="entity-card-2"]').click();
  
  await expect(page.locator('[data-testid="arrow-1-2"]')).toBeVisible();
});

test('exports board as PNG', async ({ page }) => {
  await page.goto('/board');
  
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export PNG' }).click();
  
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('board.png');
});
```

---

## 📦 DELIVERABLES

- [ ] E2E tests (5+ scenarios)
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 4 hours
