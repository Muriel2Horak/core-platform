# S12: Kanban Board View

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~120 hours  
**Sprint:** 8-10  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Project Manager / Team Lead / Support Agent,  
**chci** Kanban board view pro **entity instances** (Workflows, Users, Tenants, Support Tickets, atd.),  
**abych**:
- Viděl **entity instances** jako kanban karty (Workflow cards, User cards, Ticket cards)
- Definoval **vlastní sloupce** a mapoval je na **workflow stavy**
- **Agregoval více stavů** do jednoho sloupce (např. "In Progress" = [ASSIGNED, STARTED, BLOCKED])
- Při drop viděl **status picker dialog** (vybrat konkrétní stav z agregovaných)
- Filtroval podle assignee, priority, tenant (multi-select filtry)
- Používal **swimlanes** (group by: Priority, Tenant, Assignee)
- Viděl **hierarchii** (Epic → Story → Task → Subtask) v kartách
- **Drag & drop** s preservation vazeb:
  - Posunu **child task** → parent zůstane ve svém sloupci
  - Posunu **parent epic** → všechny children se přesunou s ním
  - Volba: "Move only this item" vs. "Move with children"

**Use cases:**
- **Workflows:** DRAFT → ASSIGNED → STARTED → BLOCKED → REVIEW → DONE
- **Support Tickets:** NEW → OPEN → IN_PROGRESS → WAITING_CUSTOMER → RESOLVED
- **Users:** INVITED → ACTIVE → SUSPENDED → DEACTIVATED
- **Sales Leads:** PROSPECT → QUALIFIED → PROPOSAL → NEGOTIATION → WON/LOST

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Kanban Board s Entity Instances + WF Status Mapping

**GIVEN** Workflow entity s různými stavy  
**WHEN** otevřu Kanban view  
**THEN** zobrazí se sloupce mapované na workflow stavy:

**Column Configuration (Admin setup):**

```
Kanban Columns Setup:
┌────────────────────────────────────────────────────────────────┐
│ Column: "To Do"                                                │
│ Mapped Statuses: [DRAFT, ASSIGNED] ← Multiple statuses!       │
│ Color: #E3F2FD                                                 │
│ WIP Limit: 10                                                  │
│                                                                │
│ Column: "In Progress"                                          │
│ Mapped Statuses: [STARTED, BLOCKED] ← Aggregated!             │
│ Color: #FFF9C4                                                 │
│ WIP Limit: 5                                                   │
│                                                                │
│ Column: "Review"                                               │
│ Mapped Statuses: [REVIEW, WAITING_APPROVAL]                   │
│ Color: #F8BBD0                                                 │
│                                                                │
│ Column: "Done"                                                 │
│ Mapped Statuses: [COMPLETED, CANCELLED, REJECTED]             │
│ Color: #C8E6C9                                                 │
└────────────────────────────────────────────────────────────────┘
```

**Kanban View (showing Workflow entity instances):**

```
┌──────────────┬──────────────┬──────────────┬──────────────┐
│  To Do (12)  │ In Progress  │  Review (5)  │  Done (45)   │
│ DRAFT,       │  STARTED,    │ REVIEW,      │ COMPLETED,   │
│ ASSIGNED     │  BLOCKED     │ WAITING_     │ CANCELLED    │
│              │              │ APPROVAL     │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │ ┌──────────┐ │
│ │WF-123    │ │ │WF-456    │ │ │WF-789    │ │ │WF-222    │ │
│ │Deploy v2 │ │ │User Auth │ │ │API Tests │ │ │Dashboard │ │
│ │          │ │ │          │ │ │          │ │ │          │ │
│ │📊 DRAFT  │ │ │⚡ STARTED│ │ │👀 REVIEW │ │ │✅ DONE   │ │
│ │👤 Alice  │ │ │👤 Bob    │ │ │👤 Alice  │ │ │👤 Alice  │ │
│ │🔴 High   │ │ │🟡 Medium │ │ │🟢 Low    │ │ │� Low    │ │
│ │🏢 ACME   │ │ │� Beta Co│ │ │🏢 ACME   │ │ │🏢 ACME   │ │
│ └──────────┘ │ └──────────┘ │ └──────────┘ │ └──────────┘ │
│              │              │              │              │
│ ┌──────────┐ │ ┌──────────┐ │              │              │
│ │WF-124    │ │ │WF-457    │ │              │              │
│ │📊 ASSIGNED│ │ │🚫 BLOCKED│ │              │              │
│ │...       │ │ │...       │ │              │              │
│ └──────────┘ │ └──────────┘ │              │              │
└──────────────┴──────────────┴──────────────┴──────────────┘

🔍 Filters: [Assignee: All ▼] [Priority: All ▼] [Tenant: All ▼]
📊 Swimlanes: [None ▼]  [Group by: Priority | Tenant | Assignee]
```

