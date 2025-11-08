# S3: Dashboard Grid Layout

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🔴 **P0 - CRITICAL**  
**Effort:** ~70 hours  
**Sprint:** 3-4  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Dashboard Designer / Power User,  
**chci** 12-column drag-and-drop grid system pro umístění widgets (KPI tiles, charts, tables),  
**abych** mohl:
- Vytvořit custom dashboard layout přetažením widgets
- Měnit velikost widgets podle potřeby (malé KPI tile vs. velký chart)
- Uložit layout pro opakované použití
- Vidět responsive dashboard na tablet/mobilu
- Navigovat keyboard shortcuts (bez myši)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: 12-Column Responsive Grid

**GIVEN** prázdný dashboard  
**WHEN** přidám widget  
**THEN** zobrazí se v 12-column grid:
- **Desktop (>1200px)**: 12 columns
- **Tablet (768-1200px)**: 6 columns (widgets se auto-reorganizují)
- **Mobile (<768px)**: 1 column (stacked layout)

**AND** grid spacing:
- Gap mezi widgets: 16px
- Padding okolo gridu: 24px

### AC2: Drag & Drop Widgets

**GIVEN** dashboard s 3 widgets  
**WHEN** drag widget A na pozici widget B  
**THEN**:
- Widget A se přesune
- Widget B se posune (collision detection)
- Placeholder ukazuje budoucí pozici během draggu
- Snap to grid (auto-align na column boundaries)

**AND** drag handle:
- Pouze header widgetu je draggable (ne celý content)
- Cursor změní na `move` při hoveru nad header

### AC3: Widget Resize

**GIVEN** widget v gridu  
**WHEN** hover nad widget okraji  
**THEN** zobrazí se 8 resize handles:
- **Corners**: NE, NW, SE, SW (diagonal resize)
- **Sides**: N, S, E, W (vertical/horizontal resize)
- Cursor změní na resize ikonu (`nwse-resize`, `nesw-resize`, atd.)

**WHEN** drag resize handle  
**THEN**:
- Widget se mění velikost v real-time
- Respektuje min/max size constraints:
  - Min: 2 columns × 2 rows
  - Max: 12 columns × 10 rows
- Ostatní widgety se přesouvají (collision avoidance)

### AC4: Layout Persistence

**GIVEN** uložený dashboard layout  
**WHEN** refresh stránky (F5)  
**THEN** layout se obnoví:
- Stejné pozice widgets
- Stejné velikosti
- Stejný pořadí (z-index)

**AND** backend save:
```http
POST /api/dashboards/:id/layout
{
  "widgets": [
    {"id": "widget-1", "x": 0, "y": 0, "w": 6, "h": 4},
    {"id": "widget-2", "x": 6, "y": 0, "w": 6, "h": 4}
  ]
}
```

### AC5: Keyboard Navigation

**GIVEN** focused widget v gridu  
**WHEN** stisknu klávesy  
**THEN** widget se ovládá:
- `Arrow keys` → Posun widget o 1 column/row
- `Shift + Arrows` → Resize widget
- `Tab` → Focus další widget
- `Delete` → Smaž widget (s confirm dialogem)
- `Ctrl + Z` → Undo poslední změny

### AC6: Accessibility (WCAG 2.1 AA)

**GIVEN** screen reader uživatel  
**WHEN** interakce s gridem  
**THEN** screen reader oznamuje:
- "Widget added at column 3, row 2"
- "Widget moved from column 1 to column 5"
- "Widget resized to 6 columns by 4 rows"
- "Focus on KPI Tile widget, position 3 of 8"

**AND** ARIA attributes:
- `role="grid"` na container
- `role="gridcell"` na každém widgetu
- `aria-label` s popisem widgetu
- Focus indicators (blue border, 3px)

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: GridLayout Component Setup** (12h)

**Cíl:** Základní react-grid-layout integration

**Implementation:**

