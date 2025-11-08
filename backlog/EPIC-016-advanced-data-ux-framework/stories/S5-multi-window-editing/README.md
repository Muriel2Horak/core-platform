# S5: Multi-Window Editing System

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~60 hours  
**Sprint:** 5-6  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Admin / Support / Power User,  
**chci** mít možnost otevřít detail několika záznamů současně v samostatných popup oknech,  
**abych** mohl:
- Editovat 5 user profileů paralelně v 5 různých oknech
- Přesouvat okna mezi více monitory (multi-monitor workflow)
- Porovnat data side-by-side (2 workflows, 3 tenant configs)
- Neztrácet kontext při přepínání mezi záznamy

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Multi-Instance Popup Manager

**GIVEN** seznam uživatelů v DataView  
**WHEN** kliknu na 3 různé řádky (John, Alice, Bob)  
**THEN** otevřou se 3 samostatné popup okna:
- John User Detail (popup #1)
- Alice User Detail (popup #2)
- Bob User Detail (popup #3)

**AND** každý popup má:
- Vlastní URL state (`/users/123?popup=1`)
- Nezávislou editaci (změny v popup #1 neovlivní popup #2)
- Close button (X) → zavře pouze tento popup
- Drag handle → přesun okna

### AC2: Window Positioning & Resizing

**GIVEN** otevřené popup okno  
**WHEN** drag-and-drop okna nebo resize handles  
**THEN** okno:
- Lze přesouvat myší (drag header)
- Lze měnit velikost (8 resize handles: N, S, E, W, NE, NW, SE, SW)
- Ukládá pozici do localStorage (`popup_positions`)
- Obnoví pozici při dalším otevření stejného záznamu

**AND** multi-monitor support:
- Okno lze přesunout mimo hlavní obrazovku
- Pozice relativní k viewport (ne absolutní px)

### AC3: Popup State Persistence

**GIVEN** uživatel má otevřené 3 popup okna  
**WHEN** refresh stránky (F5)  
**THEN** všechny popup okna se OBNOVÍ:
- Stejné záznamy (User #123, #456, #789)
- Stejné pozice a velikosti
- Stejný editační stav (pokud byly nesaved změny → warning)

### AC4: Keyboard Navigation

**GIVEN** 5 otevřených popup oken  
**WHEN** použiju klávesnici  
**THEN** mohu:
- `Ctrl + Tab` → přepnout na další popup (cycle)
- `Ctrl + Shift + Tab` → předchozí popup
- `Ctrl + W` → zavřít aktivní popup
- `Esc` → zavřít všechny popupy
- `Alt + 1-9` → přepnout na popup #N

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Window Manager Service** (12h)

**Implementation:**

```typescript
// frontend/src/services/WindowManager.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface PopupWindow {
  id: string;                    // Unique ID (uuid)
  entity: string;                // 'Users' | 'Tenants' | 'Workflows'
  recordId: number;              // Record ID (123, 456)
  title: string;                 // "User: John Doe"
  position: { x: number; y: number };
  size: { width: number; height: number };
  zIndex: number;                // Stacking order
  isActive: boolean;             // Currently focused
  isDirty: boolean;              // Has unsaved changes
}

interface WindowManagerState {
  windows: PopupWindow[];
  activeWindowId: string | null;
  maxZIndex: number;

  // Actions
  openWindow: (entity: string, recordId: number, title: string) => void;
  closeWindow: (id: string) => void;
  closeAllWindows: () => void;
  focusWindow: (id: string) => void;
  updatePosition: (id: string, x: number, y: number) => void;
  updateSize: (id: string, width: number, height: number) => void;
  markDirty: (id: string, dirty: boolean) => void;
}

export const useWindowManager = create<WindowManagerState>()(
  persist(
    (set, get) => ({
      windows: [],
      activeWindowId: null,
      maxZIndex: 1000,

      openWindow: (entity, recordId, title) => {
        const existing = get().windows.find(
          w => w.entity === entity && w.recordId === recordId
        );

        if (existing) {
          // Focus existing window
          set({ activeWindowId: existing.id });
          return;
        }

        const newWindow: PopupWindow = {
          id: crypto.randomUUID(),
          entity,
          recordId,
          title,
          position: calculateCascadePosition(get().windows.length),
          size: { width: 600, height: 800 },
          zIndex: get().maxZIndex + 1,
          isActive: true,
          isDirty: false
        };

        set(state => ({
          windows: [...state.windows, newWindow],
          activeWindowId: newWindow.id,
          maxZIndex: state.maxZIndex + 1
        }));
      },

      closeWindow: (id) => {
        const window = get().windows.find(w => w.id === id);
        if (window?.isDirty) {
          if (!confirm('You have unsaved changes. Close anyway?')) {
            return;
          }
        }

        set(state => ({
          windows: state.windows.filter(w => w.id !== id),
          activeWindowId: state.activeWindowId === id
            ? state.windows[0]?.id ?? null
            : state.activeWindowId
        }));
      },

      closeAllWindows: () => {
        const dirtyWindows = get().windows.filter(w => w.isDirty);
        if (dirtyWindows.length > 0) {
          if (!confirm(`${dirtyWindows.length} windows have unsaved changes. Close all?`)) {
            return;
          }
        }

        set({ windows: [], activeWindowId: null });
      },

      focusWindow: (id) => {
        set(state => ({
          windows: state.windows.map(w =>
            w.id === id
              ? { ...w, zIndex: state.maxZIndex + 1, isActive: true }
              : { ...w, isActive: false }
          ),
          activeWindowId: id,
          maxZIndex: state.maxZIndex + 1
        }));
      },

      updatePosition: (id, x, y) => {
        set(state => ({
          windows: state.windows.map(w =>
            w.id === id ? { ...w, position: { x, y } } : w
          )
        }));
      },

      updateSize: (id, width, height) => {
        set(state => ({
          windows: state.windows.map(w =>
            w.id === id ? { ...w, size: { width, height } } : w
          )
        }));
      },

      markDirty: (id, dirty) => {
        set(state => ({
          windows: state.windows.map(w =>
            w.id === id ? { ...w, isDirty: dirty } : w
          )
        }));
      }
    }),
    {
      name: 'window-manager-storage',
      partialize: (state) => ({
        windows: state.windows.map(w => ({
          ...w,
          isDirty: false  // Don't persist dirty state
        }))
      })
    }
  )
);

function calculateCascadePosition(index: number): { x: number; y: number } {
  const offset = 30 * index;
  return {
    x: 100 + offset,
    y: 100 + offset
  };
}
```

---

#### **T2: Draggable Popup Component** (15h)

**Implementation:**

```typescript
// frontend/src/components/popup/DraggablePopup.tsx
import React, { useRef, useState } from 'react';
import { Rnd } from 'react-rnd';
import { Paper, AppBar, Toolbar, IconButton, Typography } from '@mui/material';
import { Close, Minimize, Fullscreen } from '@mui/icons-material';
import { useWindowManager } from '@/services/WindowManager';

interface DraggablePopupProps {
  id: string;
  title: string;
  children: React.ReactNode;
  onClose?: () => void;
}

export const DraggablePopup: React.FC<DraggablePopupProps> = ({
  id,
  title,
  children,
  onClose
}) => {
  const {
    windows,
    focusWindow,
    updatePosition,
    updateSize,
    closeWindow
  } = useWindowManager();

  const window = windows.find(w => w.id === id);
  if (!window) return null;

  const handleDragStop = (_e: any, data: { x: number; y: number }) => {
    updatePosition(id, data.x, data.y);
  };

  const handleResizeStop = (
    _e: any,
    _direction: any,
    ref: HTMLElement,
    _delta: any,
    position: { x: number; y: number }
  ) => {
    updateSize(id, ref.offsetWidth, ref.offsetHeight);
    updatePosition(id, position.x, position.y);
  };

  return (
    <Rnd
      size={{ width: window.size.width, height: window.size.height }}
      position={{ x: window.position.x, y: window.position.y }}
      onDragStop={handleDragStop}
      onResizeStop={handleResizeStop}
      minWidth={400}
      minHeight={300}
      bounds="window"
      dragHandleClassName="drag-handle"
      style={{ zIndex: window.zIndex }}
      onClick={() => focusWindow(id)}
    >
      <Paper
        elevation={window.isActive ? 24 : 8}
        sx={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          border: window.isActive ? '2px solid #1976d2' : 'none'
        }}
      >
        <AppBar
          position="static"
          className="drag-handle"
          sx={{ cursor: 'move' }}
        >
          <Toolbar variant="dense">
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              {title}
              {window.isDirty && ' *'}
            </Typography>
            <IconButton color="inherit" size="small">
              <Minimize />
            </IconButton>
            <IconButton color="inherit" size="small">
              <Fullscreen />
            </IconButton>
            <IconButton
              color="inherit"
              size="small"
              onClick={() => closeWindow(id)}
            >
              <Close />
            </IconButton>
          </Toolbar>
        </AppBar>

        <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
          {children}
        </div>
      </Paper>
    </Rnd>
  );
};
```

---

#### **T3: Popup Manager Overlay** (8h)

**Implementation:**

```typescript
// frontend/src/components/popup/PopupManager.tsx
import React, { useEffect } from 'react';
import { useWindowManager } from '@/services/WindowManager';
import { DraggablePopup } from './DraggablePopup';
import { UserDetailPopup } from '@/features/users/UserDetailPopup';
import { TenantDetailPopup } from '@/features/tenants/TenantDetailPopup';
import { WorkflowDetailPopup } from '@/features/workflows/WorkflowDetailPopup';

export const PopupManager: React.FC = () => {
  const { windows, closeWindow, closeAllWindows, focusWindow } = useWindowManager();

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'Tab') {
        e.preventDefault();
        cycleWindows(e.shiftKey);
      }
      if (e.ctrlKey && e.key === 'w') {
        e.preventDefault();
        if (windows.length > 0) {
          closeWindow(windows.find(w => w.isActive)?.id ?? windows[0].id);
        }
      }
      if (e.key === 'Escape') {
        e.preventDefault();
        closeAllWindows();
      }
      if (e.altKey && /^[1-9]$/.test(e.key)) {
        e.preventDefault();
        const index = parseInt(e.key) - 1;
        if (windows[index]) {
          focusWindow(windows[index].id);
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [windows]);

  const cycleWindows = (reverse: boolean) => {
    const activeIndex = windows.findIndex(w => w.isActive);
    const nextIndex = reverse
      ? (activeIndex - 1 + windows.length) % windows.length
      : (activeIndex + 1) % windows.length;
    focusWindow(windows[nextIndex].id);
  };

  return (
    <>
      {windows.map(window => (
        <DraggablePopup
          key={window.id}
          id={window.id}
          title={window.title}
        >
          {renderPopupContent(window)}
        </DraggablePopup>
      ))}
    </>
  );
};

function renderPopupContent(window: PopupWindow) {
  switch (window.entity) {
    case 'Users':
      return <UserDetailPopup userId={window.recordId} />;
    case 'Tenants':
      return <TenantDetailPopup tenantId={window.recordId} />;
    case 'Workflows':
      return <WorkflowDetailPopup workflowId={window.recordId} />;
    default:
      return <div>Unknown entity: {window.entity}</div>;
  }
}
```

---

#### **T4: URL State Synchronization** (10h)

**Cíl:** Persistence popup stavu v URL (F5 restore)

**Implementation:**

```typescript
// frontend/src/hooks/usePopupUrlSync.ts
import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useWindowManager } from '@/services/WindowManager';

export const usePopupUrlSync = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const { windows, openWindow } = useWindowManager();

  // Sync URL → State (on mount / F5)
  useEffect(() => {
    const popupParams = searchParams.get('popups');
    if (!popupParams) return;

    // Format: "Users:123,Tenants:456,Workflows:789"
    const popupSpecs = popupParams.split(',');
    popupSpecs.forEach(spec => {
      const [entity, recordId] = spec.split(':');
      if (entity && recordId) {
        openWindow(entity, parseInt(recordId), `${entity} #${recordId}`);
      }
    });
  }, []);

  // Sync State → URL (when windows change)
  useEffect(() => {
    if (windows.length === 0) {
      searchParams.delete('popups');
    } else {
      const popupString = windows
        .map(w => `${w.entity}:${w.recordId}`)
        .join(',');
      searchParams.set('popups', popupString);
    }

    setSearchParams(searchParams, { replace: true });
  }, [windows]);
};
```

**Usage:**

```typescript
// frontend/src/App.tsx
import { PopupManager } from '@/components/popup/PopupManager';
import { usePopupUrlSync } from '@/hooks/usePopupUrlSync';

