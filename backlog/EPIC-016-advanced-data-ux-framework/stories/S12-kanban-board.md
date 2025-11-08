# S12: Kanban Board View

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~120 hours  
**Sprint:** 8-10  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Project Manager / Team Lead,  
**chci** Kanban board view pro workflows/tasks s pokročilými funkcemi,  
**abych**:
- Viděl **workflows v kanbanu** (To Do → In Progress → Review → Done)
- Filtroval podle assignee, priority, tenant (multi-select filtry)
- Používal **swimlanes** (group by: Priority, Tenant, Assignee)
- Viděl **hierarchii** (Epic → Story → Task → Subtask) v kartách
- **Drag & drop** s preservation vazeb:
  - Posunu **child task** → parent zůstane ve svém sloupci
  - Posunu **parent epic** → všechny children se přesunou s ním
  - Volba: "Move only this item" vs. "Move with children"

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Kanban Board Základní Layout

**GIVEN** workflow data s různými stavy  
**WHEN** otevřu Kanban view  
**THEN** zobrazí se 4-5 sloupců (customizable):

```
┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│  To Do (12)  │ In Progress  │   Review (5) │  Testing (3) │   Done (45)  │
│              │     (8)      │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │
│ │WF-123    │ │ │WF-456    │ │ │WF-789    │ │ │WF-111    │ │ │WF-222    │ │
│ │Deploy v2 │ │ │User Auth │ │ │API Tests │ │ │E2E Tests │ │ │Dashboard │ │
│ │          │ │ │          │ │ │          │ │ │          │ │ │          │ │
│ │👤 Alice  │ │ │👤 Bob    │ │ │👤 Alice  │ │ │👤 Charlie│ │ │👤 Alice  │ │
│ │🔴 High   │ │ │🟡 Medium │ │ │🟢 Low    │ │ │🟡 Medium │ │ │🟢 Low    │ │
│ └──────────┘ │ └──────────┘ │ └──────────┘ │ └──────────┘ │ └──────────┘ │
│              │              │              │              │              │
│ ┌──────────┐ │ ┌──────────┐ │              │              │              │
│ │WF-124    │ │ │WF-457    │ │              │              │              │
│ │...       │ │ │...       │ │              │              │              │
│ └──────────┘ │ └──────────┘ │              │              │              │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘

🔍 Filters: [Assignee: All ▼] [Priority: All ▼] [Tenant: All ▼]
📊 Swimlanes: [None ▼]  [Group by: Priority | Tenant | Assignee]
```

**Card structure:**

```typescript
interface KanbanCard {
  id: string;
  title: string;
  status: string; // Column ID
  assignee: User;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  tenant?: Tenant;
  
  // Hierarchie
  parentId?: string; // Epic/Story parent
  children?: KanbanCard[]; // Subtasks
  hierarchyLevel: number; // 0=Epic, 1=Story, 2=Task, 3=Subtask
}
```

### AC2: Multi-Select Filtry

**GIVEN** 50+ workflows v kanbanu  
**WHEN** aplikuji filtry  
**THEN** zobrazí se pouze filtrované items:

**Filter UI:**

```
🔍 Filters (3 active)
┌────────────────────────────────────────────┐
│ Assignee:  [Alice ×] [Bob ×]               │
│ Priority:  [High ×] [Medium ×]             │
│ Tenant:    [Company A ×]                   │
│                                            │
│ [Clear All]  [Save as View...]             │
└────────────────────────────────────────────┘
```

**Implementation:**

```typescript
interface KanbanFilters {
  assigneeIds: string[];
  priorities: ('HIGH' | 'MEDIUM' | 'LOW')[];
  tenantIds: string[];
  tags?: string[];
  dateRange?: { from: Date; to: Date };
}

const filteredCards = cards.filter(card => {
  if (filters.assigneeIds.length > 0 && !filters.assigneeIds.includes(card.assignee.id)) {
    return false;
  }
  if (filters.priorities.length > 0 && !filters.priorities.includes(card.priority)) {
    return false;
  }
  // ... other filters
  return true;
});
```

