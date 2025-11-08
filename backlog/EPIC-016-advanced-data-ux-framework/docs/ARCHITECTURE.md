# EPIC-016: Advanced Data UX Framework - Architecture

**Version:** 1.0  
**Date:** 2025-10-27  
**Status:** 📋 PLANNED

---

## 🏗️ COMPONENT HIERARCHY

```
┌─────────────────────────────────────────────────────────────────┐
│ App.tsx                                                         │
│ ├── MainLayout                                                  │
│ │   ├── DashboardPage                                          │
│ │   │   └── GridLayout (S3)                                    │
│ │   │       ├── Widget                                         │
│ │   │       │   ├── DataView (S1)                             │
│ │   │       │   │   ├── ViewModeSwitcher                      │
│ │   │       │   │   └── ViewRenderer                          │
│ │   │       │   │       ├── TableViewRenderer                 │
│ │   │       │   │       ├── ChartViewRenderer                 │
│ │   │       │   │       ├── PivotViewRenderer                 │
│ │   │       │   │       ├── HeatmapViewRenderer               │
│ │   │       │   │       └── CardViewRenderer                  │
│ │   │       │   ├── KPITile (S7)                              │
│ │   │       │   ├── HeatmapWidget (S7)                        │
│ │   │       │   └── NetworkGraphWidget (S7)                   │
│ │   │       └── WidgetConfigPanel                             │
│ │   │                                                           │
│ │   ├── UsersPage                                              │
│ │   │   └── DataView entity="Users" (S1)                      │
│ │   │       ├── AdvancedFilters (S2)                          │
│ │   │       │   ├── MultiSelectFilter                         │
│ │   │       │   ├── DateRangePicker                           │
│ │   │       │   └── ExportButtons (XLS/CSV/PDF)               │
│ │   │       └── TableViewRenderer                             │
│ │   │                                                           │
│ │   └── WorkflowsPage                                          │
│ │       └── DataView entity="Workflows"                       │
│ │                                                               │
│ └── PopupManager (S5)                                          │
│     └── DraggablePopup[]                                       │
│         ├── UserDetailPopup                                    │
│         │   ├── CustomizableLayout (S8)                       │
│         │   │   ├── FormFields (EPIC-014 S3)                 │
│         │   │   ├── ActivityChart                             │
│         │   │   └── AuditTable                                │
│         │   └── SaveLayoutButton                              │
│         └── TenantDetailPopup                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 DATA FLOW

### 1. DataView Query Flow

```
User Action (filter change, view mode switch)
    ↓
DataView Component
    ↓
useDataView Hook
    ↓
useCubeQuery (TanStack Query)
    ↓
Cube.js Backend API
    ↓
PostgreSQL
    ↓
Cube.js Response (JSON)
    ↓
TanStack Query Cache
    ↓
ViewRenderer (Table/Chart/Pivot)
    ↓
UI Update
```

### 2. Dashboard Layout Flow

```
User drags widget
    ↓
GridLayout onLayoutChange
    ↓
useLayoutStore (Zustand)
    ↓
POST /api/layouts (backend save)
    ↓
PostgreSQL layouts table
    ↓
Success → Update local state
```

### 3. Multi-Window Flow

```
User clicks table row
    ↓
DataView onRowClick
    ↓
useWindowManager.openWindow()
    ↓
PopupManager renders DraggablePopup
    ↓
Load detail data (GET /api/users/:id)
    ↓
Render CustomizableLayout (S8)
    ↓
User edits → markDirty(true)
    ↓
Close → unsaved warning
```

---

## 💾 STATE MANAGEMENT

### Zustand Stores

#### 1. WindowManager Store

```typescript
interface WindowManagerState {
  windows: PopupWindow[];        // Otevřené popupy
  activeWindowId: string | null;
  maxZIndex: number;
  
  openWindow(entity, recordId, title): void;
  closeWindow(id): void;
  focusWindow(id): void;
  updatePosition(id, x, y): void;
}
```

#### 2. Layout Store

```typescript
interface LayoutState {
  currentLayout: GridLayout;     // Aktivní dashboard layout
  savedLayouts: Record<string, GridLayout>; // Uložené layouts
  
  saveLayout(name, layout): void;
  loadLayout(name): void;
  updateWidget(widgetId, config): void;
}
```

#### 3. Filter Store

```typescript
interface FilterState {
  activeFilters: Record<string, Filter[]>; // Per entity
  
  addFilter(entity, filter): void;
  removeFilter(entity, filterId): void;
  clearFilters(entity): void;
}
```

### React Query Cache

```typescript
// Cube.js query cache
queryKey: ['cube', entity, { filters, dimensions, measures }]
staleTime: 5 minutes
cacheTime: 10 minutes
refetchOnWindowFocus: true
```

---

## 🎨 STYLING APPROACH

### 1. Design Tokens (EPIC-014)

```typescript
import { tokens } from '@epic-014/design-system';