**Entity card = Workflow instance:**

```typescript
interface KanbanCard {
  // Entity reference
  entityType: 'WORKFLOW' | 'USER' | 'TENANT' | 'TICKET';
  entityId: string;
  
  // Workflow-specific fields
  id: string; // WF-123
  title: string; // "Deploy v2"
  status: WorkflowStatus; // DRAFT, ASSIGNED, STARTED, BLOCKED, REVIEW, DONE
  assignee: User;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  tenant: Tenant;
  
  // Hierarchie
  parentId?: string; // Epic/Story parent
  children?: KanbanCard[]; // Subtasks
  hierarchyLevel: number; // 0=Epic, 1=Story, 2=Task, 3=Subtask
}

// Column = Aggregated statuses
interface KanbanColumn {
  id: string;
  name: string; // "In Progress"
  mappedStatuses: WorkflowStatus[]; // [STARTED, BLOCKED]
  color: string;
  wipLimit?: number;
  displayOrder: number;
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

---

### AC4: Status Picker Dialog (Agregované Stavy) 🆕

**GIVEN** sloupec "In Progress" má mapované stavy: [STARTED, BLOCKED, WAITING]  
**WHEN** přetáhnu Workflow kartu do tohoto sloupce  
**THEN** zobrazí se dialog pro výběr konkrétního stavu:

**Drop do sloupce s 1 stavem:**

```
Drop WF-123 to "To Do" (mapped: [DRAFT])
→ No dialog, status auto-set to DRAFT ✅
```

**Drop do sloupce s více stavy (KLÍČOVÁ FUNKCE!):**

```
Drop WF-456 to "In Progress" (mapped: [STARTED, BLOCKED, WAITING])

┌────────────────────────────────────────┐
│ Select Workflow Status                 │
├────────────────────────────────────────┤
│ Column "In Progress" has 3 statuses:   │
│                                        │
│ ● STARTED (recommended)                │
│   Workflow is actively being worked on │
│                                        │
│ ○ BLOCKED                              │
│   Waiting for dependencies/blockers    │
│                                        │
│ ○ WAITING                              │
│   Waiting for external input           │
│                                        │
│ Previous status: DRAFT                 │
│                                        │
│ [Cancel]  [Set Status]                 │
└────────────────────────────────────────┘
```

**Po výběru:**
- Karta se přesune do sloupce "In Progress"
- `workflow.status` se nastaví na vybraný (např. `STARTED`)
- Notification: "WF-456 moved to In Progress (STARTED)"

**Backend API:**

```typescript
// frontend/src/hooks/useKanbanDrop.ts
const handleCardDrop = async (cardId: string, targetColumnId: string) => {
  const column = columns.find(c => c.id === targetColumnId);
  
  if (column.mappedStatuses.length === 1) {
    // Auto-set single status (no dialog)
    await updateWorkflowStatus(cardId, column.mappedStatuses[0]);
  } else {
    // Show status picker dialog
    const selectedStatus = await showStatusPickerDialog({
      columnName: column.name,
      availableStatuses: column.mappedStatuses,
      currentStatus: card.status,
      suggestedStatus: suggestDefaultStatus(card, column)
    });
    
    if (selectedStatus) {
      await updateWorkflowStatus(cardId, selectedStatus);
    }
  }
};