### AC3: Swimlanes (Group By)

**GIVEN** kanban s workflows  
**WHEN** aktivuji swimlanes: "Group by Priority"  
**THEN** každá priority level má vlastní horizontal swim lane:

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 🔴 HIGH PRIORITY (5 items)                                              │
├──────────────┬──────────────┬──────────────┬──────────────┬────────────┤
│  To Do (2)   │ In Progress  │   Review (1) │  Testing (0) │  Done (1)  │
├──────────────┼──────────────┼──────────────┼──────────────┼────────────┤
│ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │              │ ┌────────┐ │
│ │WF-123    │ │ │WF-456    │ │ │WF-789    │ │              │ │WF-111  │ │
│ └──────────┘ │ └──────────┘ │ └──────────┘ │              │ └────────┘ │
└──────────────┴──────────────┴──────────────┴──────────────┴────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 🟡 MEDIUM PRIORITY (8 items)                                            │
├──────────────┬──────────────┬──────────────┬──────────────┬────────────┤
│  To Do (3)   │ In Progress  │   Review (2) │  Testing (1) │  Done (1)  │
├──────────────┼──────────────┼──────────────┼──────────────┼────────────┤
│ ┌──────────┐ │ ┌──────────┐ │              │              │            │
│ │...       │ │ │...       │ │              │              │            │
│ └──────────┘ │ └──────────┘ │              │              │            │
└──────────────┴──────────────┴──────────────┴──────────────┴────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 🟢 LOW PRIORITY (12 items)                                              │
├──────────────┬──────────────┬──────────────┬──────────────┬────────────┤
│  To Do (5)   │ In Progress  │   Review (2) │  Testing (2) │  Done (2)  │
└──────────────┴──────────────┴──────────────┴──────────────┴────────────┘
```

**Swimlane options:**
- **Group by Priority**: HIGH, MEDIUM, LOW
- **Group by Tenant**: Company A, Company B, Company C
- **Group by Assignee**: Alice, Bob, Charlie
- **None**: Single horizontal board (no swimlanes)

### AC4: Hierarchie Visualization

**GIVEN** Epic obsahuje 3 Stories, každá Story má 2-3 Tasky  
**WHEN** zobrazím Epic card v kanbanu  
**THEN** card ukazuje hierarchii:

```
┌────────────────────────────────────┐
│ EPIC-123: Authentication System    │  ← Epic card (level 0)
│ 👤 Alice  🔴 High  📅 Due: Nov 15  │
│                                    │
│ 📊 Progress: 5/8 tasks done (63%)  │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│ ███████████████░░░░░░░░░░░░░░░░░  │  ← Progress bar
│                                    │
│ 📁 3 Stories:                      │  ← Children indicator
│   ├─ STORY-456: Login UI (✅ Done) │
│   ├─ STORY-457: OAuth (🟡 Review) │
│   └─ STORY-458: 2FA (⏳ To Do)    │
│                                    │
│ [Expand ▼]  [Move ⋮]               │  ← Actions
└────────────────────────────────────┘
```

**Expanded view (click "Expand"):**

```
┌────────────────────────────────────┐
│ EPIC-123: Authentication System    │
│ 👤 Alice  🔴 High                  │
│                                    │
│ ┌──────────────────────────────┐  │
│ │ ├─ STORY-456: Login UI       │  │  ← Child story
│ │    👤 Bob  ✅ Done            │  │
│ │    Tasks:                     │  │
│ │    ├─ TASK-1: Design (✅)    │  │
│ │    └─ TASK-2: Implement (✅) │  │
│ └──────────────────────────────┘  │
│                                    │
│ ┌──────────────────────────────┐  │
│ │ ├─ STORY-457: OAuth          │  │
│ │    👤 Alice  🟡 Review        │  │
│ │    Tasks:                     │  │
│ │    ├─ TASK-3: Google (✅)    │  │
│ │    ├─ TASK-4: GitHub (✅)    │  │
│ │    └─ TASK-5: Testing (🟡)   │  │
│ └──────────────────────────────┘  │
│                                    │
│ [Collapse ▲]                       │
└────────────────────────────────────┘
```

### AC5: Drag & Drop s Preservation Vazeb

**GIVEN** Epic s 3 child stories  
**WHEN** drag & drop různé scenáře  
**THEN** vazby se chovají logically:

#### Scenario 1: Move child only (without parent)

```
Before:
┌─────────────┬─────────────┐
│  To Do      │ In Progress │
├─────────────┼─────────────┤
│ ┌─────────┐ │             │
│ │ EPIC-123│ │             │  ← Parent stays in To Do
│ │  └─STORY│ │             │
│ │   -456  │ │             │
│ └─────────┘ │             │
└─────────────┴─────────────┘

