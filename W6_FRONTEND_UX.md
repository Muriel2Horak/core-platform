# W6: Frontend UX - Workflow Visualization & Interaction

**Status:** ✅ Complete  
**Tag:** `studio-workflow-W6`  
**Date:** 2025-01-14

---

## 📋 Overview

W6 delivers **interactive workflow visualization** with real-time state tracking, action management, and collaborative editing through presence-aware locking.

### Key Features
- **WorkflowGraph**: React Flow visualization with layout algorithms (ELK/Dagre)
- **TimelinePanel**: Historical timeline with durations and SLA tracking
- **ActionsBar**: Context-aware action buttons with lock detection
- **Presence Lock**: Multi-user collaboration with read-only mode

---

## 🎨 Components

### 1. WorkflowGraph Component

**Location:** `frontend/src/components/Workflow/WorkflowGraph.tsx`

#### Features
- **Current State Highlighting**: Blue border + background for active node
- **Edge Visualization**:
  - ✅ Allowed transitions: Green stroke, animated flow
  - ❌ Blocked transitions: Gray stroke, static, with "why not" tooltip
- **Layout Engines**:
  - **ELK** (default): Hierarchical layout for complex workflows
  - **Dagre**: Compact layout for simple workflows
- **Legend**: Visual guide for state indicators

#### Props
```typescript
interface WorkflowGraphProps {
  graph: {
    entityType: string;
    entityId: string;
    currentState: string | null;
    nodes: Array<{
      id: string;
      code: string;
      label: string;
      type: string;
      current: boolean;
      metadata?: Record<string, any>;
    }>;
    edges: Array<{
      id: string;
      source: string;
      target: string;
      label: string;
      transitionCode: string;
      allowed: boolean;
      whyNot?: string;
      slaMinutes?: number;
    }>;
  };
}
```

#### Usage
```tsx
import { WorkflowGraph } from '@/components/Workflow';

<WorkflowGraph graph={workflowGraphData} />
```

