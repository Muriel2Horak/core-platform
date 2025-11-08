# S15: Task Breakdown & Hierarchical Decomposition

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟢 **P2 - MEDIUM**  
**Effort:** ~35 hours  
**Sprint:** 2-3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Project Manager / Team Lead,  
**chci** rozpad velkých epics na stories, stories na tasks, tasks na subtasks (hierarchical decomposition),  
**abych**:
- Viděl **celou hierarchii** v tree view: Epic → Story → Task → Subtask
- Klikl na "Add Subtask" a vytvořil child item **bez ztráty vazby** na parent
- **Collapse/expand** větve pro lepší přehled
- Viděl **progress agregaci** (3/5 subtasks done → task 60% done)
- Přesunul subtask do jiného tasku (drag & drop **s reparentingem**)

**Use cases:**
- Epic breakdown: "Q4 Revenue Feature" → 5 stories → 15 tasks → 40 subtasks
- Sprint planning: Rozpad story na tasks, tasks assign jednotlivcům
- Daily standup: Zobrazit pouze my subtasks (filter by assignee)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Hierarchical Tree View

**GIVEN** epic s hierarchií  
**WHEN** otevřu Tree View  
**THEN** vidím celou strukturu:

**Tree visualization:**

```
📁 EPIC-123: Q4 Revenue Dashboard
│
├─ 📄 STORY-456: Analytics Module
│  │
│  ├─ ✓ TASK-789: Design mockups (DONE)
│  │
│  ├─ ⚡ TASK-790: Implement charts (IN PROGRESS)
│  │  ├─ ✓ SUBTASK-1: Line chart component
│  │  ├─ ⏳ SUBTASK-2: Bar chart component (IN PROGRESS)
│  │  └─ ⏳ SUBTASK-3: Pie chart component
│  │
│  └─ ⏳ TASK-791: Write tests
│
├─ 📄 STORY-457: Export to PDF
│  ├─ ⏳ TASK-792: PDF library integration
│  └─ ⏳ TASK-793: Export button UI
│
└─ 📄 STORY-458: User permissions
   └─ ⏳ TASK-794: RBAC implementation
```

**Tree item structure:**

```typescript
interface TreeItem {
  id: string;
  title: string;
  type: 'EPIC' | 'STORY' | 'TASK' | 'SUBTASK';
  status: 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
  assignee?: User;
  parentId?: string;
  children?: TreeItem[];
  
  // Progress tracking
  progress: {
    total: number;         // Total child items
    completed: number;     // Completed child items
    percentage: number;    // (completed / total) * 100
  };
}
```

**Visual indicators:**

| Icon | Type     | Meaning |
|------|----------|---------|
| 📁   | Epic     | High-level feature |
| 📄   | Story    | User story |
| ✓    | Task (done) | Completed task |
| ⚡   | Task (in progress) | Active task |
| ⏳   | Task (todo) | Pending task |

### AC2: Add Subtask (Preserve Parent Link)

**GIVEN** task bez subtasks  
**WHEN** kliknu "Add Subtask"  
**THEN** otevře se inline form:

**Add subtask flow:**

```
TASK-790: Implement charts (IN PROGRESS)
│
├─ [+ Add Subtask] ← Click here
│
└─ (Inline form opens)

   ┌────────────────────────────────────────┐
   │ Create Subtask                         │
   ├────────────────────────────────────────┤
   │ Title: *                               │
   │ ┌────────────────────────────────────┐ │
   │ │ Pie chart component                │ │
   │ └────────────────────────────────────┘ │
   │                                        │
   │ Assignee:                              │
   │ [Alice ▼]                              │
   │                                        │
   │ Estimated hours: [4h]                  │
   │                                        │
   │ ☑ Start in TODO status                 │
   │                                        │
   │ Parent: TASK-790 (locked)              │  ← Cannot change
   │                                        │
   │ [Cancel]  [Create Subtask]             │
   └────────────────────────────────────────┘
```

**After creation:**
- Subtask appears in tree under parent
- Parent `parentId` link preserved
- Parent progress updates: 2/3 subtasks → 66%

**Backend:**