After drag STORY-456 → In Progress:
┌─────────────┬─────────────┐
│  To Do      │ In Progress │
├─────────────┼─────────────┤
│ ┌─────────┐ │ ┌─────────┐ │
│ │ EPIC-123│ │ │STORY-456│ │  ← Child moved independently
│ │         │ │ │Parent:   │ │
│ │         │ │ │EPIC-123  │ │  ← Vazba preserved!
│ └─────────┘ │ └─────────┘ │
└─────────────┴─────────────┘
```

**Confirmation dialog při drag child:**

```
┌────────────────────────────────────────┐
│ Move STORY-456?                        │
│                                        │
│ This story belongs to EPIC-123         │
│ (currently in "To Do")                 │
│                                        │
│ ○ Move only this story                 │
│   Parent stays in "To Do"              │
│                                        │
│ ○ Move parent too                      │
│   EPIC-123 → "In Progress"             │
│                                        │
│ [Cancel]  [Move]                       │
└────────────────────────────────────────┘
```

#### Scenario 2: Move parent with children

```
Before:
┌─────────────┬─────────────┐
│  To Do      │ In Progress │
├─────────────┼─────────────┤
│ ┌─────────┐ │             │
│ │ EPIC-123│ │             │
│ │  ├─STORY│ │             │
│ │   -456  │ │             │
│ │  ├─STORY│ │             │
│ │   -457  │ │             │
│ │  └─STORY│ │             │
│ │   -458  │ │             │
│ └─────────┘ │             │
└─────────────┴─────────────┘

After drag EPIC-123 → In Progress:
┌─────────────┬─────────────┐
│  To Do      │ In Progress │
├─────────────┼─────────────┤
│             │ ┌─────────┐ │
│             │ │ EPIC-123│ │  ← Parent moved
│             │ │  ├─STORY│ │  ← Children moved too!
│             │ │   -456  │ │
│             │ │  ├─STORY│ │
│             │ │   -457  │ │
│             │ │  └─STORY│ │
│             │ │   -458  │ │
│             │ └─────────┘ │
└─────────────┴─────────────┘
```

**Confirmation dialog při drag parent:**

```
┌────────────────────────────────────────┐
│ Move EPIC-123 with children?           │
│                                        │
│ This epic has 3 child stories          │
│                                        │
│ ● Move epic and all children           │
│   (3 stories will move to "In Progress")│
│                                        │
│ ○ Move only epic                       │
│   (children stay in current columns)   │
│                                        │
│ [Cancel]  [Move All]                   │
└────────────────────────────────────────┘
```

#### Scenario 3: Automatic parent state update

```
Rule: Parent automaticky změní stav když:
- Všechny children jsou Done → Parent → Done
- První child přesune do In Progress → Parent → In Progress (if To Do)

Before:
┌─────────────┬──────┬──────┐
│  To Do      │ Done │      │
├─────────────┼──────┼──────┤
│ ┌─────────┐ │      │      │
│ │ EPIC-123│ │      │      │
│ │  └─STORY│ │      │      │
│ │   -456  │ │      │      │
│ └─────────┘ │      │      │
└─────────────┴──────┴──────┘

