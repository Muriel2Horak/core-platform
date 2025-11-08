# EPIC-016: Advanced Data UX Framework

**Status:** 📋 **PLANNED**  
**Priority:** 🔴 **P0 - CRITICAL**  
**Owner:** Product Team + UX Team  
**Created:** 8. listopadu 2025  
**Target:** Q1-Q2 2026  
**Effort:** ~740 hours (~18-19 sprints)

---

## 📋 EPIC OVERVIEW

### Vision

> Comprehensive framework for viewing, filtering, analyzing, and editing data across all entities with customizable layouts, multi-window support, and Cube.js-powered analytics.

**What this EPIC delivers:**
- 📊 **Universal Data Views** - Tabulky, grafy, pivot tables pro všechny entity (Users, Tenants, Workflows, Custom)
- 🪟 **Multi-Window Editing** - Popup okna s drag & drop layoutem, multi-monitor support
- 🎨 **Customization Framework** - Uživatelé si mohou upravit layouts a sdílet s týmem
- 🔍 **Advanced Analytics** - Cube.js integration, filtrování, kontingenční tabulky, export
- 📱 **Responsive Design** - Funguje na desktop, tablet, mobile

### Business Value

**Problem:**
- ❌ Každá entita (Users, Workflows, Tenants) má vlastní hardcoded view
- ❌ Žádná možnost customizace (uživatel nemůže přidat sloupce, grafy)
- ❌ Editace pouze v inline tabulce (bez multi-window support)
- ❌ Reporting oddělený od entity views (2 různé UX)

**Solution:**
- ✅ **Reusable framework** - Jeden `<DataView>` component pro všechny entity
- ✅ **Self-service customization** - Business users si mohou upravit view bez dev pomoci
- ✅ **Unified UX** - Reporting + CRUD v jednom frameworku
- ✅ **Power user features** - Multi-window editing, pivot tables, advanced filters

**Impact:**
- 📈 **User productivity:** +200% (multi-window editing, saved filters)
- 🎯 **Self-service:** 80% customization bez IT tickets
- ⏱️ **Time to insight:** 5 min → 30 sec (saved filters, dashboards)
- 💰 **ROI:** -70% development time (reusable components)

---

## 🎯 SCOPE

### ✅ IN SCOPE

1. **Universal Data Views**
   - Generic `<DataView>` component (works for any entity)
   - View mode switcher: Table ↔ Chart ↔ Pivot ↔ Heatmap ↔ Cards
   - Cube.js integration (dynamic schema detection)

2. **Advanced Filtering & Search**
   - Multi-select filters (Status, Role, Tenant)
   - Date range picker, tag input, search
   - Advanced filter builder (AND/OR conditions)
   - Saved filters (personal favorites)

3. **Dashboard Grid Layout**
   - 12-column drag & drop grid (react-grid-layout)
   - Widget library (KPI tiles, charts, tables)
   - Role-based default dashboards (Admin, Tenant Admin, Analyst, Viewer)
   - Save/share dashboards

4. **Visual Query Builder**
   - Cube.js schema introspection API
   - No-code query builder (measures, dimensions, filters)
   - Live preview renderer

5. **Multi-Window Detail Popups** 🆕
   - Draggable, resizable popup windows
   - Multi-instance support (otevřít 5+ popupů současně)
   - Multi-monitor support (drag na jinou obrazovku)
   - Z-index management (active window foreground)
   - State persistence (obnovit popupy při reload)

6. **Customizable Popup Layouts** 🆕
   - Drag & drop layout builder pro detail popup
   - Add sections: Fields, Charts, Tables
   - Resize sections (6-col, 12-col grid)
   - Save layout (private, team, public scope)

7. **Export & Pagination**
   - Export table to XLS, CSV, PDF
   - Server-side pagination (large datasets 10k+ rows)
   - Infinite scroll (optional)

8. **Kontingenční Tabulka (Pivot)**
   - Cross-tabulation (rows × columns)
   - Cube.js pivot queries
   - Interactive drill-down

9. **Extended Widget Types**
   - Advanced charts: Heatmap, Sankey, Treemap, Gauge
   - Chart customization (colors, legends, axes)

10. **Sharing & Collaboration**
    - Share dashboards/views with team
    - Public dashboards (anonymní přístup)
    - Permissions (view-only, edit)