const styles = {
  grid: {
    gap: tokens.spacing.md,        // 16px
    padding: tokens.spacing.lg     // 24px
  },
  widget: {
    background: tokens.colors.surface,
    border: `1px solid ${tokens.colors.border}`,
    borderRadius: tokens.radii.md  // 8px
  }
};
```

### 2. Responsive Breakpoints

```typescript
const breakpoints = {
  mobile: '@media (max-width: 767px)',
  tablet: '@media (min-width: 768px) and (max-width: 1199px)',
  desktop: '@media (min-width: 1200px)'
};
```

---

## ⚡ PERFORMANCE PATTERNS

### 1. Virtual Scrolling (react-window)

```typescript
// Pro tabulky s 10k+ řádky
import { FixedSizeList } from 'react-window';

<FixedSizeList
  height={600}
  itemCount={data.length}
  itemSize={50}
  width="100%"
>
  {({ index, style }) => (
    <div style={style}>{data[index]}</div>
  )}
</FixedSizeList>
```

### 2. Memoization

```typescript
const MemoizedChart = React.memo(ChartViewRenderer, (prev, next) => {
  return prev.data === next.data && prev.viewMode === next.viewMode;
});
```

### 3. Lazy Loading

```typescript
const HeatmapWidget = lazy(() => import('./widgets/HeatmapWidget'));
const NetworkGraph = lazy(() => import('./widgets/NetworkGraph'));

<Suspense fallback={<WidgetSkeleton />}>
  <HeatmapWidget data={data} />
</Suspense>
```

### 4. Debounced Filters

```typescript
const debouncedFilter = useDebouncedCallback((value) => {
  applyFilter(value);
}, 300);
```

---

## 🔌 API INTEGRATION

### Backend Endpoints

#### 1. Cube.js Schema

```http
GET /api/cube/schema/:entity
Response: {
  entity: "Users",
  dimensions: [...],
  measures: [...]
}
```

#### 2. Query Execution

```http
POST /api/cube/query
Body: {
  "dimensions": ["Users.name"],
  "measures": ["Users.count"],
  "filters": [{"member": "Users.status", "operator": "equals", "values": ["ACTIVE"]}]
}
Response: {
  "data": [...]
}
```

#### 3. Layout Persistence

```http
POST /api/layouts
Body: {
  "name": "My Dashboard",
  "widgets": [...]
}

GET /api/layouts/:id
PATCH /api/layouts/:id
DELETE /api/layouts/:id
```

---

## 🧩 INTEGRATION POINTS

### EPIC-004 (Reporting Infrastructure)

- **S1 Cube.js Schemas**: DataView automaticky detekuje dimensions/measures
- **Integration**: `GET /api/cube/schema/:entity` → parsování v `useDataView` hook

### EPIC-014 (Design System)

- **S3 Forms**: Použití v popup editacích (S5, S8)
- **S6 Accessibility**: WCAG 2.1 AA compliance pro všechny komponenty
- **S7 Loading**: Skeleton states pro DataView, GridLayout
- **S8 Errors**: ErrorBoundary wrapper pro widgety
- **S9 Tables**: Nahrazení MUI DataGrid (S11 integration)

### EPIC-003 (RBAC)

- **Role Detection**: S4 (Role-Based Defaults) používá role z JWT tokenu
- **Permissions**: S10 (Sharing) permission check před uložením layoutu

---

## 🧪 TESTING STRATEGY

### Unit Tests

- **Components**: DataView, GridLayout, ViewRenderers (80%+ coverage)
- **Hooks**: useDataView, useWindowManager, useCubeQuery
- **Utils**: Filter builders, export functions

### Integration Tests

- **DataView + Cube.js**: Mock Cube.js API responses
- **GridLayout + State**: Test layout save/load
- **Popup + Forms**: Test multi-window editing

### E2E Tests

- **User Journeys**: Dashboard creation, data exploration, multi-record edit
- **Performance**: Lighthouse score 90+ (desktop), 80+ (mobile)
- **Accessibility**: axe-core violations = 0

---

## 📏 CODING STANDARDS

### TypeScript

```typescript
// Strict mode
"strict": true
"noImplicitAny": true
"strictNullChecks": true

// Props interface
interface DataViewProps {
  entity: string;
  viewMode?: ViewMode;  // Optional with default
  onRowClick?: (row: any) => void;  // Optional callback
}
```

### Error Handling

```typescript
try {
  const data = await cubeQuery(params);
  return data;
} catch (error) {
  logger.error('Cube.js query failed', { error, params });
  toast.error('Failed to load data');
  throw error;  // Re-throw pro ErrorBoundary
}
```

---

## 🔐 SECURITY CONSIDERATIONS

### 1. Query Sanitization

```typescript
// Validate filters before sending to Cube.js
function sanitizeFilter(filter: Filter): Filter {
  if (!ALLOWED_OPERATORS.includes(filter.operator)) {
    throw new Error(`Invalid operator: ${filter.operator}`);
  }
  return filter;
}
```

### 2. RBAC Enforcement

```typescript
// Check permissions before layout save
if (!hasPermission(user, 'layouts.create')) {
  throw new ForbiddenError('Insufficient permissions');
}
```

---

## 📖 RELATED DOCUMENTS

- [README.md](../README.md) - EPIC Overview
- [S1: Universal Data View Engine](../stories/S1-data-view-engine.md)
- [S5: Multi-Window Editing](../stories/S5-multi-window-editing.md)
- [USER_FLOWS.md](USER_FLOWS.md) - User Journey Wireframes

---

**Version History:**

- v1.0 (2025-10-27): Initial architecture design
