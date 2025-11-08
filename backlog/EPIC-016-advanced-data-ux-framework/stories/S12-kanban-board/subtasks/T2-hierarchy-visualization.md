# T2: Hierarchie Visualization

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 25 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Implementovat `KanbanCard` komponent s hierarchickou strukturou (Epic → Story → Task → Subtask), progress bars pro parent karty, expand/collapse children.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Hierarchical KanbanCard** - zobrazuje parent-child vztahy s indentací
2. **Progress bar** - `(completedChildren / totalChildren) * 100%`
3. **Expand/collapse** - button ▶/▼ pro zobrazení/skrytí children
4. **Visual indicators** - 📁 Epic, 📄 Story, ✓ Task (done), ⚡ Task (in progress)
5. **Children summary** - Collapsed: "📁 3 child items", Expanded: nested cards
6. **Recursive rendering** - children obsahují další children (max 4 levels)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/kanban/KanbanCard.tsx
import React, { useState } from 'react';
import { Card, CardContent, Typography, Box, LinearProgress, IconButton } from '@mui/material';
import { ExpandMore, ChevronRight, Folder, Description, CheckCircle, FlashOn } from '@mui/icons-material';

interface KanbanCardProps {
  card: KanbanCard;
  level?: number;
}

export const KanbanCard: React.FC<KanbanCardProps> = ({ card, level = 0 }) => {
  const [expanded, setExpanded] = useState(false);
  const hasChildren = card.children && card.children.length > 0;
  
  const completedChildren = card.children?.filter(c => c.status === 'DONE').length || 0;
  const totalChildren = card.children?.length || 0;
  const progressPercent = totalChildren > 0 ? (completedChildren / totalChildren) * 100 : 0;

  const getIcon = () => {
    if (card.type === 'EPIC') return <Folder fontSize="small" />;
    if (card.type === 'STORY') return <Description fontSize="small" />;
    if (card.status === 'DONE') return <CheckCircle fontSize="small" color="success" />;
    if (card.status === 'IN_PROGRESS') return <FlashOn fontSize="small" color="warning" />;
    return null;
  };

  return (
    <Box sx={{ ml: level * 2 }}>
      <Card variant="outlined" sx={{ mb: 1 }}>
        <CardContent>
          {/* Header */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            {hasChildren && (
              <IconButton size="small" onClick={() => setExpanded(!expanded)}>
                {expanded ? <ExpandMore /> : <ChevronRight />}
              </IconButton>
            )}
            {getIcon()}
            <Typography variant="body2" fontWeight="bold" sx={{ ml: 1 }}>
              {card.id}: {card.title}
            </Typography>
          </Box>

          {/* Metadata */}
          <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
            <Typography variant="caption">{card.status}</Typography>
            <Typography variant="caption">👤 {card.assignee?.name}</Typography>
            <Typography variant="caption">{card.priority}</Typography>
          </Box>

          {/* Progress bar for parents */}
          {hasChildren && (
            <Box sx={{ mt: 1 }}>
              <Typography variant="caption">
                Progress: {completedChildren}/{totalChildren} ({Math.round(progressPercent)}%)
              </Typography>
              <LinearProgress variant="determinate" value={progressPercent} sx={{ mt: 0.5 }} />
            </Box>
          )}

          {/* Children summary (collapsed) */}
          {!expanded && hasChildren && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              📁 {totalChildren} child item{totalChildren > 1 ? 's' : ''}
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Expanded children */}
      {expanded && card.children?.map(child => (
        <KanbanCard key={child.id} card={child} level={level + 1} />
      ))}
    </Box>
  );
};
```

---

## ✅ TESTING

```typescript
test('shows progress bar for parent cards', () => {
  const card = {
    id: 'EPIC-1',
    type: 'EPIC',
    children: [
      { id: 'STORY-1', status: 'DONE' },
      { id: 'STORY-2', status: 'IN_PROGRESS' }
    ]
  };

  render(<KanbanCard card={card} />);
  expect(screen.getByText('Progress: 1/2 (50%)')).toBeInTheDocument();
});
```

---

## 📦 DELIVERABLES

- [ ] `KanbanCard.tsx` with hierarchy support
- [ ] Progress calculation logic
- [ ] Expand/collapse functionality
- [ ] Unit tests (70%+ coverage)

---

**Estimated:** 25 hours