```typescript
// frontend/src/components/dashboard/GridLayout.tsx
import React, { useState } from 'react';
import RGL, { WidthProvider, Layout } from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

const ReactGridLayout = WidthProvider(RGL);

interface GridLayoutProps {
  widgets: Widget[];
  onLayoutChange?: (layout: Layout[]) => void;
  editable?: boolean;
}

interface Widget {
  id: string;
  type: 'kpi' | 'chart' | 'table' | 'custom';
  content: React.ReactNode;
  defaultLayout?: LayoutItem;
}

interface LayoutItem {
  i: string;
  x: number;
  y: number;
  w: number;
  h: number;
  minW?: number;
  minH?: number;
  maxW?: number;
  maxH?: number;
}

export const GridLayout: React.FC<GridLayoutProps> = ({
  widgets,
  onLayoutChange,
  editable = true
}) => {
  const [layout, setLayout] = useState<Layout[]>(
    widgets.map(w => w.defaultLayout || generateDefaultLayout(w))
  );

  const handleLayoutChange = (newLayout: Layout[]) => {
    setLayout(newLayout);
    onLayoutChange?.(newLayout);
  };

  return (
    <ReactGridLayout
      className="dashboard-grid"
      layout={layout}
      cols={12}
      rowHeight={60}
      width={1200}
      isDraggable={editable}
      isResizable={editable}
      onLayoutChange={handleLayoutChange}
      compactType="vertical"
      preventCollision={false}
      margin={[16, 16]}
      containerPadding={[24, 24]}
      draggableHandle=".widget-header"
      resizeHandles={['n', 's', 'e', 'w', 'ne', 'nw', 'se', 'sw']}
    >
      {widgets.map(widget => (
        <div key={widget.id} className="widget-container">
          <WidgetWrapper
            widget={widget}
            editable={editable}
            onRemove={() => handleRemoveWidget(widget.id)}
          />
        </div>
      ))}
    </ReactGridLayout>
  );
};

function generateDefaultLayout(widget: Widget): LayoutItem {
  return {
    i: widget.id,
    x: 0,
    y: Infinity, // Auto-place at bottom
    w: widget.type === 'kpi' ? 3 : 6,
    h: widget.type === 'kpi' ? 2 : 4,
    minW: 2,
    minH: 2,
    maxW: 12,
    maxH: 10
  };
}
```

**Widget Wrapper:**

```typescript
// frontend/src/components/dashboard/WidgetWrapper.tsx
import React from 'react';
import { Paper, IconButton, Box } from '@mui/material';
import { DragIndicator, Close } from '@mui/icons-material';

interface WidgetWrapperProps {
  widget: Widget;
  editable: boolean;
  onRemove: () => void;
}

export const WidgetWrapper: React.FC<WidgetWrapperProps> = ({
  widget,
  editable,
  onRemove
}) => {
  return (
    <Paper
      elevation={2}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}
    >
      {/* Header (drag handle) */}
      <Box
        className="widget-header"
        sx={{
          display: 'flex',
          alignItems: 'center',
          p: 1,
          borderBottom: '1px solid',
          borderColor: 'divider',
          cursor: editable ? 'move' : 'default',
          backgroundColor: 'background.default'
        }}
      >
        {editable && <DragIndicator sx={{ mr: 1, color: 'text.secondary' }} />}
        <Box sx={{ flexGrow: 1, fontWeight: 500 }}>
          {widget.title || `${widget.type} Widget`}
        </Box>
        {editable && (
          <IconButton size="small" onClick={onRemove}>
            <Close fontSize="small" />
          </IconButton>
        )}
      </Box>

      {/* Content */}
      <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
        {widget.content}
      </Box>
    </Paper>
  );
};
```

**Deliverable:** Funkční 12-column grid s drag & drop

---

#### **T2: Responsive Breakpoints** (8h)

**Implementation:**

