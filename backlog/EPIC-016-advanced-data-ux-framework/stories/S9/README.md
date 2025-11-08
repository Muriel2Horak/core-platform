# S9: Tile Click Actions

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟢 **P2 - MEDIUM**  
**Effort:** ~55 hours  
**Sprint:** 3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Dashboard User,  
**chci** konfigurovat click actions na chart bars/tiles/table rows (drill-down, external URL, modal popup),  
**abych**:
- Klikl na bar "Acme Corp" v chart → filtroval DataView na tento tenant
- Klikl na KPI tile "145 Active Users" → otevřel modal s user listem
- Klikl na table row → navigoval na detail page
- Klikl na custom action → otevřel external tool (Grafana dashboard, atd.)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Drill-Down Action (Filter DataView)

**GIVEN** bar chart zobrazující "Users per Tenant"  
**WHEN** kliknu na bar "Acme Corp"  
**THEN**:
- DataView se filtruje: `tenantName = "Acme Corp"`
- Breadcrumb navigation: "All Tenants > Acme Corp"
- URL state update: `/dashboard?filter=tenantName:Acme`
- Back button (< All Tenants) pro clear filter

**Configuration:**

```typescript
const chartConfig = {
  type: 'bar',
  onClick: {
    action: 'drill-down',
    target: 'dataview-1', // ID of DataView to filter
    filterField: 'Tenants.name'
  }
};
```

### AC2: Open Modal Action

**GIVEN** KPI tile "145 Active Users"  
**WHEN** kliknu na tile  
**THEN**:
- Otevře se modal dialog:
  - Title: "Active Users (145)"
  - Content: Table s user list (email, role, lastLogin)
  - Actions: Export CSV, Close
- Modal je draggable & resizable (reuse S5 multi-window system)

**Configuration:**

```typescript
const kpiConfig = {
  label: 'Active Users',
  value: 145,
  onClick: {
    action: 'open-modal',
    modal: {
      title: 'Active Users',
      content: {
        type: 'table',
        dataSource: 'Users',
        filters: [{ member: 'Users.status', operator: 'equals', values: ['ACTIVE'] }]
      }
    }
  }
};
```

### AC3: Navigate to URL Action

**GIVEN** table row pro User "alice@example.com"  
**WHEN** kliknu na row  
**THEN**:
- Browser naviguje na: `/users/123` (detail page)
- Nebo otevře external URL v new tab: `https://grafana.example.com/d/user-123`

**Configuration:**

```typescript
const tableConfig = {
  columns: ['email', 'role', 'status'],
  onRowClick: {
    action: 'navigate',
    url: '/users/{{userId}}', // Template with row data
    target: '_self' // or '_blank' for new tab
  }
};
```

### AC4: Custom Action (External Tool)

**GIVEN** chart bar "Revenue Spike on 2025-01-15"  
**WHEN** kliknu na bar  
**THEN**:
- Otevře se Grafana dashboard v new tab:
  - URL: `https://grafana.example.com/d/revenue?from=2025-01-15&to=2025-01-16`
  - Pre-filled time range based on clicked data point

**Configuration:**

```typescript
const chartConfig = {
  type: 'line',
  onClick: {
    action: 'external-url',
    urlTemplate: 'https://grafana.example.com/d/revenue?from={{date}}&to={{date+1d}}',
    target: '_blank'
  }
};
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Click Action Configuration System** (12h)

**Implementation:**

```typescript
// frontend/src/types/clickActions.ts
export type ClickAction =
  | DrillDownAction
  | OpenModalAction
  | NavigateAction
  | ExternalUrlAction;

interface DrillDownAction {
  action: 'drill-down';
  target: string; // DataView ID
  filterField: string; // Cube.js dimension
  filterValue?: string; // Optional static value
}

interface OpenModalAction {
  action: 'open-modal';
  modal: {
    title: string;
    content: {
      type: 'table' | 'chart' | 'custom';
      dataSource?: string;
      filters?: Filter[];
      component?: React.ComponentType;
    };
  };
}

interface NavigateAction {
  action: 'navigate';
  url: string; // Template string: "/users/{{userId}}"
  target: '_self' | '_blank';
}

interface ExternalUrlAction {
  action: 'external-url';
  urlTemplate: string; // "https://grafana.example.com?from={{date}}"
  target: '_blank';
}
```

**Usage in widget config:**

```typescript
// frontend/src/components/widgets/ChartWidget.tsx
import { ClickAction } from '@/types/clickActions';

interface ChartWidgetProps {
  config: {
    chartType: 'bar' | 'line' | 'pie';
    onClick?: ClickAction;
  };
}