After drag STORY-456 → Done:
┌─────────────┬──────────────┐
│  To Do      │     Done     │
├─────────────┼──────────────┤
│             │ ┌──────────┐ │
│             │ │ EPIC-123 │ │  ← Auto-moved!
│             │ │  └─STORY │ │
│             │ │   -456   │ │
│             │ └──────────┘ │
└─────────────┴──────────────┘

💡 Notification: "EPIC-123 auto-moved to Done (all children completed)"
```

**Auto-update rules:**

```typescript
const AUTO_UPDATE_RULES = {
  allChildrenDone: {
    condition: (parent, children) => children.every(c => c.status === 'DONE'),
    action: (parent) => parent.status = 'DONE',
    notification: 'Parent auto-moved to Done (all children completed)'
  },
  
  firstChildInProgress: {
    condition: (parent, children) => 
      parent.status === 'TODO' && children.some(c => c.status === 'IN_PROGRESS'),
    action: (parent) => parent.status = 'IN_PROGRESS',
    notification: 'Parent auto-moved to In Progress'
  }
};
```

### AC6: Column Customization

**GIVEN** kanban board  
**WHEN** admin klikne "Customize Columns"  
**THEN** může editovat sloupce:

```
┌────────────────────────────────────────┐
│ Kanban Columns Configuration           │
├────────────────────────────────────────┤
│ ┌────────────────────────────────────┐ │
│ │ 1. To Do                           │ │  ← Drag handle
│ │    Status: TODO                    │ │
│ │    WIP Limit: None                 │ │
│ │    [Edit] [Delete]                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 2. In Progress                     │ │
│ │    Status: IN_PROGRESS             │ │
│ │    WIP Limit: 5 items              │ │  ← Work In Progress limit
│ │    [Edit] [Delete]                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 3. Review                          │ │
│ │    Status: REVIEW                  │ │
│ │    WIP Limit: 3 items              │ │
│ │    [Edit] [Delete]                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ [+ Add Column]                         │
│                                        │
│ [Cancel]  [Save]                       │
└────────────────────────────────────────┘
```

**WIP Limit enforcement:**

```
When dragging card to "In Progress" (WIP Limit: 5, current: 5):

┌────────────────────────────────────────┐
│ ⚠️ WIP Limit Reached                   │
│                                        │
│ Column "In Progress" is at capacity    │
│ (5/5 items)                            │
│                                        │
│ Move anyway?                           │
│                                        │
│ [Cancel]  [Override]                   │
└────────────────────────────────────────┘
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Kanban Board Component** (20h)

**Implementation:**

```typescript
// frontend/src/components/kanban/KanbanBoard.tsx
import React, { useState } from 'react';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { Box, Typography } from '@mui/material';

interface KanbanBoardProps {
  columns: KanbanColumn[];
  cards: KanbanCard[];
  onCardMove: (cardId: string, toColumnId: string) => void;
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

  const handleDragEnd = (result: any) => {
    if (!result.destination) return;

    const cardId = result.draggableId;
    const toColumnId = result.destination.droppableId;

    onCardMove(cardId, toColumnId);
  };

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {Object.entries(groupedCards).map(([swimlaneName, swimlaneCards]) => (
          <Box key={swimlaneName} sx={{ mb: 3 }}>
            {swimlanes && (
              <Typography variant="h6" sx={{ mb: 1 }}>
                {swimlaneName}
              </Typography>
            )}

            <Box sx={{ display: 'flex', gap: 2, overflowX: 'auto' }}>
              {columns.map(column => (
                <Droppable key={column.id} droppableId={column.id}>
                  {(provided, snapshot) => (
                    <Box
                      ref={provided.innerRef}
                      {...provided.droppableProps}
                      sx={{
                        minWidth: 280,
                        bgcolor: snapshot.isDraggingOver ? 'action.hover' : 'background.paper',
                        borderRadius: 1,
                        p: 2
                      }}
                    >
                      <Typography variant="subtitle1" sx={{ mb: 2 }}>
                        {column.name} ({swimlaneCards.filter(c => c.status === column.id).length})
                      </Typography>

                      {swimlaneCards
                        .filter(card => card.status === column.id)
                        .map((card, index) => (
                          <Draggable key={card.id} draggableId={card.id} index={index}>
                            {(provided) => (
                              <Box
                                ref={provided.innerRef}
                                {...provided.draggableProps}
                                {...provided.dragHandleProps}
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
              ))}
            </Box>
          </Box>
        ))}
      </Box>
    </DragDropContext>
  );
};
```

