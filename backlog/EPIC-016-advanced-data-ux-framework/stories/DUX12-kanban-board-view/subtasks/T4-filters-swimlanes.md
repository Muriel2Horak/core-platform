# T4: Filters & Swimlanes

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 20 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Implementovat multi-select filtry (Assignee, Priority, Type, Tags) a swimlane grouping (By Assignee, By Priority, None).

---

## 🎯 ACCEPTANCE CRITERIA

1. **Multi-select filters** - dropdown s checkboxes pro Assignee, Priority, Type, Tags
2. **Filter persistence** - uložení do localStorage
3. **Swimlanes** - horizontální rozdělit board podle groupBy kritéria
4. **Empty swimlanes** - zobrazit prázdné swimlanes s textem "No cards"
5. **Filter badge** - zobrazit počet aktivních filtrů (např. "Filters (3)")

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/kanban/KanbanFilters.tsx
import { FormControl, Select, MenuItem, Checkbox, ListItemText, Chip } from '@mui/material';

interface KanbanFiltersProps {
  assignees: User[];
  priorities: Priority[];
  types: EntityType[];
  selectedFilters: KanbanFilters;
  onFilterChange: (filters: KanbanFilters) => void;
}

export const KanbanFilters: React.FC<KanbanFiltersProps> = ({
  assignees,
  priorities,
  types,
  selectedFilters,
  onFilterChange
}) => {
  const handleAssigneeChange = (event: SelectChangeEvent<string[]>) => {
    onFilterChange({
      ...selectedFilters,
      assignees: event.target.value as string[]
    });
  };

  const activeFiltersCount = 
    selectedFilters.assignees.length +
    selectedFilters.priorities.length +
    selectedFilters.types.length +
    selectedFilters.tags.length;

  return (
    <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
      {/* Assignee filter */}
      <FormControl sx={{ minWidth: 200 }}>
        <InputLabel>Assignee</InputLabel>
        <Select
          multiple
          value={selectedFilters.assignees}
          onChange={handleAssigneeChange}
          renderValue={(selected) => (
            <Box sx={{ display: 'flex', gap: 0.5 }}>
              {selected.map(id => (
                <Chip key={id} label={assignees.find(a => a.id === id)?.name} size="small" />
              ))}
            </Box>
          )}
        >
          {assignees.map(assignee => (
            <MenuItem key={assignee.id} value={assignee.id}>
              <Checkbox checked={selectedFilters.assignees.includes(assignee.id)} />
              <ListItemText primary={assignee.name} />
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {/* Active filter badge */}
      {activeFiltersCount > 0 && (
        <Chip label={`Filters (${activeFiltersCount})`} color="primary" onDelete={clearFilters} />
      )}

      {/* Swimlane dropdown */}
      <FormControl sx={{ minWidth: 150 }}>
        <InputLabel>Group by</InputLabel>
        <Select value={selectedFilters.groupBy} onChange={handleGroupByChange}>
          <MenuItem value="NONE">None</MenuItem>
          <MenuItem value="ASSIGNEE">By Assignee</MenuItem>
          <MenuItem value="PRIORITY">By Priority</MenuItem>
        </Select>
      </FormControl>
    </Box>
  );
};
```

### Swimlane Rendering

```typescript
// frontend/src/components/kanban/KanbanBoard.tsx (update)
const groupBySwimlane = (cards: KanbanCard[], groupBy: GroupBy): Record<string, KanbanCard[]> => {
  if (groupBy === 'NONE') {
    return { default: cards };
  }

  if (groupBy === 'ASSIGNEE') {
    return cards.reduce((acc, card) => {
      const key = card.assignee?.id || 'unassigned';
      acc[key] = acc[key] || [];
      acc[key].push(card);
      return acc;
    }, {} as Record<string, KanbanCard[]>);
  }

  if (groupBy === 'PRIORITY') {
    return cards.reduce((acc, card) => {
      const key = card.priority || 'no-priority';
      acc[key] = acc[key] || [];
      acc[key].push(card);
      return acc;
    }, {} as Record<string, KanbanCard[]>);
  }

  return { default: cards };
};

// Render
const groupedCards = groupBySwimlane(filteredCards, filters.groupBy);

return (
  <Box>
    <KanbanFilters {...filterProps} />
    
    {Object.entries(groupedCards).map(([swimlaneKey, swimlaneCards]) => (
      <Box key={swimlaneKey} sx={{ mb: 4 }}>
        <Typography variant="h6">{getSwimlaneTitle(swimlaneKey)}</Typography>
        
        <Box sx={{ display: 'flex', gap: 2 }}>
          {columns.map(column => (
            <KanbanColumn
              key={column.id}
              column={column}
              cards={swimlaneCards.filter(c => column.statuses.includes(c.status))}
            />
          ))}
        </Box>
        
        {swimlaneCards.length === 0 && (
          <Typography variant="body2" color="text.secondary">No cards</Typography>
        )}
      </Box>
    ))}
  </Box>
);
```

---

## ✅ TESTING

```typescript
test('filters cards by assignee', () => {
  const cards = [
    { id: '1', assignee: { id: 'user1' } },
    { id: '2', assignee: { id: 'user2' } }
  ];

  render(<KanbanBoard cards={cards} />);
  selectFilter('Assignee', 'user1');
  
  expect(screen.queryByText('Task 1')).toBeInTheDocument();
  expect(screen.queryByText('Task 2')).not.toBeInTheDocument();
});

test('groups cards into swimlanes by priority', () => {
  const cards = [
    { id: '1', priority: 'HIGH' },
    { id: '2', priority: 'LOW' }
  ];

  render(<KanbanBoard cards={cards} />);
  selectGroupBy('By Priority');
  
  expect(screen.getByText('HIGH')).toBeInTheDocument();
  expect(screen.getByText('LOW')).toBeInTheDocument();
});
```

---

## 📦 DELIVERABLES

- [ ] Multi-select filter dropdowns
- [ ] Swimlane grouping logic
- [ ] localStorage persistence
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 20 hours