11. **EPIC-014 Integration**
    - Replace MUI components with Design System
    - Forms (S3), Tables (S9), Loading (S7), Errors (S8)

### ❌ OUT OF SCOPE

- ❌ Backend Cube.js schemas → **EPIC-004** (already done)
- ❌ General UI components (buttons, inputs) → **EPIC-014 Design System**
- ❌ Navigation, sidebar, layout → Core app structure
- ❌ Workflow engine integration → **EPIC-005** (separate)
- ❌ Real-time collaboration (Google Docs-like) → Future epic
- ❌ AI-powered insights → Future epic (EPIC-009/010)

---

## 📊 PROGRESS OVERVIEW

**Overall Completion:** 🔴 **0%** (11 stories, 0 done)

### Story Status

| ID | Story | Status | Effort | Priority | Sprint | Dependencies |
|----|-------|--------|--------|----------|--------|--------------|
| **S1** | [Universal Data View Engine](stories/S1-data-view-engine.md) | 📋 TODO | 80h | P0 | 1-2 | EPIC-014 S9 ⏳ |
| **S2** | [Advanced Filtering & Search](stories/S2-filtering-search.md) | 📋 TODO | 60h | P0 | 2-3 | EPIC-014 S3 ⏳ |
| **S3** | [Dashboard Grid Layout](stories/S3-dashboard-grid.md) | 📋 TODO | 70h | P0 | 3-4 | None |
| **S4** | [Visual Query Builder](stories/S4-query-builder.md) | 📋 TODO | 45h | P1 | 5 | S3, EPIC-014 S3 ⏳ |
| **S5** | [Multi-Window Detail Popups](stories/S5-multi-window-popups.md) | 📋 TODO | 100h | P0 | 6-8 | None |
| **S6** | [Customizable Popup Layouts](stories/S6-customizable-popups.md) | 📋 TODO | 70h | P1 | 9-10 | S5 |
| **S7** | [Export & Pagination](stories/S7-export-pagination.md) | 📋 TODO | 40h | P1 | 11 | S1 |
| **S8** | [Kontingenční Tabulka (Pivot)](stories/S8-pivot-table.md) | 📋 TODO | 50h | P2 | 12 | S1, S4 |
| **S9** | [Extended Widget Types](stories/S9-extended-widgets.md) | 📋 TODO | 150h | P2 | 13-16 | S4 |
| **S10** | [Sharing & Collaboration](stories/S10-sharing.md) | 📋 TODO | 30h | P3 | 17 | S3 |
| **S11** | [EPIC-014 Integration](stories/S11-epic014-integration.md) | 📋 TODO | 45h | P0 | 18 | ⏳ EPIC-014 S3, S9 |

**Total Effort:** ~740 hours (~18-19 sprints @ 40h/sprint)

### Completion by Priority

| Priority | Stories | Completed | Total Effort | % Done |
|----------|---------|-----------|--------------|--------|
| **P0** | 4 | 0/4 | 295h | 0% |
| **P1** | 3 | 0/3 | 215h | 0% |
| **P2** | 2 | 0/2 | 200h | 0% |
| **P3** | 2 | 0/2 | 30h | 0% |

---

## 🗺️ ROADMAP

### **Phase 1: Universal Data Views** (Sprint 1-3, ~210h)
🎯 **Goal:** Generic view system pro všechny entity s filtrováním

**Stories:**
- ✅ S1: Universal Data View Engine (80h)
- ✅ S2: Advanced Filtering & Search (60h)
- ✅ S3: Dashboard Grid Layout (70h)

**Deliverable:** 
- Users/Workflows/Tenants views s tabulkou, grafy, KPI tiles
- Multi-select filters, date ranges, saved filters
- Dashboard s drag & drop widgets

**Success Criteria:**
- User může otevřít "Users" view → zobrazí tabulku
- User může přepnout na Chart view → zobrazí bar chart
- User může přidat filter "Status = Active" → table se filtruje
- User může vytvořit dashboard s 3 widgets (KPI + Chart + Table)

---

### **Phase 2: Visual Query Builder** (Sprint 5, ~45h)
🎯 **Goal:** No-code query builder pro business users

**Stories:**
- ✅ S4: Visual Query Builder (45h)

