# T1: Kanban Board Component

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 TASK DESCRIPTION

Implementovat hlavní `KanbanBoard` komponent s DragDropContext z `@hello-pangea/dnd`, rendering sloupců a karet, support pro swimlanes grouping.

---

## 🎯 ACCEPTANCE CRITERIA

1. **KanbanBoard component** s props: columns, cards, onCardMove, swimlanes
2. **DragDropContext** wrapper s handleDragEnd logikou
3. **Columns rendering** - horizontální layout, scrollable
4. **Card badges** - počet karet per sloupec (e.g., "To Do (12)")
5. **Swimlanes support** - group by Priority/Tenant/Assignee (horizontal lanes)
6. **Responsive layout** - funguje na desktop (1920px), laptop (1440px), tablet (1024px)

---

## 🏗️ IMPLEMENTATION

### Frontend Component

```typescript
// frontend/src/components/kanban/KanbanBoard.tsx
import React from 'react';
import { DragDropContext, Droppable, Draggable, DropResult } from '@hello-pangea/dnd';
import { Box, Typography, Paper } from '@mui/material';
import { KanbanCard } from './KanbanCard';
import { groupBySwimlane } from './utils';

interface KanbanBoardProps {
  columns: KanbanColumn[];
  cards: KanbanCard[];
  onCardMove: (cardId: string, toColumnId: string) => Promise<void>;
  swimlanes?: 'priority' | 'tenant' | 'assignee' | null;
}

export const KanbanBoard: React.FC<KanbanBoardProps> = ({
  columns,
  cards,
  onCardMove,
  swimlanes
}) => {
  const groupedCards = swimlanes 
    ? groupBySwimlane(cards, swimlanes)
    : { default: cards };

  const handleDragEnd = async (result: DropResult) => {
    if (!result.destination) return;

    const cardId = result.draggableId;
    const toColumnId = result.destination.droppableId;

    await onCardMove(cardId, toColumnId);
  };

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {Object.entries(groupedCards).map(([swimlaneName, swimlaneCards]) => (
          <Box key={swimlaneName} sx={{ mb: swimlanes ? 3 : 0 }}>
            {swimlanes && (
              <Typography variant="h6" sx={{ mb: 1, px: 2 }}>
                {swimlaneName}
              </Typography>
            )}

            <Box sx={{ display: 'flex', gap: 2, overflowX: 'auto', px: 2 }}>
              {columns.map(column => {
                const columnCards = swimlaneCards.filter(
                  card => column.mappedStatuses.includes(card.status)
                );

                return (
                  <Paper
                    key={column.id}
                    sx={{
                      minWidth: 300,
                      maxWidth: 300,
                      flexShrink: 0,
                      backgroundColor: column.color || '#f5f5f5',
                      p: 2
                    }}
                  >
                    {/* Column header */}
                    <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="subtitle1" fontWeight="bold">
                        {column.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        ({columnCards.length})
                      </Typography>
                    </Box>

                    {/* Mapped statuses */}
                    <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
                      {column.mappedStatuses.join(', ')}
                    </Typography>

                    {/* Droppable area */}
                    <Droppable droppableId={column.id}>
                      {(provided, snapshot) => (
                        <Box
                          ref={provided.innerRef}
                          {...provided.droppableProps}
                          sx={{
                            minHeight: 200,
                            backgroundColor: snapshot.isDraggingOver ? '#e3f2fd' : 'transparent',
                            borderRadius: 1,
                            p: 1
                          }}
                        >
                          {columnCards.map((card, index) => (
                            <Draggable key={card.id} draggableId={card.id} index={index}>
                              {(provided, snapshot) => (
                                <Box
                                  ref={provided.innerRef}
                                  {...provided.draggableProps}
                                  {...provided.dragHandleProps}
                                  sx={{
                                    mb: 1,
                                    opacity: snapshot.isDragging ? 0.5 : 1
                                  }}
                                >
                                  <KanbanCard card={card} />
                                </Box>
                              )}
                            </Draggable>
                          ))}
                          {provided.placeholder}
                        </Box>
                      )}
                    </Droppable>
                  </Paper>
                );
              })}
            </Box>
          </Box>
        ))}
      </Box>
    </DragDropContext>
  );
};
```

### Utility: Swimlane Grouping

```typescript
// frontend/src/components/kanban/utils/groupBySwimlane.ts
export const groupBySwimlane = (
  cards: KanbanCard[],
  swimlaneType: 'priority' | 'tenant' | 'assignee'
): Record<string, KanbanCard[]> => {
  switch (swimlaneType) {
    case 'priority':
      return {
        'HIGH': cards.filter(c => c.priority === 'HIGH'),
        'MEDIUM': cards.filter(c => c.priority === 'MEDIUM'),
        'LOW': cards.filter(c => c.priority === 'LOW')
      };

    case 'tenant':
      return cards.reduce((acc, card) => {
        const tenantName = card.tenant?.name || 'Unassigned';
        acc[tenantName] = [...(acc[tenantName] || []), card];
        return acc;
      }, {} as Record<string, KanbanCard[]>);

    case 'assignee':
      return cards.reduce((acc, card) => {
        const assigneeName = card.assignee?.name || 'Unassigned';
        acc[assigneeName] = [...(acc[assigneeName] || []), card];
        return acc;
      }, {} as Record<string, KanbanCard[]>);

    default:
      return { default: cards };
  }
};
```

---

## ✅ TESTING

### Unit Tests

```typescript
// frontend/src/components/kanban/__tests__/KanbanBoard.test.tsx
describe('KanbanBoard', () => {
  it('renders columns with card counts', () => {
    const columns = [
      { id: '1', name: 'To Do', mappedStatuses: ['DRAFT'], color: '#e3f2fd' }
    ];
    const cards = [
      { id: 'c1', title: 'Task 1', status: 'DRAFT', ... }
    ];

    render(<KanbanBoard columns={columns} cards={cards} onCardMove={jest.fn()} />);

    expect(screen.getByText('To Do')).toBeInTheDocument();
    expect(screen.getByText('(1)')).toBeInTheDocument();
  });

  it('groups cards by swimlane', () => {
    const cards = [
      { id: 'c1', priority: 'HIGH', ... },
      { id: 'c2', priority: 'LOW', ... }
    ];

    render(<KanbanBoard ... swimlanes="priority" />);

    expect(screen.getByText('HIGH')).toBeInTheDocument();
    expect(screen.getByText('LOW')).toBeInTheDocument();
  });
});
```

### E2E Tests

```typescript
// e2e/specs/kanban/board.spec.ts
test('drag card between columns', async ({ page }) => {
  await page.goto('/kanban/workflows');

  // Drag from "To Do" to "In Progress"
  await page.dragAndDrop(
    '[data-testid="card-WF-123"]',
    '[data-testid="column-in-progress"]'
  );

  // Verify card moved
  await expect(page.locator('[data-testid="column-in-progress"] >> text=WF-123')).toBeVisible();
});
```

---

## 📦 DELIVERABLES

- [ ] `KanbanBoard.tsx` component
- [ ] `groupBySwimlane.ts` utility
- [ ] Unit tests (80%+ coverage)
- [ ] E2E test for drag & drop
- [ ] Responsive CSS (desktop/laptop/tablet)

---

**Estimated:** 20 hours  
**Actual:** ___ hours