export const App = () => {
  usePopupUrlSync();  // Enable URL sync

  return (
    <>
      <MainLayout />
      <PopupManager />  {/* Render all popups */}
    </>
  );
};
```

---

#### **T5: Multi-Monitor Support** (5h)

**Cíl:** Detekce více monitorů, pozice relativní k viewport

**Implementation:**

```typescript
// frontend/src/utils/multiMonitor.ts
export function getAvailableScreens(): ScreenDetails[] {
  if ('getScreenDetails' in window) {
    // Multi-Screen Window Placement API (experimental)
    return (window as any).getScreenDetails().screens;
  }

  // Fallback: Single screen
  return [
    {
      availWidth: window.screen.availWidth,
      availHeight: window.screen.availHeight,
      left: window.screenLeft,
      top: window.screenTop
    }
  ];
}

export function normalizePosition(
  x: number,
  y: number
): { x: number; y: number } {
  // Ensure position is within viewport bounds
  const maxX = window.innerWidth - 400;  // Min popup width
  const maxY = window.innerHeight - 300; // Min popup height

  return {
    x: Math.max(0, Math.min(x, maxX)),
    y: Math.max(0, Math.min(y, maxY))
  };
}
```

---

## 🧪 TESTING

### E2E Tests

```typescript
// e2e/specs/popup/multi-window.spec.ts
import { test, expect } from '@playwright/test';