**Deliverable:** Kanban board with drag & drop

---

#### **T2: Hierarchie Visualization** (25h)

**Implementation:**

```typescript
// frontend/src/components/kanban/KanbanCard.tsx
import React, { useState } from 'react';
import { Box, Typography, Avatar, Chip, IconButton, Collapse } from '@mui/material';
import { ExpandMore, ExpandLess } from '@mui/icons-material';

interface KanbanCardProps {
  card: KanbanCard;
  level?: number; // Indentation level
}

export const KanbanCard: React.FC<KanbanCardProps> = ({ card, level = 0 }) => {
  const [expanded, setExpanded] = useState(false);

  const hasChildren = card.children && card.children.length > 0;
  const completedChildren = card.children?.filter(c => c.status === 'DONE').length || 0;
  const totalChildren = card.children?.length || 0;
  const progressPercent = totalChildren > 0 ? (completedChildren / totalChildren) * 100 : 0;

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'error';
      case 'MEDIUM': return 'warning';
      case 'LOW': return 'success';
      default: return 'default';
    }
  };

  return (
    <Box
      sx={{
        bgcolor: 'background.paper',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 1,
        p: 2,
        mb: 1,
        ml: level * 2 // Indentation for hierarchy
      }}
    >
      {/* Card header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">
          {card.id}: {card.title}
        </Typography>
        {hasChildren && (
          <IconButton size="small" onClick={() => setExpanded(!expanded)}>
            {expanded ? <ExpandLess /> : <ExpandMore />}
          </IconButton>
        )}
      </Box>

      {/* Assignee & Priority */}
      <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
        <Avatar src={card.assignee.avatar} sx={{ width: 24, height: 24 }} />
        <Chip
          label={card.priority}
          size="small"
          color={getPriorityColor(card.priority)}
        />
      </Box>

      {/* Progress bar (if has children) */}
      {hasChildren && (
        <Box sx={{ mb: 1 }}>
          <Typography variant="caption" color="text.secondary">
            Progress: {completedChildren}/{totalChildren} tasks done ({progressPercent.toFixed(0)}%)
          </Typography>
          <Box
            sx={{
              width: '100%',
              height: 4,
              bgcolor: 'grey.300',
              borderRadius: 1,
              mt: 0.5
            }}
          >
            <Box
              sx={{
                width: `${progressPercent}%`,
                height: '100%',
                bgcolor: 'primary.main',
                borderRadius: 1
              }}
            />
          </Box>
        </Box>
      )}

      {/* Children summary (collapsed) */}
      {hasChildren && !expanded && (
        <Typography variant="caption" color="text.secondary">
          📁 {totalChildren} child items
        </Typography>
      )}

      {/* Expanded children */}
      <Collapse in={expanded}>
        <Box sx={{ mt: 2 }}>
          {card.children?.map(child => (
            <KanbanCard key={child.id} card={child} level={level + 1} />
          ))}
        </Box>
      </Collapse>
    </Box>
  );
};
```

**Deliverable:** Hierarchical card visualization

---

#### **T3: Drag & Drop Logic s Vazbami** (30h)

**Implementation:**