**Deliverable:**
- Drag & drop query builder (entity, measures, dimensions, filters)
- Live preview renderer
- Integration s dashboardy (widgets use queries)

**Success Criteria:**
- Business analyst může vytvořit query "Count of Workflows by Status" bez SQL
- Preview shows correct chart data
- Query se uloží a použije v dashboard widget

**⚠️ BLOCKER:** Vyžaduje EPIC-014 S3 (form components) pro dropdown UI

---

### **Phase 3: Multi-Window Editing** (Sprint 6-10, ~170h)
🎯 **Goal:** Popup windows s customizací pro power users

**Stories:**
- ✅ S5: Multi-Window Detail Popups (100h)
- ✅ S6: Customizable Popup Layouts (70h)

**Deliverable:**
- Draggable, resizable popup windows
- Multi-instance support (5+ popupů současně)
- Multi-monitor support (drag na jinou obrazovku)
- Layout builder (přidat grafy, tabulky do popupu)
- Save/share layouts (private, team, public)

**Success Criteria:**
- Support agent otevře 3 user detail popupy vedle sebe
- Tenant admin upraví User Detail popup → přidá graf "Login Activity"
- Layout se uloží a sdílí s týmem
- Po refresh page se popupy obnoví

**Use Case:**
```
Screen 1 (main)              Screen 2 (secondary monitor)
┌─────────────────────┐      ┌─────────────────────────────┐
│ Users Table         │      │ User Detail: Jane Smith     │
│ [List of 100 users] │      │ [Edit form + Activity chart]│
└─────────────────────┘      └─────────────────────────────┘
  │
  ├─ Popup: John Doe (edit form)
  └─ Popup: Alice Brown (edit form)
```

---

### **Phase 4: Advanced Analytics** (Sprint 11-16, ~240h)
🎯 **Goal:** Kontingenční tabulky, extended charts, export

**Stories:**
- ✅ S7: Export & Pagination (40h)
- ✅ S8: Kontingenční Tabulka (50h)
- ✅ S9: Extended Widget Types (150h)

**Deliverable:**
- Export table to XLS, CSV, PDF
- Pivot table (cross-tabulation)
- Advanced charts: Heatmap, Sankey, Treemap, Gauge

**Success Criteria:**
- Analyst exportuje filtered user list do Excel
- Analyst vytvoří pivot "Workflows by Status × Tenant"
- Data analyst vytvoří heatmap "Activity by Day of Week × Hour"

---

### **Phase 5: Collaboration & Integration** (Sprint 17-18, ~75h)
🎯 **Goal:** Sdílení dashboardů, EPIC-014 integration

**Stories:**
- ✅ S10: Sharing & Collaboration (30h)
- ✅ S11: EPIC-014 Integration (45h)

**Deliverable:**
- Share dashboard with team
- Public dashboards (anonymní přístup)
- Replace MUI components s Design System

**Success Criteria:**
- Team lead sdílí dashboard s týmem
- Dashboard má consistent UI (EPIC-014 komponenty)

**⚠️ BLOCKER:** Vyžaduje EPIC-014 S3 (Forms) + S9 (Tables) delivery

---

## 🔗 DEPENDENCIES

### ⏳ **Blocking Dependencies** (Must deliver before this EPIC)

| Dependency | Status | Impact | Workaround |
|------------|--------|--------|------------|
| **EPIC-014 S3: Forms** | ⏳ TODO | 🔴 CRITICAL - S2, S4, S11 blokováno | ⚠️ Use MUI temporarily (tech debt) |
| **EPIC-014 S9: Tables** | ⏳ TODO | 🔴 CRITICAL - S1, S7, S11 blokováno | ⚠️ Use MUI DataGrid (tech debt) |
| **EPIC-014 S7: Loading** | ✅ Can use | 🟢 OK | ✅ Already available |
| **EPIC-014 S8: Errors** | ✅ Can use | 🟢 OK | ✅ Already available |

**Mitigation Strategy:**
- **Option 1:** Start with S3, S5, S6 (no EPIC-014 dependency)
- **Option 2:** Use MUI temporarily → replace in S11 (tech debt)
- **Option 3:** Wait for EPIC-014 S3, S9 delivery (delays EPIC-016)