test('User can open multiple edit popups simultaneously', async ({ page }) => {
  await page.goto('/users');

  // Open 3 user detail popups
  await page.click('tr:has-text("John Doe")');
  await page.click('tr:has-text("Alice Smith")');
  await page.click('tr:has-text("Bob Johnson")');

  // Verify 3 popups are visible
  await expect(page.locator('.draggable-popup')).toHaveCount(3);
  await expect(page.locator('text=John Doe')).toBeVisible();
  await expect(page.locator('text=Alice Smith')).toBeVisible();

  // Edit in popup #1
  await page.locator('.draggable-popup:has-text("John Doe") input[name="email"]')
    .fill('newemail@example.com');

  // Verify popup #2 unchanged
  await expect(page.locator('.draggable-popup:has-text("Alice Smith") input[name="email"]'))
    .not.toHaveValue('newemail@example.com');
});

test('Popups restore after page refresh', async ({ page }) => {
  await page.goto('/users');

  // Open 2 popups
  await page.click('tr:has-text("John Doe")');
  await page.click('tr:has-text("Alice Smith")');

  // Get URL with popup state
  const url = page.url();
  expect(url).toContain('popups=Users:123,Users:456');

  // Refresh page
  await page.reload();

  // Verify popups restored
  await expect(page.locator('.draggable-popup')).toHaveCount(2);
});