```typescript
// Extend GridLayout with responsive breakpoints
import { Responsive, WidthProvider } from 'react-grid-layout';

const ResponsiveGridLayout = WidthProvider(Responsive);

export const GridLayout: React.FC<GridLayoutProps> = ({ ... }) => {
  const [layouts, setLayouts] = useState<{ [key: string]: Layout[] }>({
    lg: generateLayout(widgets, 12),  // Desktop
    md: generateLayout(widgets, 6),   // Tablet
    sm: generateLayout(widgets, 1)    // Mobile
  });

  const breakpoints = { lg: 1200, md: 768, sm: 0 };
  const cols = { lg: 12, md: 6, sm: 1 };

  return (
    <ResponsiveGridLayout
      className="dashboard-grid"
      layouts={layouts}
      breakpoints={breakpoints}
      cols={cols}
      rowHeight={60}
      onLayoutChange={(_, allLayouts) => setLayouts(allLayouts)}
      // ... rest of props
    >
      {/* ... widgets */}
    </ResponsiveGridLayout>
  );
};

function generateLayout(widgets: Widget[], cols: number): Layout[] {
  return widgets.map((widget, index) => {
    const widthRatio = cols / 12; // Scale width to available columns
    return {
      i: widget.id,
      x: (index % (cols / 2)) * (cols / 2), // 2 widgets per row
      y: Math.floor(index / (cols / 2)) * 4,
      w: Math.min(widget.defaultLayout?.w || 6, cols) * widthRatio,
      h: widget.defaultLayout?.h || 4
    };
  });
}
```

**Deliverable:** Responsive grid pro desktop/tablet/mobile

---

#### **T3: Layout State Management (Zustand)** (15h)

**Implementation:**

```typescript
// frontend/src/stores/dashboardLayoutStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { Layout } from 'react-grid-layout';

interface DashboardLayoutState {
  layouts: Record<string, Layout[]>; // Key: dashboardId
  currentDashboardId: string | null;
  
  saveLayout: (dashboardId: string, layout: Layout[]) => Promise<void>;
  loadLayout: (dashboardId: string) => Promise<Layout[]>;
  updateWidget: (widgetId: string, updates: Partial<Layout>) => void;
  removeWidget: (widgetId: string) => void;
  addWidget: (widget: Widget) => void;
  undo: () => void;
  redo: () => void;
}

export const useDashboardLayout = create<DashboardLayoutState>()(
  persist(
    (set, get) => ({
      layouts: {},
      currentDashboardId: null,
      history: [],
      historyIndex: -1,

      saveLayout: async (dashboardId, layout) => {
        try {
          // Save to backend
          await fetch(`/api/dashboards/${dashboardId}/layout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ layout })
          });

          // Update local state
          set(state => ({
            layouts: {
              ...state.layouts,
              [dashboardId]: layout
            }
          }));

          // Add to history
          const newHistory = [...get().history.slice(0, get().historyIndex + 1), layout];
          set({ history: newHistory, historyIndex: newHistory.length - 1 });
        } catch (error) {
          console.error('Failed to save layout:', error);
          throw error;
        }
      },

      loadLayout: async (dashboardId) => {
        try {
          const response = await fetch(`/api/dashboards/${dashboardId}/layout`);
          const { layout } = await response.json();

          set(state => ({
            layouts: {
              ...state.layouts,
              [dashboardId]: layout
            },
            currentDashboardId: dashboardId
          }));

          return layout;
        } catch (error) {
          console.error('Failed to load layout:', error);
          return [];
        }
      },

      updateWidget: (widgetId, updates) => {
        const dashboardId = get().currentDashboardId;
        if (!dashboardId) return;

        set(state => ({
          layouts: {
            ...state.layouts,
            [dashboardId]: state.layouts[dashboardId].map(item =>
              item.i === widgetId ? { ...item, ...updates } : item
            )
          }
        }));
      },

      removeWidget: (widgetId) => {
        const dashboardId = get().currentDashboardId;
        if (!dashboardId) return;

        set(state => ({
          layouts: {
            ...state.layouts,
            [dashboardId]: state.layouts[dashboardId].filter(item => item.i !== widgetId)
          }
        }));
      },

      addWidget: (widget) => {
        const dashboardId = get().currentDashboardId;
        if (!dashboardId) return;

        const newItem = generateDefaultLayout(widget);
        set(state => ({
          layouts: {
            ...state.layouts,
            [dashboardId]: [...(state.layouts[dashboardId] || []), newItem]
          }
        }));
      },

      undo: () => {
        const { history, historyIndex, currentDashboardId } = get();
        if (historyIndex > 0 && currentDashboardId) {
          const previousLayout = history[historyIndex - 1];
          set(state => ({
            layouts: {
              ...state.layouts,
              [currentDashboardId]: previousLayout
            },
            historyIndex: historyIndex - 1
          }));
        }
      },

      redo: () => {
        const { history, historyIndex, currentDashboardId } = get();
        if (historyIndex < history.length - 1 && currentDashboardId) {
          const nextLayout = history[historyIndex + 1];
          set(state => ({
            layouts: {
              ...state.layouts,
              [currentDashboardId]: nextLayout
            },
            historyIndex: historyIndex + 1
          }));
        }
      }
    }),
    {
      name: 'dashboard-layout-storage',
      partialize: (state) => ({ layouts: state.layouts })
    }
  )
);
```

**Usage:**

```typescript
const DashboardPage = () => {
  const { loadLayout, saveLayout } = useDashboardLayout();
  const dashboardId = 'main-dashboard';

  useEffect(() => {
    loadLayout(dashboardId);
  }, [dashboardId]);

  const handleLayoutChange = (newLayout: Layout[]) => {
    saveLayout(dashboardId, newLayout);
  };

  return <GridLayout onLayoutChange={handleLayoutChange} ... />;
};
```

**Deliverable:** Zustand store s persistence a undo/redo

---

#### **T4: Keyboard Navigation** (10h)

**Implementation:**

```typescript
// frontend/src/hooks/useGridKeyboard.ts
import { useEffect } from 'react';
import { useDashboardLayout } from '@/stores/dashboardLayoutStore';