export const ChartWidget: React.FC<ChartWidgetProps> = ({ config, data }) => {
  const handleClick = (event: any) => {
    if (!config.onClick) return;

    const clickedData = event.activePayload[0].payload; // Recharts event data
    executeClickAction(config.onClick, clickedData);
  };

  return (
    <ResponsiveContainer>
      <BarChart data={data} onClick={handleClick}>
        {/* ... */}
      </BarChart>
    </ResponsiveContainer>
  );
};
```

**Deliverable:** Click action type system

---

#### **T2: Drill-Down Implementation** (15h)

**Implementation:**

```typescript
// frontend/src/hooks/useClickActions.ts
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useDrillDown } from '@/components/query-builder/DrillDownManager'; // From S2

export const useClickActions = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { addDrillLevel } = useDrillDown();

  const executeClickAction = (action: ClickAction, clickedData: any) => {
    switch (action.action) {
      case 'drill-down':
        handleDrillDown(action, clickedData);
        break;
      case 'open-modal':
        handleOpenModal(action, clickedData);
        break;
      case 'navigate':
        handleNavigate(action, clickedData);
        break;
      case 'external-url':
        handleExternalUrl(action, clickedData);
        break;
    }
  };

  const handleDrillDown = (action: DrillDownAction, clickedData: any) => {
    const filterValue = action.filterValue || clickedData[action.filterField.split('.')[1]];

    // Add drill-down level (breadcrumb navigation)
    addDrillLevel({
      field: action.filterField,
      value: filterValue,
      label: filterValue
    });

    // Update URL state
    const params = new URLSearchParams(searchParams);
    params.set('filter', `${action.filterField}:${filterValue}`);
    setSearchParams(params);

    // Notify DataView to apply filter (via event bus or context)
    window.dispatchEvent(new CustomEvent('dataview-filter', {
      detail: {
        targetId: action.target,
        filter: {
          member: action.filterField,
          operator: 'equals',
          values: [filterValue]
        }
      }
    }));
  };

  const handleOpenModal = (action: OpenModalAction, clickedData: any) => {
    // Use multi-window manager from S5
    const { openWindow } = useWindowManager();

    openWindow({
      id: `modal-${Date.now()}`,
      title: interpolateTemplate(action.modal.title, clickedData),
      content: <ModalContent config={action.modal.content} data={clickedData} />,
      width: 800,
      height: 600
    });
  };

  const handleNavigate = (action: NavigateAction, clickedData: any) => {
    const url = interpolateTemplate(action.url, clickedData);

    if (action.target === '_blank') {
      window.open(url, '_blank');
    } else {
      navigate(url);
    }
  };

  const handleExternalUrl = (action: ExternalUrlAction, clickedData: any) => {
    const url = interpolateTemplate(action.urlTemplate, clickedData);
    window.open(url, action.target);
  };

  return { executeClickAction };
};

function interpolateTemplate(template: string, data: any): string {
  return template.replace(/{{(\w+)}}/g, (_, key) => {
    return data[key] || '';
  });
}
```

**Deliverable:** Drill-down with URL state sync

---

#### **T3: Modal Content Renderer** (10h)

**Implementation:**

```typescript
// frontend/src/components/actions/ModalContent.tsx
import React from 'react';
import { DataView } from '@/components/data-view/DataView'; // From S1
import { Box } from '@mui/material';

interface ModalContentProps {
  config: {
    type: 'table' | 'chart' | 'custom';
    dataSource?: string;
    filters?: Filter[];
    component?: React.ComponentType<any>;
  };
  data: any;
}

export const ModalContent: React.FC<ModalContentProps> = ({ config, data }) => {
  if (config.type === 'custom' && config.component) {
    const CustomComponent = config.component;
    return <CustomComponent data={data} />;
  }

  // Use DataView component from S1
  return (
    <Box sx={{ p: 2 }}>
      <DataView
        dataSource={config.dataSource!}
        viewMode={config.type === 'table' ? 'table' : 'chart'}
        filters={config.filters}
      />
    </Box>
  );
};
```

**Deliverable:** Modal content with DataView

---

#### **T4: Navigation State Management** (8h)

**Implementation:**

```typescript
// frontend/src/stores/navigationStore.ts
import { create } from 'zustand';

interface NavigationState {
  history: Array<{ url: string; timestamp: number }>;
  currentAction: ClickAction | null;

  pushNavigation: (url: string) => void;
  popNavigation: () => void;
  setCurrentAction: (action: ClickAction | null) => void;
}

export const useNavigation = create<NavigationState>((set, get) => ({
  history: [],
  currentAction: null,

  pushNavigation: (url) => {
    set(state => ({
      history: [...state.history, { url, timestamp: Date.now() }]
    }));
  },

  popNavigation: () => {
    const navigate = useNavigate();
    const history = get().history;
    if (history.length > 1) {
      const previous = history[history.length - 2];
      navigate(previous.url);
      set({ history: history.slice(0, -1) });
    }
  },

  setCurrentAction: (action) => set({ currentAction: action })
}));
```

**Deliverable:** Navigation state tracking

---

#### **T5: Click Action Configuration UI** (10h)

**Implementation:**

```typescript
// frontend/src/components/actions/ClickActionConfig.tsx
import React from 'react';
import { Box, Select, MenuItem, TextField, Typography } from '@mui/material';