```typescript
// frontend/src/components/kanban/useDragDropLogic.ts
import { useState } from 'react';

interface DragDropOptions {
  cards: KanbanCard[];
  onCardsUpdate: (cards: KanbanCard[]) => void;
}

export const useDragDropLogic = ({ cards, onCardsUpdate }: DragDropOptions) => {
  const [confirmDialog, setConfirmDialog] = useState<{
    open: boolean;
    card: KanbanCard | null;
    toColumn: string | null;
  }>({ open: false, card: null, toColumn: null });

  const handleCardMove = async (cardId: string, toColumnId: string, moveChildren = true) => {
    const card = findCard(cards, cardId);
    if (!card) return;

    const parent = findParent(cards, cardId);

    // Scenario 1: Moving child without parent
    if (parent && !moveChildren) {
      await updateCardStatus(card, toColumnId);
      // Parent stays in original column
      return;
    }

    // Scenario 2: Moving parent with children
    if (card.children && card.children.length > 0 && moveChildren) {
      await updateCardStatus(card, toColumnId);
      // Recursively move all children
      for (const child of card.children) {
        await updateCardStatus(child, toColumnId);
      }
      return;
    }

    // Scenario 3: Auto-update parent if rules met
    await updateCardStatus(card, toColumnId);
    if (parent) {
      await checkAutoUpdateRules(parent, cards);
    }
  };

  const checkAutoUpdateRules = async (parent: KanbanCard, allCards: KanbanCard[]) => {
    const children = allCards.filter(c => c.parentId === parent.id);

    // Rule 1: All children done → Parent done
    if (children.every(c => c.status === 'DONE') && parent.status !== 'DONE') {
      await updateCardStatus(parent, 'DONE');
      showNotification('Parent auto-moved to Done (all children completed)');
    }

    // Rule 2: First child in progress → Parent in progress
    if (
      parent.status === 'TODO' &&
      children.some(c => c.status === 'IN_PROGRESS')
    ) {
      await updateCardStatus(parent, 'IN_PROGRESS');
      showNotification('Parent auto-moved to In Progress');
    }
  };

  const updateCardStatus = async (card: KanbanCard, newStatus: string) => {
    await fetch(`/api/workflows/${card.id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: newStatus })
    });

    const updatedCards = cards.map(c =>
      c.id === card.id ? { ...c, status: newStatus } : c
    );
    onCardsUpdate(updatedCards);
  };

  const findCard = (cards: KanbanCard[], cardId: string): KanbanCard | null => {
    for (const card of cards) {
      if (card.id === cardId) return card;
      if (card.children) {
        const found = findCard(card.children, cardId);
        if (found) return found;
      }
    }
    return null;
  };

  const findParent = (cards: KanbanCard[], cardId: string): KanbanCard | null => {
    for (const card of cards) {
      if (card.children?.some(c => c.id === cardId)) return card;
      if (card.children) {
        const found = findParent(card.children, cardId);
        if (found) return found;
      }
    }
    return null;
  };

  return {
    handleCardMove,
    confirmDialog,
    setConfirmDialog
  };
};
```

**Deliverable:** Drag & drop logic with hierarchy preservation

---

#### **T4: Filtry & Swimlanes** (20h)

**Implementation:**

```typescript
// frontend/src/components/kanban/KanbanFilters.tsx
import React from 'react';
import { Box, Autocomplete, TextField, Chip } from '@mui/material';

interface KanbanFiltersProps {
  filters: KanbanFilters;
  onFiltersChange: (filters: KanbanFilters) => void;
  assignees: User[];
  tenants: Tenant[];
}