export const useGridKeyboard = (widgets: Widget[]) => {
  const { updateWidget, removeWidget, undo, redo } = useDashboardLayout();
  const [focusedWidgetId, setFocusedWidgetId] = useState<string | null>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!focusedWidgetId) return;

      const widget = widgets.find(w => w.id === focusedWidgetId);
      if (!widget) return;

      // Arrow keys → Move widget
      if (e.key.startsWith('Arrow') && !e.shiftKey) {
        e.preventDefault();
        const delta = { x: 0, y: 0 };
        if (e.key === 'ArrowLeft') delta.x = -1;
        if (e.key === 'ArrowRight') delta.x = 1;
        if (e.key === 'ArrowUp') delta.y = -1;
        if (e.key === 'ArrowDown') delta.y = 1;

        updateWidget(focusedWidgetId, {
          x: Math.max(0, widget.layout.x + delta.x),
          y: Math.max(0, widget.layout.y + delta.y)
        });

        announceToScreenReader(
          `Widget moved to column ${widget.layout.x + delta.x}, row ${widget.layout.y + delta.y}`
        );
      }

      // Shift + Arrow → Resize widget
      if (e.key.startsWith('Arrow') && e.shiftKey) {
        e.preventDefault();
        const delta = { w: 0, h: 0 };
        if (e.key === 'ArrowLeft') delta.w = -1;
        if (e.key === 'ArrowRight') delta.w = 1;
        if (e.key === 'ArrowUp') delta.h = -1;
        if (e.key === 'ArrowDown') delta.h = 1;

        updateWidget(focusedWidgetId, {
          w: Math.max(2, Math.min(12, widget.layout.w + delta.w)),
          h: Math.max(2, widget.layout.h + delta.h)
        });

        announceToScreenReader(
          `Widget resized to ${widget.layout.w + delta.w} columns by ${widget.layout.h + delta.h} rows`
        );
      }

      // Delete → Remove widget
      if (e.key === 'Delete') {
        if (confirm('Remove this widget?')) {
          removeWidget(focusedWidgetId);
          announceToScreenReader('Widget removed');
        }
      }

      // Ctrl + Z → Undo
      if (e.ctrlKey && e.key === 'z') {
        e.preventDefault();
        undo();
        announceToScreenReader('Undo');
      }

      // Ctrl + Y → Redo
      if (e.ctrlKey && e.key === 'y') {
        e.preventDefault();
        redo();
        announceToScreenReader('Redo');
      }

      // Tab → Cycle widgets
      if (e.key === 'Tab') {
        e.preventDefault();
        const currentIndex = widgets.findIndex(w => w.id === focusedWidgetId);
        const nextIndex = (currentIndex + 1) % widgets.length;
        setFocusedWidgetId(widgets[nextIndex].id);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [focusedWidgetId, widgets]);

  return { focusedWidgetId, setFocusedWidgetId };
};

