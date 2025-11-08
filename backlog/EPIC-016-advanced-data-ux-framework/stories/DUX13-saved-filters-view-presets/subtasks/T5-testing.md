# T5: Testing

**Story:** [S13: Saved Filters & Views](README.md)  
**Effort:** 4 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3, T4

---

## 📋 TASK DESCRIPTION

E2E a unit testy pro saved views - create, update, share, version history, rollback.

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/views/saved-views.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Saved Views', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/kanban');
  });

  test('creates new saved view', async ({ page }) => {
    // Apply filters
    await page.locator('[data-testid="filter-priority"]').click();
    await page.getByRole('option', { name: 'HIGH' }).click();
    
    // Open save dialog
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('+ Create new view').click();
    
    // Fill form
    await page.getByLabel('View Name').fill('High Priority Tasks');
    await page.getByRole('button', { name: 'Save' }).click();
    
    // Verify
    await expect(page.getByText('Current View: High Priority Tasks')).toBeVisible();
  });

  test('switches between views', async ({ page }) => {
    // Create view 1
    await createView(page, 'View 1', { priorities: ['HIGH'] });
    
    // Create view 2
    await createView(page, 'View 2', { priorities: ['LOW'] });
    
    // Switch to View 1
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('View 1').click();
    
    // Verify filters applied
    await expect(page.locator('[data-testid="filter-badge"]')).toHaveText('Filters (1)');
  });

  test('shares view with other users', async ({ page }) => {
    // Create view
    await createView(page, 'Shared View', {});
    
    // Open save dialog
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('Shared View').click({ button: 'right' });
    await page.getByText('Edit view').click();
    
    // Enable sharing
    await page.getByLabel('Share this view with others').check();
    
    // Select users
    await page.getByLabel('Share with users').click();
    await page.getByRole('option', { name: 'Jan Novák' }).click();
    
    // Save
    await page.getByRole('button', { name: 'Update' }).click();
    
    // Verify
    await expect(page.getByText('Shared with 1 user')).toBeVisible();
  });

  test('shows version history', async ({ page }) => {
    // Create view
    await createView(page, 'Versioned View', { priorities: ['HIGH'] });
    
    // Update view
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('Versioned View').click({ button: 'right' });
    await page.getByText('Edit view').click();
    await page.getByLabel('View Name').fill('Updated View');
    await page.getByRole('button', { name: 'Update' }).click();
    
    // Open version history
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('Updated View').click({ button: 'right' });
    await page.getByText('Version History').click();
    
    // Verify
    await expect(page.getByText('Version 1')).toBeVisible();
    await expect(page.getByText('Version 2')).toBeVisible();
    await expect(page.getByText('Current', { exact: true })).toBeVisible();
  });

  test('restores old version', async ({ page }) => {
    // Create view with version history
    await createView(page, 'View v1', { priorities: ['HIGH'] });
    await updateView(page, 'View v2', { priorities: ['LOW'] });
    
    // Open version history
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('View v2').click({ button: 'right' });
    await page.getByText('Version History').click();
    
    // Restore v1
    await page.getByText('Version 1').click();
    await page.getByRole('button', { name: 'Restore' }).click();
    
    // Verify filters rolled back
    const filters = await page.locator('[data-testid="active-filters"]').textContent();
    expect(filters).toContain('HIGH');
  });

  test('deletes saved view', async ({ page }) => {
    // Create view
    await createView(page, 'To Delete', {});
    
    // Delete
    await page.getByRole('button', { name: 'Current View' }).click();
    await page.getByText('To Delete').click({ button: 'right' });
    await page.getByText('Delete view').click();
    await page.getByRole('button', { name: 'Confirm' }).click();
    
    // Verify
    await expect(page.getByText('To Delete')).not.toBeVisible();
    await expect(page.getByText('Current View: Default')).toBeVisible();
  });
});

// Helper functions
async function createView(page, name, filters) {
  await page.getByRole('button', { name: 'Current View' }).click();
  await page.getByText('+ Create new view').click();
  await page.getByLabel('View Name').fill(name);
  await page.getByRole('button', { name: 'Save' }).click();
}

async function updateView(page, name, filters) {
  await page.getByRole('button', { name: 'Current View' }).click();
  await page.getByText(name).click({ button: 'right' });
  await page.getByText('Edit view').click();
  await page.getByLabel('View Name').fill(name);
  await page.getByRole('button', { name: 'Update' }).click();
}
```

### Unit Tests

```typescript
// frontend/src/components/views/__tests__/SaveViewDialog.test.tsx
test('validates view name required', () => {
  render(<SaveViewDialog open currentFilters={filters} />);
  
  fireEvent.click(screen.getByRole('button', { name: 'Save' }));
  
  expect(screen.getByText('Name is required')).toBeInTheDocument();
});

test('shows filter preview', () => {
  const filters = { priorities: ['HIGH', 'MEDIUM'], groupBy: 'ASSIGNEE' };
  
  render(<SaveViewDialog open currentFilters={filters} />);
  
  expect(screen.getByText('Priorities: HIGH, MEDIUM')).toBeInTheDocument();
  expect(screen.getByText('Grouped by: ASSIGNEE')).toBeInTheDocument();
});
```

---

## 📦 DELIVERABLES

- [ ] E2E tests (6+ scenarios)
- [ ] Unit tests (60%+ coverage)
- [ ] Test fixtures
- [ ] CI integration

---

**Estimated:** 4 hours