#### Visual Indicators
| State | Border Color | Background | Icon |
|-------|-------------|------------|------|
| Current | Blue (#2196f3) | Light Blue | ● |
| Past | Gray (#757575) | White | ○ |
| Future | Gray (#757575) | White | ○ |

| Transition | Stroke Color | Animation | Tooltip |
|-----------|-------------|-----------|---------|
| Allowed | Green (#4caf50) | ✅ Animated | - |
| Blocked | Gray (#9e9e9e) | ❌ Static | "Why not" reason |

---

### 2. TimelinePanel Component

**Location:** `frontend/src/components/Workflow/TimelinePanel.tsx`

#### Features
- **MUI Timeline**: Vertical timeline with connectors
- **Duration Display**: Human-readable format (5m 30s, 2h 15m, 3d 12h)
- **SLA Badges**:
  - ✅ **OK**: Green with checkmark icon
  - ⚠️ **WARN**: Orange with warning icon
  - 🚨 **BREACH**: Red with error icon
- **Actor Tracking**: Shows who performed each transition
- **Relative Timestamps**: "2 hours ago" using date-fns

#### Props
```typescript
interface TimelinePanelProps {
  history: {
    entityType: string;
    entityId: string;
    entries: Array<{
      eventType: string;
      fromState: string | null;
      toState: string;
      transitionCode: string;
      timestamp: string;
      durationMs: number;
      actor: string;
      slaStatus: 'NONE' | 'OK' | 'WARN' | 'BREACH';
    }>;
    totalDurationMs: number;
  };
}
```

#### Usage
```tsx
import { TimelinePanel } from '@/components/Workflow';

<TimelinePanel history={workflowHistory} />
```

#### Duration Formatting
```typescript
300000 ms    → "5m 0s"
3600000 ms   → "1h 0m"
7200000 ms   → "2h 0m"
86400000 ms  → "1d 0h"
```

---

### 3. ActionsBar Component

**Location:** `frontend/src/components/Workflow/ActionsBar.tsx`

#### Features
- **Allowed Actions**: Buttons for permitted transitions from current state
- **Lock Detection**: Read-only mode when workflow locked by another user
- **Stale Data Refresh**: Auto-refresh before action apply (30s timeout)
- **Disabled State**: Tooltips explaining "why not" for blocked actions

#### Props
```typescript
interface ActionsBarProps {
  entityType: string;
  entityId: string;
  currentState: string;
  allowedActions: Array<{
    code: string;
    targetState: string;
    label: string;
    enabled: boolean;
    reason?: string;
  }>;
  isLocked: boolean;
  lockedBy?: string;
  onActionApply: (actionCode: string) => Promise<void>;
  onRefresh: () => Promise<void>;
}
```

#### Usage
```tsx
import { ActionsBar } from '@/components/Workflow';

<ActionsBar
  entityType="order"
  entityId="123"
  currentState="submitted"
  allowedActions={actions}
  isLocked={isLocked}
  lockedBy={lockOwner}
  onActionApply={handleApply}
  onRefresh={handleRefresh}
/>
```

#### Lock States
| State | Icon | Warning | Actions |
|-------|------|---------|---------|
| Unlocked | 🔓 (green) | - | Enabled |
| Locked (self) | 🔒 (orange) | "You have locked this workflow" | Enabled |
| Locked (other) | 🔒 (red) | "Locked by user@example.com" | Disabled (read-only) |

---

## 🔐 Presence Lock System

### Architecture

```
┌─────────────────┐
│  User A Browser │
│                 │
│ 1. Opens WF     │──────┐
│ 2. Publishes    │      │
│    LOCK event   │      ▼
└─────────────────┘  ┌───────────────┐
                     │ Kafka Topic:  │
                     │ workflow.locks│
                     └───────────────┘
                          │     ▲
                          ▼     │
                     ┌────────────────┐
                     │ Backend        │
                     │ Lock Cache     │
                     │ (5min TTL)     │
                     └────────────────┘
                              │
                              ▼
                     ┌───────────────────┐
                     │ REST API          │
                     │ GET /lock-status  │
                     └───────────────────┘
                              │
                              ▼
┌─────────────────────────────────────┐
│  User B Browser                     │
│                                     │
│ 1. Polls /lock-status (2s interval)│
│ 2. Receives {locked: true}          │
│ 3. ActionsBar → read-only mode      │
└─────────────────────────────────────┘
```

### Lock Event Schema
```typescript
{
  entityType: "order",
  entityId: "123",
  action: "LOCK" | "UNLOCK",
  userId: "user@example.com",
  timestamp: 1705234567890
}
```

### API Endpoints

#### GET /api/workflows/{entity}/{id}/lock-status
**Response:**
```json
{
  "locked": true,
  "lockedBy": "jane.doe@example.com",
  "lockedAt": "2025-01-14T10:30:00Z"
}
```

#### POST /api/workflows/{entity}/{id}/lock
**Request:**
```json
{
  "userId": "john.doe@example.com"
}
```

#### POST /api/workflows/{entity}/{id}/unlock
**Request:**
```json
{
  "userId": "john.doe@example.com"
}
```

---

## 🎨 Layout Algorithms

### ELK (Eclipse Layout Kernel)
**File:** `frontend/src/hooks/useElkLayout.ts`

- **Best for**: Complex workflows with many nodes
- **Algorithm**: Hierarchical (layered) layout
- **Direction**: Top-to-bottom
- **Spacing**: 100px between layers, 80px between nodes

### Dagre
**File:** `frontend/src/hooks/useDagreLayout.ts`

- **Best for**: Simple workflows with few nodes
- **Algorithm**: Directed acyclic graph layout
- **Direction**: Top-to-bottom
- **Spacing**: 100px rank separation, 80px node separation

### Toggle Usage
```tsx
const [layout, setLayout] = useState<'elk' | 'dagre'>('elk');

<ToggleButtonGroup value={layout} exclusive onChange={(e, val) => setLayout(val)}>
  <ToggleButton value="elk">ELK</ToggleButton>
  <ToggleButton value="dagre">Dagre</ToggleButton>
</ToggleButtonGroup>
```

---

## 🧪 Testing

### Unit Tests (Vitest + RTL)

**Files:**
- `frontend/src/components/Workflow/__tests__/WorkflowGraph.test.tsx` (7 tests)
- `frontend/src/components/Workflow/__tests__/TimelinePanel.test.tsx` (8 tests)
- `frontend/src/components/Workflow/__tests__/ActionsBar.test.tsx` (9 tests)

**Coverage:**
- Component rendering ✅
- User interactions (click, hover) ✅
- Conditional logic (locked/unlocked, stale/fresh) ✅
- Edge cases (empty, disabled) ✅

**Run:**
```bash
cd frontend && npm test -- WorkflowGraph TimelinePanel ActionsBar
```

### Integration Test (Java + Testcontainers)

**File:** `backend/src/test/java/com/platform/workflow/PresenceLockIT.java` (4 tests)

**Scenarios:**
1. Lock signal → Actions disabled
2. Unlock signal → Actions enabled
3. Multiple users → First-come-first-served
4. Lock expiration → Auto-unlock after 5 minutes

**Run:**
```bash
cd backend && ./mvnw test -Dtest=PresenceLockIT
```

### E2E Test (Playwright)

**File:** `e2e/pre/06_workflow_ux.spec.ts` (8 tests)

**Flow:**
1. Create workflow instance (draft → submitted)
2. Verify graph rendering (current state blue)
3. Toggle layout (elk ↔ dagre)
4. Check edge styling (green/gray, animated/static)
5. Validate timeline (durations, SLA badges)
6. Test action buttons (enabled/disabled)
7. Simulate lock → Read-only mode
8. Unlock → Editable mode

**Run:**
```bash
npm run test:e2e -- pre/06_workflow_ux
```

---

## 📦 Dependencies

### Frontend
- `reactflow`: ^11.10.0 - Graph visualization
- `@mui/lab`: ^5.0.0-alpha.170 - Timeline components
- `elkjs`: ^0.11.0 - ELK layout algorithm
- `dagre`: ^0.8.5 - Dagre layout algorithm
- `date-fns`: ^2.30.0 - Date formatting

### Backend
- `spring-kafka` - Lock signal publishing/consuming
- `testcontainers-kafka` - Integration testing
- `awaitility` - Async assertions

---

## 🚀 Next Steps (W7)

**W7: Workflow Execution Engine**
- Action execution with rollback support
- Async workflows with timers
- Compensation logic for failed transitions
- Event sourcing for audit trail

**Tag:** `studio-workflow-W7`

---

## 📝 Commits

1. `W6: Frontend UX - Graph, Timeline, Actions (Scope)` - beb1444
2. `W6: Frontend UX Unit Tests (Vitest + RTL)` - adc4a8d
3. `W6: Presence Lock Integration Test` - 376c48e
4. `W6: Workflow UX E2E Test (Playwright)` - 9c45a32
5. `W6: Documentation (W6_FRONTEND_UX.md)` - (current)

---

**Author:** GitHub Copilot  
**Sprint:** Workflow + Studio EPIC (W5-W12)  
**Phase:** W6 - Frontend UX  
**Status:** ✅ Complete