export const KanbanFilters: React.FC<KanbanFiltersProps> = ({
  filters,
  onFiltersChange,
  assignees,
  tenants
}) => {
  return (
    <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
      <Autocomplete
        multiple
        options={assignees}
        getOptionLabel={(option) => option.name}
        value={assignees.filter(a => filters.assigneeIds.includes(a.id))}
        onChange={(_, newValue) => {
          onFiltersChange({
            ...filters,
            assigneeIds: newValue.map(v => v.id)
          });
        }}
        renderInput={(params) => <TextField {...params} label="Assignee" />}
        renderTags={(value, getTagProps) =>
          value.map((option, index) => (
            <Chip label={option.name} {...getTagProps({ index })} />
          ))
        }
        sx={{ minWidth: 250 }}
      />

      <Autocomplete
        multiple
        options={['HIGH', 'MEDIUM', 'LOW']}
        value={filters.priorities}
        onChange={(_, newValue) => {
          onFiltersChange({
            ...filters,
            priorities: newValue as any
          });
        }}
        renderInput={(params) => <TextField {...params} label="Priority" />}
        sx={{ minWidth: 200 }}
      />

      <Autocomplete
        multiple
        options={tenants}
        getOptionLabel={(option) => option.name}
        value={tenants.filter(t => filters.tenantIds.includes(t.id))}
        onChange={(_, newValue) => {
          onFiltersChange({
            ...filters,
            tenantIds: newValue.map(v => v.id)
          });
        }}
        renderInput={(params) => <TextField {...params} label="Tenant" />}
        sx={{ minWidth: 250 }}
      />
    </Box>
  );
};
```

**Deliverable:** Multi-select filters & swimlane grouping

---

#### **T5: Column Customization** (15h)

**Backend API:**

```java
// backend/src/main/java/cz/muriel/core/kanban/model/KanbanColumn.java
@Entity
@Table(name = "kanban_columns")
@Data
public class KanbanColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String statusKey; // Maps to workflow status

    @Column
    private Integer displayOrder;

    @Column
    private Integer wipLimit; // Work In Progress limit

    @Column(nullable = false)
    private Long dashboardId; // Which dashboard this column belongs to
}
```

**Deliverable:** Column customization API + UI

---

#### **T6: Testing** (10h)

**E2E tests:**

```typescript
// e2e/specs/kanban/kanban-board.spec.ts
test('Kanban board displays columns and cards', async ({ page }) => {
  await page.goto('/kanban');

  // Verify columns
  await expect(page.locator('text=To Do')).toBeVisible();
  await expect(page.locator('text=In Progress')).toBeVisible();
  await expect(page.locator('text=Done')).toBeVisible();

  // Verify cards
  await expect(page.locator('text=WF-123')).toBeVisible();
});

test('Drag & drop card to new column', async ({ page }) => {
  await page.goto('/kanban');

  // Drag WF-123 from To Do to In Progress
  const card = page.locator('[data-card-id="WF-123"]');
  const targetColumn = page.locator('[data-column-id="IN_PROGRESS"]');

  await card.dragTo(targetColumn);

  // Verify card moved
  await expect(targetColumn.locator('text=WF-123')).toBeVisible();
});

test('Move parent with children confirmation', async ({ page }) => {
  await page.goto('/kanban');

  // Drag epic with children
  const epic = page.locator('[data-card-id="EPIC-123"]');
  await epic.dragTo(page.locator('[data-column-id="DONE"]'));

  // Verify confirmation dialog
  await expect(page.locator('text=Move epic and all children')).toBeVisible();

  // Confirm move
  await page.click('button:has-text("Move All")');

  // Verify all moved
  await expect(page.locator('[data-column-id="DONE"] >> text=EPIC-123')).toBeVisible();
  await expect(page.locator('[data-column-id="DONE"] >> text=STORY-456')).toBeVisible();
});
```

**Deliverable:** E2E tests for kanban board

---

## 📊 SUCCESS METRICS

- ✅ Kanban render < 1s (100 cards)
- ✅ Drag & drop latency < 100ms
- ✅ Filter apply < 300ms
- ✅ 80%+ teams use kanban view
- ✅ Hierarchy preservation: 0 lost parent-child links

---

## 🔗 DEPENDENCIES

- **EPIC-003:** RBAC (permissions per card)
- **S1:** DataView (underlying data engine)
- **S2:** Advanced Filtering (filter logic)

---

## 📚 LIBRARIES

- `@hello-pangea/dnd` (drag & drop)
- `react-beautiful-dnd` (alternative)
- `react-window` (virtualized rendering for 1000+ cards)

---

**Status:** 📋 TODO  
**Effort:** ~120 hours (~3 sprints)  
**Next:** S13 (Saved Filters & Views)
