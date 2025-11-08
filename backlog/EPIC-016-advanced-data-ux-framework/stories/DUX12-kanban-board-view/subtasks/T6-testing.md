# T6: Testing

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3, T4, T5

---

## 📋 TASK DESCRIPTION

Comprehensive E2E testing pro Kanban Board - drag & drop, filters, swimlanes, hierarchy, status picker.

---

## 🎯 TEST COVERAGE

1. **Drag & drop scenarios** (T3) - solo card, card with children, status picker
2. **Filters** (T4) - multi-select, persistence, reset
3. **Swimlanes** (T4) - grouping, empty swimlanes
4. **Hierarchy** (T2) - expand/collapse, progress bars
5. **Column customization** (T5) - rename, status mapping, WIP limits

---

## 🏗️ IMPLEMENTATION

```typescript
// e2e/specs/kanban/kanban-board.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Kanban Board', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/kanban');
    await page.waitForSelector('[data-testid="kanban-board"]');
  });

  test('drags solo card to new column', async ({ page }) => {
    // Arrange
    const card = page.locator('[data-testid="kanban-card-TASK-1"]');
    const targetColumn = page.locator('[data-testid="kanban-column-IN_PROGRESS"]');
    
    // Act
    await card.dragTo(targetColumn);
    
    // Assert
    await expect(targetColumn.locator('[data-testid="kanban-card-TASK-1"]')).toBeVisible();
  });

  test('shows status picker when dropping into multi-status column', async ({ page }) => {
    // Arrange
    const card = page.locator('[data-testid="kanban-card-TASK-1"]');
    const multiStatusColumn = page.locator('[data-testid="kanban-column-IN_PROGRESS"]');
    
    // Act
    await card.dragTo(multiStatusColumn);
    
    // Assert - Status picker dialog zobrazí
    await expect(page.getByText('Vyberte stav pro kartu')).toBeVisible();
    await expect(page.getByText('Started')).toBeVisible();
    await expect(page.getByText('Blocked')).toBeVisible();
    
    // Select status
    await page.getByText('Started').click();
    
    // Verify card moved
    await expect(multiStatusColumn.locator('[data-testid="kanban-card-TASK-1"]')).toBeVisible();
  });

  test('drags card with children and preserves hierarchy', async ({ page }) => {
    // Arrange
    const epic = page.locator('[data-testid="kanban-card-EPIC-1"]');
    const targetColumn = page.locator('[data-testid="kanban-column-DONE"]');
    
    // Expand to see children
    await epic.locator('[data-testid="expand-button"]').click();
    const childBefore = epic.locator('[data-testid="kanban-card-STORY-1"]');
    await expect(childBefore).toBeVisible();
    
    // Act
    await epic.dragTo(targetColumn);
    
    // Assert - Epic AND child moved
    await expect(targetColumn.locator('[data-testid="kanban-card-EPIC-1"]')).toBeVisible();
    
    // Expand again
    await targetColumn.locator('[data-testid="kanban-card-EPIC-1"]').locator('[data-testid="expand-button"]').click();
    await expect(targetColumn.locator('[data-testid="kanban-card-STORY-1"]')).toBeVisible();
  });

  test('filters cards by assignee', async ({ page }) => {
    // Act
    await page.locator('[data-testid="filter-assignee"]').click();
    await page.getByRole('option', { name: 'Jan Novák' }).click();
    
    // Assert
    await expect(page.locator('[data-testid="kanban-card-TASK-1"]')).toBeVisible();
    await expect(page.locator('[data-testid="kanban-card-TASK-2"]')).not.toBeVisible();
    
    // Filter badge
    await expect(page.getByText('Filters (1)')).toBeVisible();
  });

  test('groups cards into swimlanes by priority', async ({ page }) => {
    // Act
    await page.locator('[data-testid="swimlane-groupby"]').selectOption('PRIORITY');
    
    // Assert
    await expect(page.getByText('HIGH')).toBeVisible();
    await expect(page.getByText('MEDIUM')).toBeVisible();
    await expect(page.getByText('LOW')).toBeVisible();
  });

  test('shows progress bar for parent cards', async ({ page }) => {
    // Arrange
    const epic = page.locator('[data-testid="kanban-card-EPIC-1"]');
    
    // Assert
    await expect(epic.locator('[data-testid="progress-bar"]')).toBeVisible();
    await expect(epic.getByText('Progress: 2/3 (67%)')).toBeVisible();
  });

  test('customizes column - rename and set WIP limit', async ({ page }) => {
    // Act
    await page.locator('[data-testid="kanban-column-TODO"]').locator('[data-testid="settings-button"]').click();
    
    // Dialog opens
    await expect(page.getByText('Customize Column')).toBeVisible();
    
    // Rename
    await page.getByLabel('Column Name').fill('Backlog');
    
    // Set WIP limit
    await page.getByLabel('WIP Limit').fill('5');
    
    // Save
    await page.getByRole('button', { name: 'Save' }).click();
    
    // Assert
    await expect(page.locator('[data-testid="kanban-column-TODO"]').getByText('Backlog')).toBeVisible();
  });

  test('shows WIP limit warning when exceeded', async ({ page }) => {
    // Arrange - Create column with WIP limit 2
    const column = page.locator('[data-testid="kanban-column-IN_PROGRESS"]');
    
    // Drag 3 cards into column (exceeds limit)
    await page.locator('[data-testid="kanban-card-TASK-1"]').dragTo(column);
    await page.locator('[data-testid="kanban-card-TASK-2"]').dragTo(column);
    await page.locator('[data-testid="kanban-card-TASK-3"]').dragTo(column);
    
    // Assert
    await expect(column.getByText('WIP limit exceeded! (3/2)')).toBeVisible();
    await expect(column.locator('[data-testid="WarningIcon"]')).toBeVisible();
  });
});
```

---

## 🧪 UNIT TEST EXAMPLES

```typescript
// frontend/src/components/kanban/__tests__/KanbanBoard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { KanbanBoard } from '../KanbanBoard';

test('renders columns with card counts', () => {
  const columns = [
    { id: 'todo', name: 'To Do', statuses: ['TODO'] },
    { id: 'done', name: 'Done', statuses: ['DONE'] }
  ];
  const cards = [
    { id: '1', status: 'TODO' },
    { id: '2', status: 'TODO' },
    { id: '3', status: 'DONE' }
  ];

  render(<KanbanBoard columns={columns} cards={cards} />);
  
  expect(screen.getByText('To Do (2)')).toBeInTheDocument();
  expect(screen.getByText('Done (1)')).toBeInTheDocument();
});

test('calculates progress for parent cards', () => {
  const card = {
    id: 'EPIC-1',
    children: [
      { id: 'S1', status: 'DONE' },
      { id: 'S2', status: 'DONE' },
      { id: 'S3', status: 'IN_PROGRESS' }
    ]
  };

  render(<KanbanCard card={card} />);
  
  expect(screen.getByText('Progress: 2/3 (67%)')).toBeInTheDocument();
});
```

---

## 📦 DELIVERABLES

- [ ] E2E tests for all features (10+ scenarios)
- [ ] Unit tests (70%+ coverage)
- [ ] Test data fixtures
- [ ] CI integration

---

**Estimated:** 10 hours
