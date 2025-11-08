# T3: Drag & Drop Logic se Status Picker

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 30 hours  
**Priority:** P0  
**Dependencies:** T1, T2

---

## 📋 TASK DESCRIPTION

Implementovat drag & drop logiku s 3 scénáři:
1. **Card bez children** → přesun karty
2. **Card s children** → přesun celého subtree
3. **Drop do sloupce s více statusy** → zobrazit status picker dialog (AC4)

---

## 🎯 ACCEPTANCE CRITERIA

1. **Drag solo card** - přesun karty bez children do nového sloupce
2. **Drag with children** - přesun karty + celý subtree (vazby zachovány)
3. **Status picker dialog** - při dropu do sloupce s více statusy zobrazit dialog
4. **Status options** - dialog zobrazuje POUZE statusy namapované do target sloupce
5. **Vazba preservation** - parent-child vztahy se nezmění při přesunu
6. **Server sync** - PATCH `/api/workflows/{id}` s novým statusem

---

## 🏗️ IMPLEMENTATION

### Status Picker Dialog

```typescript
// frontend/src/components/kanban/StatusPickerDialog.tsx
import { Dialog, DialogTitle, List, ListItemButton, ListItemText } from '@mui/material';

interface StatusPickerDialogProps {
  open: boolean;
  statuses: WorkflowStatus[];  // Statusy z target sloupce
  onSelect: (status: WorkflowStatus) => void;
  onCancel: () => void;
}

export const StatusPickerDialog: React.FC<StatusPickerDialogProps> = ({
  open,
  statuses,
  onSelect,
  onCancel
}) => {
  return (
    <Dialog open={open} onClose={onCancel}>
      <DialogTitle>Vyberte stav pro kartu</DialogTitle>
      <List>
        {statuses.map(status => (
          <ListItemButton key={status.id} onClick={() => onSelect(status)}>
            <ListItemText 
              primary={status.name}
              secondary={status.description}
            />
          </ListItemButton>
        ))}
      </List>
    </Dialog>
  );
};
```

### Drag & Drop Logic

```typescript
// frontend/src/components/kanban/KanbanBoard.tsx (aktualizace)
import { StatusPickerDialog } from './StatusPickerDialog';

const handleDragEnd = (result: DropResult) => {
  if (!result.destination) return;

  const { draggableId, source, destination } = result;
  const card = findCardById(draggableId);
  const targetColumn = columns[destination.droppableId];

  // ✅ AC4: Pokud sloupec má více statusů, zobrazit picker
  if (targetColumn.statuses.length > 1) {
    setStatusPickerState({
      open: true,
      card,
      targetColumn,
      statuses: targetColumn.statuses
    });
    return;
  }

  // ✅ AC2: Single status - direct assignment
  const newStatus = targetColumn.statuses[0];
  moveCard(card, newStatus);
};

const moveCard = async (card: KanbanCard, newStatus: WorkflowStatus) => {
  // ✅ AC1: Solo card - přesun karty
  if (!card.children || card.children.length === 0) {
    await api.patch(`/api/workflows/${card.id}`, { status: newStatus.id });
    return;
  }

  // ✅ AC2: Card with children - přesun celého subtree
  const subtreeIds = getAllChildrenIds(card);
  await Promise.all([
    api.patch(`/api/workflows/${card.id}`, { status: newStatus.id }),
    ...subtreeIds.map(id => api.patch(`/api/workflows/${id}`, { status: newStatus.id }))
  ]);
};

const getAllChildrenIds = (card: KanbanCard): string[] => {
  if (!card.children) return [];
  return card.children.flatMap(child => [child.id, ...getAllChildrenIds(child)]);
};
```

### Column Configuration s Status Mapping

```typescript
// backend/src/main/java/cz/muriel/core/kanban/KanbanColumn.java
@Entity
public class KanbanColumn {
  @Id
  private UUID id;
  
  private String name;  // "In Progress"
  
  @ManyToMany
  private Set<WorkflowStatus> statuses;  // {STARTED, BLOCKED}
  
  private Integer wipLimit;  // 5
  
  // Getter: Column má více statusů?
  public boolean hasMultipleStatuses() {
    return statuses.size() > 1;
  }
}
```

---

## ✅ TESTING

```typescript
test('shows status picker when dropping into multi-status column', async () => {
  const column = {
    id: 'in-progress',
    name: 'In Progress',
    statuses: [
      { id: 'STARTED', name: 'Started' },
      { id: 'BLOCKED', name: 'Blocked' }
    ]
  };

  dragCard('TASK-1', 'todo', 'in-progress');
  
  // ✅ Dialog zobrazí 2 možnosti
  expect(screen.getByText('Vyberte stav pro kartu')).toBeInTheDocument();
  expect(screen.getByText('Started')).toBeInTheDocument();
  expect(screen.getByText('Blocked')).toBeInTheDocument();
});

test('preserves parent-child vazba when moving subtree', async () => {
  const epic = {
    id: 'EPIC-1',
    children: [{ id: 'STORY-1' }]
  };

  dragCard('EPIC-1', 'backlog', 'in-progress');
  
  // ✅ Po přesunu má EPIC-1 stále child STORY-1
  const updated = await fetchCard('EPIC-1');
  expect(updated.children[0].id).toBe('STORY-1');
});
```

---

## 📦 DELIVERABLES

- [ ] Status picker dialog component
- [ ] Drag & drop with 3 scenarios
- [ ] Status aggregation logic
- [ ] Server sync (PATCH API)
- [ ] E2E tests for all scenarios

---

**Estimated:** 30 hours