```java
// backend/src/main/java/cz/muriel/core/tasks/service/TaskService.java
@Transactional
public Task createSubtask(Long parentId, CreateSubtaskRequest request) {
    var parent = taskRepository.findById(parentId).orElseThrow();
    
    // Validate hierarchy (max 4 levels: Epic > Story > Task > Subtask)
    if (parent.getType() == TaskType.SUBTASK) {
        throw new IllegalArgumentException("Cannot create subtask under subtask");
    }
    
    var subtask = new Task();
    subtask.setTitle(request.getTitle());
    subtask.setParentId(parentId);  // ✅ Preserve parent link
    subtask.setType(getChildType(parent.getType()));
    subtask.setStatus(TaskStatus.TODO);
    subtask.setAssigneeId(request.getAssigneeId());
    subtask.setEstimatedHours(request.getEstimatedHours());
    
    var created = taskRepository.save(subtask);
    
    // Update parent progress
    updateParentProgress(parent);
    
    return created;
}

private TaskType getChildType(TaskType parentType) {
    return switch (parentType) {
        case EPIC -> TaskType.STORY;
        case STORY -> TaskType.TASK;
        case TASK -> TaskType.SUBTASK;
        default -> throw new IllegalArgumentException("Invalid parent type");
    };
}
```

### AC3: Collapse/Expand Branches

**GIVEN** tree s hierarchií  
**WHEN** kliknu na expand/collapse icon  
**THEN** větev se rozbalí/sbalí:

**Collapsed view:**

```
📁 ▶ EPIC-123: Q4 Revenue Dashboard (3 stories, 7 tasks)
```

**Expanded view:**

```
📁 ▼ EPIC-123: Q4 Revenue Dashboard
  │
  ├─ 📄 ▶ STORY-456: Analytics Module (3 tasks)
  ├─ 📄 ▶ STORY-457: Export to PDF (2 tasks)
  └─ 📄 ▶ STORY-458: User permissions (1 task)
```

**Full expansion:**

```
📁 ▼ EPIC-123: Q4 Revenue Dashboard
  │
  ├─ 📄 ▼ STORY-456: Analytics Module
  │  ├─ ✓ TASK-789: Design mockups
  │  ├─ ⚡ ▼ TASK-790: Implement charts
  │  │  ├─ ✓ SUBTASK-1: Line chart
  │  │  ├─ ⏳ SUBTASK-2: Bar chart
  │  │  └─ ⏳ SUBTASK-3: Pie chart
  │  └─ ⏳ TASK-791: Write tests
  ...
```

**State management:**

```typescript
// frontend/src/hooks/useTreeExpansion.ts
export const useTreeExpansion = () => {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const toggleExpand = (nodeId: string) => {
    setExpanded(prev => {
      const newSet = new Set(prev);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  };

  const expandAll = (nodes: TreeItem[]) => {
    const allIds = collectAllIds(nodes);
    setExpanded(new Set(allIds));
  };

  const collapseAll = () => {
    setExpanded(new Set());
  };

  return { expanded, toggleExpand, expandAll, collapseAll };
};
```

### AC4: Progress Aggregation

**GIVEN** task s 5 subtasks (3 done, 2 in progress)  
**WHEN** zobrazím task v tree  
**THEN** vidím agregovaný progress:

**Progress visualization:**

```
⚡ TASK-790: Implement charts (60% done)
│  ├─ ✓ SUBTASK-1: Line chart (DONE)
│  ├─ ✓ SUBTASK-2: Bar chart (DONE)
│  ├─ ✓ SUBTASK-3: Pie chart (DONE)
│  ├─ ⏳ SUBTASK-4: Table chart (TODO)
│  └─ ⏳ SUBTASK-5: Heatmap (TODO)
│
│  [████████████░░░░░░░░] 60%  (3/5 done)
```

**Epic-level aggregation:**

```
📁 EPIC-123: Q4 Revenue Dashboard (25% done)
│
│  Stories:       2/3 done (66%)
│  Tasks:        5/10 done (50%)
│  Subtasks:    12/40 done (30%)
│  Overall:     19/53 items (36%)
│
│  [███████░░░░░░░░░░░░░] 36%
```

**Calculation:**