interface ClickActionConfigProps {
  value: ClickAction;
  onChange: (action: ClickAction) => void;
}

export const ClickActionConfig: React.FC<ClickActionConfigProps> = ({ value, onChange }) => {
  const [actionType, setActionType] = useState(value?.action || 'drill-down');

  const handleActionTypeChange = (newType: string) => {
    setActionType(newType);
    // Initialize with default config for selected type
    onChange(getDefaultConfig(newType));
  };

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>On Click Action</Typography>

      <Select
        value={actionType}
        onChange={(e) => handleActionTypeChange(e.target.value)}
        fullWidth
        sx={{ mb: 2 }}
      >
        <MenuItem value="drill-down">Drill-Down (Filter Data)</MenuItem>
        <MenuItem value="open-modal">Open Modal</MenuItem>
        <MenuItem value="navigate">Navigate to Page</MenuItem>
        <MenuItem value="external-url">External URL</MenuItem>
      </Select>

      {/* Type-specific fields */}
      {actionType === 'drill-down' && (
        <>
          <TextField
            label="Target DataView ID"
            value={(value as DrillDownAction).target || ''}
            onChange={(e) => onChange({ ...value, target: e.target.value })}
            fullWidth
            sx={{ mb: 2 }}
          />
          <TextField
            label="Filter Field"
            value={(value as DrillDownAction).filterField || ''}
            onChange={(e) => onChange({ ...value, filterField: e.target.value })}
            fullWidth
            placeholder="Users.tenantName"
          />
        </>
      )}

      {actionType === 'navigate' && (
        <>
          <TextField
            label="URL Template"
            value={(value as NavigateAction).url || ''}
            onChange={(e) => onChange({ ...value, url: e.target.value })}
            fullWidth
            placeholder="/users/{{userId}}"
            sx={{ mb: 2 }}
          />
          <Select
            label="Target"
            value={(value as NavigateAction).target || '_self'}
            onChange={(e) => onChange({ ...value, target: e.target.value })}
            fullWidth
          >
            <MenuItem value="_self">Same Tab</MenuItem>
            <MenuItem value="_blank">New Tab</MenuItem>
          </Select>
        </>
      )}
    </Box>
  );
};

function getDefaultConfig(actionType: string): ClickAction {
  switch (actionType) {
    case 'drill-down':
      return { action: 'drill-down', target: '', filterField: '' };
    case 'open-modal':
      return { action: 'open-modal', modal: { title: '', content: { type: 'table' } } };
    case 'navigate':
      return { action: 'navigate', url: '', target: '_self' };
    case 'external-url':
      return { action: 'external-url', urlTemplate: '', target: '_blank' };
    default:
      return { action: 'drill-down', target: '', filterField: '' };
  }
}
```

**Deliverable:** UI for configuring click actions

---

## 🧪 TESTING

```typescript
// e2e/specs/actions/tile-click-actions.spec.ts
import { test, expect } from '@playwright/test';

test('Drill-down on chart bar click', async ({ page }) => {
  await page.goto('/dashboard');

  // Click on bar chart bar
  await page.click('.recharts-bar >> nth=0');

  // Verify drill-down breadcrumb
  await expect(page.locator('text=All Tenants > Acme Corp')).toBeVisible();

  // Verify URL updated
  expect(page.url()).toContain('filter=tenantName:Acme');

  // Verify DataView filtered
  await expect(page.locator('.data-view')).toContainText('Acme Corp');
});

test('Open modal on KPI tile click', async ({ page }) => {
  await page.goto('/dashboard');

  // Click KPI tile
  await page.click('[data-kpi-id="active-users"]');

  // Verify modal opened
  await expect(page.locator('.modal-dialog >> text=Active Users')).toBeVisible();
  await expect(page.locator('.modal-dialog .data-table')).toBeVisible();
});

test('Navigate on table row click', async ({ page }) => {
  await page.goto('/dashboard');

  // Click table row
  await page.click('.data-table tbody tr >> nth=0');

  // Verify navigation
  await expect(page).toHaveURL(/\/users\/\d+/);
});
```

---

## 📊 SUCCESS METRICS

- ✅ Click action execution < 100ms
- ✅ Drill-down navigation intuitive (90% users understand breadcrumbs)
- ✅ Modal open < 500ms

---

## 🔗 DEPENDENCIES

- **S1:** DataView component (for modal content)
- **S2:** DrillDownManager (breadcrumb navigation)
- **S5:** Multi-window system (for modals)

---

**Status:** 📋 TODO  
**Next:** S10: Layout Sharing