// Smart suggestion: If moving from "To Do" → "In Progress", suggest STARTED
const suggestDefaultStatus = (card: KanbanCard, targetColumn: KanbanColumn) => {
  if (card.status === 'DRAFT' && targetColumn.mappedStatuses.includes('STARTED')) {
    return 'STARTED'; // Auto-select this in dialog
  }
  return targetColumn.mappedStatuses[0]; // Default: first status
};
```

**Column configuration (Admin setup):**

```java
// backend/src/main/java/cz/muriel/core/kanban/model/KanbanColumn.java
@Entity
@Table(name = "kanban_columns")
public class KanbanColumn {
    @Id
    @GeneratedValue
    private Long id;
    
    private String name; // "In Progress"
    
    @ElementCollection
    @CollectionTable(name = "kanban_column_statuses")
    private List<WorkflowStatus> mappedStatuses; // [STARTED, BLOCKED, WAITING]
    
    private String color; // #FFF9C4
    private Integer wipLimit; // 5
    private Integer displayOrder; // 2
    private Long boardId; // Which kanban board
}

enum WorkflowStatus {
    DRAFT,
    ASSIGNED,
    STARTED,
    BLOCKED,
    WAITING,
    REVIEW,
    WAITING_APPROVAL,
    COMPLETED,
    CANCELLED,
    REJECTED
}
```

**Example configurations:**

| Column | Mapped Statuses | Use Case |
|--------|----------------|----------|
| To Do | [DRAFT, ASSIGNED] | Initial states before work starts |
| In Progress | [STARTED, BLOCKED, WAITING] | Active work with blockers |
| Review | [REVIEW, WAITING_APPROVAL] | Code review or approval needed |
| Done | [COMPLETED, CANCELLED, REJECTED] | All terminal states |

**Status picker component:**

```typescript
// frontend/src/components/kanban/StatusPickerDialog.tsx
export const StatusPickerDialog: React.FC<{
  open: boolean;
  columnName: string;
  availableStatuses: WorkflowStatus[];
  currentStatus: WorkflowStatus;
  suggestedStatus: WorkflowStatus;
  onSelect: (status: WorkflowStatus) => void;
  onCancel: () => void;
}> = ({ open, columnName, availableStatuses, suggestedStatus, onSelect }) => {
  const [selectedStatus, setSelectedStatus] = useState(suggestedStatus);

  return (
    <Dialog open={open} maxWidth="sm">
      <DialogTitle>Select Workflow Status</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Column "{columnName}" has {availableStatuses.length} statuses:
        </Typography>

        <RadioGroup value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)}>
          {availableStatuses.map(status => (
            <FormControlLabel
              key={status}
              value={status}
              control={<Radio />}
              label={
                <Box>
                  <Typography>
                    {status} {status === suggestedStatus && '(recommended)'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {getStatusDescription(status)}
                  </Typography>
                </Box>
              }
            />
          ))}
        </RadioGroup>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel}>Cancel</Button>
        <Button onClick={() => onSelect(selectedStatus)} variant="contained">
          Set Status
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

---

### AC5: Hierarchie Visualization

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

---

### AC6: Drag & Drop s Preservation Vazeb

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

---

### AC7: Column Customization + Status Mapping 🆕

**GIVEN** kanban board admin  
**WHEN** klikne "Customize Columns"  
**THEN** může editovat sloupce + mapování na WF stavy:

**Column Editor:**