```typescript
// frontend/src/utils/calculateProgress.ts
export const calculateProgress = (item: TreeItem): ProgressMetrics => {
  if (!item.children || item.children.length === 0) {
    // Leaf node: 100% if DONE, 50% if IN_PROGRESS, 0% otherwise
    return {
      total: 1,
      completed: item.status === 'DONE' ? 1 : 0,
      percentage: item.status === 'DONE' ? 100 : item.status === 'IN_PROGRESS' ? 50 : 0
    };
  }

  // Parent node: aggregate children progress
  const childProgress = item.children.map(calculateProgress);
  const total = childProgress.reduce((sum, p) => sum + p.total, 0);
  const completed = childProgress.reduce((sum, p) => sum + p.completed, 0);

  return {
    total,
    completed,
    percentage: total > 0 ? Math.round((completed / total) * 100) : 0
  };
};
```

### AC5: Drag & Drop Reparenting

**GIVEN** subtask pod TASK-790  
**WHEN** přetáhnu subtask pod TASK-791  
**THEN** subtask se přesune a `parentId` se změní:

**Before drag:**

```
TASK-790: Implement charts
│  ├─ SUBTASK-1: Line chart
│  ├─ SUBTASK-2: Bar chart  ← Drag this
│  └─ SUBTASK-3: Pie chart

TASK-791: Write tests
```

**After drop:**

```
TASK-790: Implement charts
│  ├─ SUBTASK-1: Line chart
│  └─ SUBTASK-3: Pie chart

TASK-791: Write tests
│  └─ SUBTASK-2: Bar chart  ← Dropped here
```

**Confirmation dialog:**

```
┌────────────────────────────────────────┐
│ Move Subtask?                          │
│                                        │
│ Move "Bar chart component"             │
│ from: TASK-790 (Implement charts)      │
│ to:   TASK-791 (Write tests)           │
│                                        │
│ This will update the parent link.      │
│                                        │
│ [Cancel]  [Move Subtask]               │
└────────────────────────────────────────┘
```

**Implementation:**

```typescript
// frontend/src/components/tree/TreeNode.tsx
import { useDrag, useDrop } from 'react-dnd';

export const TreeNode: React.FC<{ item: TreeItem }> = ({ item }) => {
  const [{ isDragging }, drag] = useDrag({
    type: 'TREE_NODE',
    item: { id: item.id, type: item.type },
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  });

  const [{ isOver }, drop] = useDrop({
    accept: 'TREE_NODE',
    canDrop: (draggedItem) => {
      // Can only drop subtask under task
      return draggedItem.type === 'SUBTASK' && item.type === 'TASK';
    },
    drop: (draggedItem) => {
      onReparent(draggedItem.id, item.id);
    },
    collect: monitor => ({
      isOver: monitor.isOver()
    })
  });

  return (
    <div ref={drag} style={{ opacity: isDragging ? 0.5 : 1 }}>
      <div ref={drop} style={{ backgroundColor: isOver ? '#e3f2fd' : 'transparent' }}>
        {/* Tree node UI */}
      </div>
    </div>
  );
};
```

**Backend:**

```java
@Transactional
public Task reparentTask(Long taskId, Long newParentId) {
    var task = taskRepository.findById(taskId).orElseThrow();
    var newParent = taskRepository.findById(newParentId).orElseThrow();
    
    // Validate hierarchy (subtask can only move under task)
    if (task.getType() != TaskType.SUBTASK || newParent.getType() != TaskType.TASK) {
        throw new IllegalArgumentException("Invalid reparenting");
    }
    
    var oldParentId = task.getParentId();
    
    // Update parent link
    task.setParentId(newParentId);
    var updated = taskRepository.save(task);
    
    // Recalculate progress for both old and new parents
    updateParentProgress(oldParentId);
    updateParentProgress(newParentId);
    
    return updated;
}
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Tree View Component** (12h)

**Deliverable:**
- TreeNode component (recursive rendering)
- Expand/collapse state management
- Icon indicators (📁, 📄, ✓, ⚡, ⏳)
- Indentation based on depth

**Tech stack:**
- `@mui/x-tree-view` - Tree UI component
- OR `react-arborist` - Performance-focused tree library

**Implementation:**

```typescript
// frontend/src/components/tree/TreeView.tsx
import { TreeView, TreeItem } from '@mui/x-tree-view';
import { ExpandMore, ChevronRight } from '@mui/icons-material';