**Recommendation:** 👉 **Option 2** - Start now with MUI, refactor later

---

### ✅ **Provided Dependencies** (This EPIC depends ON)

| Dependency | Status | What we use |
|------------|--------|-------------|
| **EPIC-004 S1: Cube.js** | ✅ DONE | Data schemas, queries, pre-aggregations |
| **EPIC-005: Workflow Engine** | ✅ DONE | Workflow entity for views |

---

### 🔄 **Synergy Dependencies** (Nice to have)

| Dependency | Status | Synergy |
|------------|--------|---------|
| **EPIC-009: AI Integration** | 📋 PLANNED | AI-powered query suggestions |
| **EPIC-010: ML Platform** | 📋 PLANNED | Predictive analytics widgets |

---

## 🏗️ ARCHITECTURE OVERVIEW

### Component Hierarchy

```
<DataUXFramework>
  ├── <DataView entity="Users">
  │   ├── <ViewModeSwitcher> (Table/Chart/Pivot/Heatmap)
  │   ├── <FilterBar>
  │   │   ├── <MultiSelectFilter field="status">
  │   │   ├── <DateRangeFilter field="lastLogin">
  │   │   └── <SearchInput>
  │   ├── <TableView> (EPIC-014 S9)
  │   ├── <ChartView> (Recharts)
  │   └── <PivotView> (react-pivottable)
  │
  ├── <Dashboard>
  │   ├── <GridLayout> (react-grid-layout)
  │   ├── <WidgetLibrary>
  │   │   ├── <KPITile>
  │   │   ├── <ChartWidget>
  │   │   └── <TableWidget>
  │   └── <QueryBuilder> (visual, no-code)
  │
  ├── <DetailPopup entity="User" id={123}>
  │   ├── <PopupWindow> (draggable, resizable)
  │   ├── <PopupLayoutGrid> (12-column)
  │   │   ├── <FieldsSection> (EPIC-014 S3)
  │   │   ├── <ChartSection>
  │   │   └── <TableSection>
  │   └── <LayoutCustomizer> (edit mode)
  │
  └── <ExportDialog>
      ├── <FormatSelector> (XLS/CSV/PDF)
      └── <ColumnSelector>
```

### State Management

**Technology:** Zustand (lightweight, TypeScript-first)

```typescript
// Global store
interface DataUXStore {
  // Data Views
  views: Map<string, ViewState>;  // entity → view config
  activeView: string | null;
  
  // Dashboards
  dashboards: Dashboard[];
  activeDashboard: string | null;
  
  // Popups
  popups: Map<string, PopupState>;  // popupId → state
  popupZIndex: number;  // for focus management
  
  // Filters
  savedFilters: SavedFilter[];
  activeFilters: Map<string, Filter[]>;  // entity → filters
  
  // Layouts
  customLayouts: Map<string, Layout>;  // viewId → layout
  
  // Actions
  openPopup: (entity: string, id: number) => void;
  closePopup: (popupId: string) => void;
  saveLayout: (viewId: string, layout: Layout, scope: 'private'|'team'|'public') => void;
  applyFilter: (entity: string, filter: Filter) => void;
}
```

### Data Flow

```
1. USER ACTION
   User clicks "Open Users view"
   ↓
2. FETCH VIEW CONFIG
   GET /api/views/users → returns {columns, filters, defaultLayout}
   ↓
3. FETCH DATA
   POST /api/cube/query → Cube.js query → PostgreSQL
   ↓
4. RENDER VIEW
   <DataView> → <TableView> or <ChartView>
   ↓
5. USER CUSTOMIZATION
   User adds filter "Status = Active"
   → Store in Zustand
   → Re-fetch data with new filter
   → Render updated view
   ↓
6. SAVE LAYOUT (optional)
   User clicks "Save Layout"
   → POST /api/layouts/users-view {columns, filters, scope: 'private'}
   → Store in DB (user_layouts table)
```

---

## 🎨 UX DESIGN PRINCIPLES

### 1. **Progressive Disclosure**
- Default view: Simple table s basic filters
- Advanced view: Pivot, heatmap, query builder (toggle "Advanced mode")
- Power user: Multi-window, custom layouts (auto-enable for frequent users)