```
┌────────────────────────────────────────────────────────────┐
│ Edit Column: "In Progress"                                 │
├────────────────────────────────────────────────────────────┤
│ Column Name: *                                             │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ In Progress                                            │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Mapped Workflow Statuses: * (multi-select)                │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ [STARTED ×] [BLOCKED ×] [WAITING ×]                    │ │  ← Chips
│ │                                                        │ │
│ │ Available: DRAFT, ASSIGNED, REVIEW, DONE...            │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ Color:                                                     │
│ [🟡 Yellow] [🔵 Blue] [🟢 Green] [🔴 Red] [⚪ Gray]       │
│                                                            │
│ WIP Limit (optional):                                      │
│ ┌──────┐                                                   │
│ │ 5    │ items                                             │
│ └──────┘                                                   │
│ ☐ Enforce strictly (block moves when at limit)            │
│                                                            │
│ Display Order:                                             │
│ ┌──────┐                                                   │
│ │ 2    │ (1 = leftmost column)                             │
│ └──────┘                                                   │
│                                                            │
│ [Cancel]  [Save Column]                                    │
└────────────────────────────────────────────────────────────┘
```

**Column List (Admin view):**

```
┌────────────────────────────────────────────────────────────┐
│ Kanban Board: "Workflow Management"                       │
│ Columns Configuration                                      │
├────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 1. ⠿ To Do                                             │ │  ← Drag handle
│ │    Statuses: DRAFT, ASSIGNED                           │ │
│ │    WIP Limit: None    Color: 🔵 Blue                   │ │
│ │    [Edit] [Delete]                                     │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 2. ⠿ In Progress                                       │ │
│ │    Statuses: STARTED, BLOCKED, WAITING                 │ │
│ │    WIP Limit: 5       Color: 🟡 Yellow                 │ │
│ │    [Edit] [Delete]                                     │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 3. ⠿ Review                                            │ │
│ │    Statuses: REVIEW, WAITING_APPROVAL                  │ │
│ │    WIP Limit: 3       Color: 🟣 Purple                 │ │
│ │    [Edit] [Delete]                                     │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ 4. ⠿ Done                                              │ │
│ │    Statuses: COMPLETED, CANCELLED, REJECTED            │ │
│ │    WIP Limit: None    Color: 🟢 Green                  │ │
│ │    [Edit] [Delete]                                     │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ [+ Add Column]                                             │
│                                                            │
│ ⚠️ Warning: Each workflow status must be mapped to        │
│ exactly ONE column. Unmapped statuses will not appear.     │
│                                                            │
│ [Cancel]  [Save Board]                                     │
└────────────────────────────────────────────────────────────┘
```

**Status validation:**

```typescript
// Validate that all workflow statuses are mapped to exactly one column
const validateStatusMapping = (columns: KanbanColumn[]) => {
  const allStatuses = Object.values(WorkflowStatus);
  const mappedStatuses = columns.flatMap(c => c.mappedStatuses);
  
  // Check for unmapped statuses
  const unmapped = allStatuses.filter(s => !mappedStatuses.includes(s));
  if (unmapped.length > 0) {
    throw new Error(`Unmapped statuses: ${unmapped.join(', ')}`);
  }
  
  // Check for duplicate mappings
  const duplicates = mappedStatuses.filter((s, i) => mappedStatuses.indexOf(s) !== i);
  if (duplicates.length > 0) {
    throw new Error(`Status mapped to multiple columns: ${duplicates.join(', ')}`);
  }
};
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
│ ☑ Enforce strictly (configured)        │
│                                        │
│ Cannot move card. Please complete      │
│ existing items first.                  │
│                                        │
│ [OK]                                   │
└────────────────────────────────────────┘

OR (if "Enforce strictly" is OFF):

┌────────────────────────────────────────┐
│ ⚠️ WIP Limit Warning                   │
│                                        │
│ Column "In Progress" is at capacity    │
│ (5/5 items)                            │
│                                        │
│ Move anyway?                           │
│                                        │
│ [Cancel]  [Move (6/5)]                 │
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