export const TaskTreeView: React.FC<{ items: TreeItem[] }> = ({ items }) => {
  const { expanded, toggleExpand } = useTreeExpansion();

  const renderTree = (node: TreeItem) => (
    <TreeItem
      key={node.id}
      nodeId={node.id}
      label={
        <Box display="flex" alignItems="center">
          {getIcon(node.type, node.status)}
          <Typography sx={{ ml: 1 }}>{node.title}</Typography>
          {node.progress && (
            <Typography variant="caption" sx={{ ml: 2, color: 'text.secondary' }}>
              ({node.progress.completed}/{node.progress.total})
            </Typography>
          )}
        </Box>
      }
    >
      {node.children?.map(child => renderTree(child))}
    </TreeItem>
  );

  return (
    <TreeView
      defaultCollapseIcon={<ExpandMore />}
      defaultExpandIcon={<ChevronRight />}
      expanded={Array.from(expanded)}
      onNodeToggle={(e, nodeIds) => {
        // Update expanded state
      }}
    >
      {items.map(renderTree)}
    </TreeView>
  );
};
```

---

#### **T2: Add Subtask Dialog** (8h)

**Deliverable:**
- Inline form to create subtask
- Parent field locked (cannot change)
- Assignee dropdown
- Estimated hours input
- Backend API integration

---

#### **T3: Progress Calculation** (6h)

**Deliverable:**
- Recursive progress aggregation
- Progress bar component
- Backend progress calculation on status change
- Cache progress for performance

---

#### **T4: Drag & Drop Reparenting** (6h)

**Deliverable:**
- react-dnd integration
- Drag subtask, drop on task
- Confirmation dialog
- Backend reparenting API

---

#### **T5: Testing** (3h)

**E2E tests:**

```typescript
// e2e/specs/tree/task-breakdown.spec.ts
test('Expand/collapse tree nodes', async ({ page }) => {
  await page.goto('/tasks/tree');

  // Collapse epic
  await page.click('[data-testid="epic-123-expand"]');

  // Verify children hidden
  await expect(page.locator('text=STORY-456')).not.toBeVisible();

  // Expand epic
  await page.click('[data-testid="epic-123-expand"]');

  // Verify children visible
  await expect(page.locator('text=STORY-456')).toBeVisible();
});

test('Add subtask', async ({ page }) => {
  await page.goto('/tasks/tree');

  // Click "Add Subtask" on TASK-790
  await page.click('[data-testid="task-790-add-subtask"]');

  // Fill form
  await page.fill('input[name="title"]', 'New Subtask');
  await page.click('button:has-text("Create Subtask")');

  // Verify subtask created
  await expect(page.locator('text=New Subtask')).toBeVisible();
  await expect(page.locator('text=Parent: TASK-790')).toBeVisible();
});

test('Drag & drop reparenting', async ({ page }) => {
  await page.goto('/tasks/tree');

  // Drag SUBTASK-2 to TASK-791
  await page.dragAndDrop(
    '[data-testid="subtask-2"]',
    '[data-testid="task-791"]'
  );

  // Confirm dialog
  await page.click('button:has-text("Move Subtask")');

  // Verify reparented
  await expect(page.locator('[data-testid="task-791"] >> text=SUBTASK-2')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Tree render < 500ms (1000 nodes)
- ✅ Expand/collapse latency < 100ms
- ✅ Progress calculation < 50ms
- ✅ Drag & drop latency < 200ms
- ✅ 70%+ epics have task breakdown (adoption)

---

## 🔗 DEPENDENCIES

- **S12:** Kanban Board (hierarchy visualization reuse)
- **S1:** DataView (task data source)
- **EPIC-003:** RBAC (permissions)
- **Libraries:**
  - `@mui/x-tree-view` OR `react-arborist`
  - `react-dnd` (drag & drop)

---

## 🎨 DESIGN INSPIRATION

**Reference apps:**
- **ClickUp** (https://clickup.com) - Task hierarchy, subtasks
- **Asana** (https://asana.com) - Subtask creation
- **Linear** (https://linear.app) - Clean tree view, progress tracking

**Key UX patterns:**
- Inline subtask creation (no modal)
- Collapse/expand with keyboard (arrow keys)
- Progress bubbles up from leaves to root
- Drag & drop with visual feedback (drop zone highlight)

---

**Status:** 📋 TODO  
**Effort:** ~35 hours (~2 sprints)  
**Next:** Update EPIC-016 README