### 2. **Familiar Patterns**
- Dashboard grid: JIRA-like drag & drop
- Query builder: Power BI-style dropdowns
- Popup windows: Standard OS window behavior (minimize, maximize, close)

### 3. **Responsive Design**
- Desktop (>1200px): Full features, multi-column layouts
- Tablet (768-1200px): 2-column layouts, simplified filters
- Mobile (<768px): 1-column, card view preferred over table

### 4. **Accessibility**
- Keyboard navigation: Tab, Arrow keys, Enter
- Screen reader support: ARIA labels, semantic HTML
- Color contrast: WCAG 2.1 AA compliance
- Focus indicators: Visible outlines

---

## 📚 DOCUMENTATION

### User Documentation
- [ ] User Guide: Data Views (how to use table, chart, pivot modes)
- [ ] User Guide: Dashboards (create, customize, share)
- [ ] User Guide: Filters (basic, advanced, saved)
- [ ] User Guide: Multi-Window Editing (open, arrange, customize popups)
- [ ] Video Tutorial: Creating Custom Dashboard (5 min)
- [ ] Video Tutorial: Multi-Window Workflow (3 min)

### Developer Documentation
- [ ] Architecture Doc: Component hierarchy, state management
- [ ] API Doc: /api/views, /api/dashboards, /api/layouts endpoints
- [ ] Integration Guide: How to add new entity to DataView system
- [ ] Migration Guide: MUI → EPIC-014 component replacement (S11)

### Design Documentation
- [ ] Wireframes: Data View modes (table, chart, pivot, heatmap)
- [ ] Wireframes: Dashboard grid layout
- [ ] Wireframes: Detail popup layouts (3 examples)
- [ ] Design Tokens: Colors, spacing, typography for data UX
- [ ] Interaction Patterns: Drag & drop, multi-window, filters

---

## 🧪 TESTING STRATEGY

### Unit Tests
- [ ] Component tests: DataView, FilterBar, Dashboard, Popup (Jest + RTL)
- [ ] Hook tests: useDataView, useDashboard, usePopup (Jest)
- [ ] Utility tests: Query builder, layout calculator

### Integration Tests
- [ ] API integration: /api/views, /api/dashboards, /api/layouts
- [ ] Cube.js integration: Query generation, schema introspection
- [ ] EPIC-014 integration: Form components, Table component

### E2E Tests (Playwright)
- [ ] User journey: Open Users view → filter → export to Excel
- [ ] User journey: Create dashboard → add widgets → save → share
- [ ] User journey: Open 3 popups → customize layout → save
- [ ] Multi-window: Drag popup to second monitor (requires Electron)

### Performance Tests
- [ ] Table rendering: 10,000 rows with virtual scrolling
- [ ] Dashboard load time: 12 widgets < 2 seconds
- [ ] Popup open/close: < 100ms
- [ ] Filter apply: < 500ms (server-side)

---

## 💰 BUSINESS VALUE & METRICS

### Key Metrics

| Metric | Before (Current) | After (EPIC-016) | Target |
|--------|------------------|------------------|--------|
| **View Customization** | 0% (hardcoded) | 80% | Self-service customization |
| **Time to Create Dashboard** | 2h (developer) | 10 min (user) | 12x faster |
| **Multi-Window Support** | ❌ No | ✅ Yes (5+ popups) | Power user feature |
| **Data Export** | Manual SQL queries | 1-click XLS/CSV/PDF | -90% effort |
| **User Adoption** | 20% (tech users only) | 80% (all users) | +300% |
| **IT Support Tickets** | 50/month (view customization) | 5/month | -90% |

### ROI Calculation

**Development Cost:**
- ~740 hours × $100/hour = **$74,000** (one-time)

**Annual Savings:**
- IT tickets: 45 tickets/month × $50/ticket × 12 months = **$27,000/year**
- User productivity: 100 users × 2h/week × $50/hour × 52 weeks = **$520,000/year**
- Developer time: 20 custom views/year × 10h/view × $100/hour = **$20,000/year**

**Total Annual Savings:** $567,000/year  
**ROI:** (567k - 74k) / 74k = **666% first year**

**Payback Period:** ~1.6 months

---

## 🚨 RISKS & MITIGATION