function announceToScreenReader(message: string) {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', 'polite');
  announcement.className = 'sr-only';
  announcement.textContent = message;
  document.body.appendChild(announcement);
  setTimeout(() => announcement.remove(), 1000);
}
```

**Deliverable:** Full keyboard navigation support

---

#### **T5: Accessibility (WCAG 2.1 AA)** (10h)

**Implementation:**

```typescript
// Add ARIA attributes to GridLayout
<div
  role="grid"
  aria-label="Dashboard widgets grid"
  aria-rowcount={Math.max(...layout.map(item => item.y + item.h))}
  aria-colcount={12}
>
  {widgets.map((widget, index) => (
    <div
      key={widget.id}
      role="gridcell"
      aria-label={`${widget.title || widget.type} widget, position ${index + 1} of ${widgets.length}`}
      aria-rowindex={widget.layout.y + 1}
      aria-colindex={widget.layout.x + 1}
      aria-rowspan={widget.layout.h}
      aria-colspan={widget.layout.w}
      tabIndex={0}
      onFocus={() => setFocusedWidgetId(widget.id)}
      sx={{
        outline: focusedWidgetId === widget.id ? '3px solid #1976d2' : 'none',
        outlineOffset: '2px'
      }}
    >
      <WidgetWrapper widget={widget} ... />
    </div>
  ))}
</div>
```

**CSS for focus indicators:**

```css
/* frontend/src/components/dashboard/GridLayout.css */
.widget-container:focus {
  outline: 3px solid #1976d2;
  outline-offset: 2px;
  z-index: 1000;
}

.widget-container:focus-visible {
  box-shadow: 0 0 0 4px rgba(25, 118, 210, 0.25);
}

/* Screen reader only class */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

**Deliverable:** WCAG 2.1 AA compliant grid

---

## 🧪 TESTING

### E2E Tests

```typescript
// e2e/specs/dashboard/grid-layout.spec.ts
import { test, expect } from '@playwright/test';

test('Drag and drop widget works', async ({ page }) => {
  await page.goto('/dashboard');

  // Get initial position
  const widget = page.locator('.widget-container').first();
  const initialBox = await widget.boundingBox();

  // Drag widget
  await widget.locator('.widget-header').dragTo(
    page.locator('.dashboard-grid'),
    { targetPosition: { x: 600, y: 200 } }
  );

  // Verify position changed
  const newBox = await widget.boundingBox();
  expect(newBox?.x).not.toBe(initialBox?.x);
});

test('Resize widget works', async ({ page }) => {
  await page.goto('/dashboard');

  const widget = page.locator('.widget-container').first();
  const initialSize = await widget.boundingBox();

  // Drag resize handle
  await widget.locator('.react-resizable-handle-se').dragTo(
    page.locator('.dashboard-grid'),
    { targetPosition: { x: 800, y: 400 } }
  );

  const newSize = await widget.boundingBox();
  expect(newSize?.width).toBeGreaterThan(initialSize?.width || 0);
});

test('Keyboard navigation works', async ({ page }) => {
  await page.goto('/dashboard');

  // Focus widget
  await page.keyboard.press('Tab');

  // Move with arrows
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowDown');

  // Verify screen reader announcement
  const announcement = page.locator('[role="status"]');
  await expect(announcement).toHaveText(/Widget moved to column/);
});
```

---

## 📊 SUCCESS METRICS

- ✅ Grid setup < 5min (first-time user)
- ✅ Drag latency < 16ms (60fps smooth)
- ✅ Resize smooth (no janky animation)
- ✅ Layout save < 200ms
- ✅ Responsive breakpoint switch < 100ms
- ✅ Keyboard navigation 100% functional
- ✅ WCAG 2.1 AA compliant (axe-core violations = 0)

---

## 🔗 DEPENDENCIES

- **EPIC-014 S7:** Loading states (skeleton pro grid) ⏳
- **Libraries:** react-grid-layout, zustand

---

## 📚 DOCUMENTATION

- [ ] User Guide: Dashboard Grid Layout
- [ ] Developer Guide: Custom widgets in grid
- [ ] Accessibility Guide: Keyboard shortcuts cheat sheet

---

**Status:** 📋 TODO → Ready for implementation  
**Next:** S4: Role-Based Default Layouts

