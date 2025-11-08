# S3: Dashboard Grid Layout

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🔴 **P0** | **Effort:** ~70h | **Sprint:** 3-4

---

## 📋 USER STORY

**Jako** Dashboard Designer, **chci** 12-column drag-and-drop grid pro umístění widgets (KPI tiles, charts, tables), **abych** mohl vytvořit custom dashboardy.

---

## 🎯 ACCEPTANCE CRITERIA

1. **12-Column Grid**: Responsive grid system (desktop 12 cols, tablet 6, mobile 1)
2. **Drag & Drop**: Přesun widgets mezi pozicemi
3. **Resize Widgets**: 8 resize handles (N, S, E, W, NE, NW, SE, SW)
4. **Snap to Grid**: Auto-align widgets na grid columns
5. **Responsive Breakpoints**: Auto-reorganize na tablet/mobile
6. **Layout Persistence**: Uložení layoutu do localStorage + backend

---

## 🏗️ TASK BREAKDOWN (~70h)

### T1: GridLayout Component (12h)
- react-grid-layout integration
- 12-column configuration
- Drag & drop event handlers

### T2: Responsive Breakpoints (8h)
- Breakpoints: desktop (>1200px), tablet (768-1200), mobile (<768)
- Auto-reflow widgets

### T3: Widget Resize (10h)
- 8 resize handles implementation
- Min/max size constraints
- Aspect ratio lock option

### T4: Layout State Management (15h)
- Zustand store for layout
- Save layout → backend API
- Load layout on mount

### T5: Keyboard Navigation (10h)
- Arrow keys → move widget
- Tab → cycle widgets
- Delete → remove widget

### T6: Accessibility (10h)
- WCAG 2.1 AA compliance
- Screen reader announcements ("Widget moved to column 3, row 2")
- Focus indicators

### T7: Testing (5h)

---

## 📦 DEPENDENCIES

- **EPIC-014 S7**: Loading states (skeleton for grid)
- **Libraries**: react-grid-layout, zustand

---

## 📊 SUCCESS METRICS

- Grid setup < 5min
- Drag latency < 16ms (60fps)
- Resize smooth (no janky)