### 🔴 **HIGH RISK**

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **EPIC-014 delays block S11** | 🔴 CRITICAL | 60% | Start with MUI, refactor later (tech debt) |
| **Multi-monitor support browser limits** | 🟡 MEDIUM | 70% | Use Window.open() workaround or Electron app |
| **Performance issues (10k+ rows)** | 🔴 CRITICAL | 40% | Virtual scrolling, server-side pagination, Cube.js pre-aggregations |
| **Scope creep (too many features)** | 🟡 MEDIUM | 80% | Strict prioritization, Phase 1-5 roadmap |

### 🟡 **MEDIUM RISK**

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **User adoption low** | 🟡 MEDIUM | 30% | User research, training videos, onboarding tooltips |
| **Customization complexity** | 🟡 MEDIUM | 50% | Default templates, guided wizards, examples |
| **Cross-browser compatibility** | 🟢 LOW | 20% | Test on Chrome, Firefox, Safari, Edge |

---

## 📅 TIMELINE

**Start Date:** Q1 2026 (January)  
**Target Completion:** Q2 2026 (June)  
**Duration:** ~6 months (24 weeks, ~19 sprints)

### Milestones

| Milestone | Date | Deliverable |
|-----------|------|-------------|
| **M1: Phase 1 Complete** | Feb 2026 | Universal Data Views + Dashboards |
| **M2: Phase 2 Complete** | Mar 2026 | Visual Query Builder |
| **M3: Phase 3 Complete** | Apr 2026 | Multi-Window Editing |
| **M4: Phase 4 Complete** | May 2026 | Advanced Analytics (pivot, export) |
| **M5: Phase 5 Complete** | Jun 2026 | EPIC-014 Integration, Production Ready |

---

## 🎯 SUCCESS CRITERIA

### Phase 1 (MVP)
- ✅ User může otevřít "Users" view s tabulkou
- ✅ User může přepnout na Chart view
- ✅ User může filtrovat data (Status, Role, Date)
- ✅ User může vytvořit dashboard s 3 widgets
- ✅ Dashboard se uloží a zobrazí po refresh

### Phase 3 (Multi-Window)
- ✅ Support agent může otevřít 3 user detail popupy současně
- ✅ Tenant admin může přidat graf do popup layoutu
- ✅ Layout se uloží a sdílí s týmem
- ✅ Popupy se dají přesunout na jinou obrazovku

### Phase 5 (Production Ready)
- ✅ Všechny komponenty používají EPIC-014 Design System
- ✅ Export funguje (XLS, CSV, PDF)
- ✅ Pivot table funguje (kontingenční tabulka)
- ✅ Performance: Table s 10k rows < 2s load time
- ✅ User adoption: 80%+ users používají custom views

---

## 🔗 RELATED EPICS

| EPIC | Relationship | Status |
|------|--------------|--------|
| [EPIC-004: Reporting Infrastructure](../EPIC-004-reporting-analytics-infrastructure/README.md) | Provides Cube.js backend | ✅ S1 DONE |
| [EPIC-014: Design System](../EPIC-014-design-system/README.md) | Provides UI components | ⏳ S3, S9 BLOCKER |
| [EPIC-005: Workflow Engine](../EPIC-005-workflow-engine/README.md) | Provides workflow entity | ✅ DONE |
| [EPIC-009: AI Integration](../EPIC-009-ai-integration/README.md) | Future: AI query suggestions | 📋 PLANNED |
| [EPIC-010: ML Platform](../EPIC-010-ml-platform/README.md) | Future: Predictive widgets | 📋 PLANNED |

---

## 📞 CONTACTS

- **Product Owner:** TBD
- **Tech Lead:** TBD
- **UX Designer:** TBD
- **Stakeholders:** Admin team, Tenant admins, Analysts, Support team

---

## 📝 CHANGELOG

### Version 1.0.0 (8. listopadu 2025)
- ✅ Initial EPIC creation
- ✅ 11 stories defined (S1-S11)
- ✅ Roadmap established (5 phases, 18-19 sprints)
- ✅ Dependencies mapped (EPIC-014 blocker identified)
- ✅ Architecture documented (component hierarchy, state management)
- ✅ Business value calculated (ROI 666% first year)

---

**Version:** 1.0.0  
**Last Updated:** 8. listopadu 2025  
**Status:** 📋 PLANNED → Ready for Product Owner approval