test('Keyboard shortcuts work', async ({ page, context }) => {
  await page.goto('/users');

  // Open 3 popups
  await page.click('tr:has-text("John")');
  await page.click('tr:has-text("Alice")');
  await page.click('tr:has-text("Bob")');

  // Ctrl + Tab → cycle windows
  await page.keyboard.press('Control+Tab');
  // TODO: Verify focus changed

  // Ctrl + W → close active window
  await page.keyboard.press('Control+W');
  await expect(page.locator('.draggable-popup')).toHaveCount(2);

  // Esc → close all
  await page.keyboard.press('Escape');
  await expect(page.locator('.draggable-popup')).toHaveCount(0);
});
```

---

## 📊 SUCCESS METRICS

- ✅ Mohu otevřít 10+ popup oken současně bez lag
- ✅ Drag & drop < 16ms latency (60fps)
- ✅ Resize smooth (no janky)
- ✅ Popup state přežije F5 refresh
- ✅ Multi-monitor support funguje (Windows Placement API)

---

## 🔗 DEPENDENCIES

- **EPIC-014 S3:** Form component (for edit forms in popups)
- **Libraries:** react-rnd, zustand

---

## 📚 DOCUMENTATION

- [ ] User Guide: Multi-Window Editing Workflow
- [ ] Keyboard Shortcuts Cheat Sheet

---

**Status:** 📋 TODO  
**Next:** S6: Customizable Popup Layouts
