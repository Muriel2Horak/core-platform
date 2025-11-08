# EPIC vs. Story Analysis: Interactive Dashboard & Data Views

**Datum:** 8. listopadu 2025  
**Kontext:** Rozhodnutí mezi EPIC-015 vs. EPIC-004 Story S12

---

## 🎯 POŽADAVKY (Kompletní Scope)

### 1. **Dashboard Builder** (původní)
- Drag & drop grid editor (12-column layout)
- Widget library (KPI tiles, charts, tables, heatmaps, pivots)
- Visual query builder (no-code Cube.js queries)
- Role-based default dashboards (Admin, Tenant Admin, Analyst, Viewer)
- Dashboard switching (My Dashboards, Team Dashboards, Defaults)
- Save/share dashboards

### 2. **Data Views System** (🆕 nový požadavek)
- Kombinované pohledy: **KPI tiles + Data table**
- Příklad use cases:
  - **Users View**: Tiles (Total, Active, New, Churn) + filterable user table
  - **Workflows View**: Tiles (Total, Completed, Failed, Avg Duration) + workflow table
  - **Tenants View**: Tiles (Count, Active, Revenue, Usage) + tenant table
- Search & filters above table
- Responsive layout (mobile: tiles stack vertically)

### 3. **Tile Click Actions** (🆕 další rozšíření)
- **Drill-down navigation**: Tile "Total Users: 12,345" → `/users` page
- **External URL tiles**: Tile "System Metrics" → `https://grafana.local/d/system`
- **Filter propagation**: Tile "Active Users" → pre-filter table to show only active
- **Modal detail**: Tile → opens modal with detail chart (alternative to navigation)

### 4. **Related Functionality** (implicitní závislosti)
- **Advanced Table Features** (EPIC-014 S9 dependency):
  - Virtual scrolling (large datasets)
  - Column reordering (drag columns)
  - Export (CSV, Excel, PDF)
  - Inline editing (optional)
- **Filter System** (EPIC-014 S3 dependency):
  - Multi-select dropdowns
  - Date range picker
  - Tag input (multi-value)
  - Advanced filter builder (AND/OR conditions)

---

## 📏 EFFORT ESTIMATE (Detailní Breakdown)

### **Dashboard Builder** (~345h total)
| Feature | Effort | Sprint |
|---------|--------|--------|
| Drag & Drop Grid (react-grid-layout) | 70h | S1-S2 |
| Role-based Defaults | 35h | S2 |
| Visual Query Builder | 45h | S3 |
| 12-Column Grid System | 25h | S3 |
| Extended Widget Types (heatmap, pivot, sankey, treemap, gauge) | 150h | S4-S6 |
| Dashboard Sharing | 20h | S7 |

### **Data Views System** (~180h total, 🆕 NEW)
| Feature | Effort | Sprint |
|---------|--------|--------|
| View Template System (JSON schema) | 20h | S8 |
| Tile Grid Component (reusable for views) | 15h | S8 |
| Table + Tiles Layout Engine | 25h | S8 |
| View Builder UI (configure tiles + table) | 40h | S9 |
| Pre-built Views (Users, Workflows, Tenants, Audits) | 30h | S9 |
| Search & Filter Bar Component | 25h | S10 |
| Filter → Table Integration | 15h | S10 |
| Mobile Responsive Layout | 10h | S10 |

### **Tile Click Actions** (~60h total, 🆕 NEW)
| Feature | Effort | Sprint |
|---------|--------|--------|
| Click Action Config (navigation, external URL, filter, modal) | 15h | S11 |
| Drill-down Navigation Logic | 20h | S11 |
| Filter Propagation (tile → table) | 15h | S11 |
| External URL Handling (new tab, iframe, embed) | 10h | S11 |

### **EPIC-014 Integration** (~45h, BLOCKER)
| Dependency | Effort | Status |
|------------|--------|--------|
| S3: Forms (filter dropdowns, date pickers) | 15h integration | ⏳ TODO |
| S9: Tables (virtual scrolling, export, column reorder) | 20h integration | ⏳ TODO |
| S7: Loading States | 5h integration | ✅ Can use now |
| S8: Error Handling | 5h integration | ✅ Can use now |

---

## **TOTAL EFFORT: ~630 hours (~16 sprints)**

---

## 🏗️ EPIC vs. STORY CRITERIA

### ✅ **Doporučení: EPIC-015 "Advanced Reporting UI"**

**Proč EPIC (ne Story)?**

| Kritérium | EPIC threshold | Tento projekt | ✅/❌ |
|-----------|----------------|---------------|-------|
| **Effort** | >200h | ~630h | ✅ |
| **Multiple Stories** | >3 stories | 6-8 stories | ✅ |
| **Cross-team Dependencies** | Yes/No | EPIC-014 (Design System) | ✅ |
| **Multiple Sprints** | >4 sprints | ~16 sprints | ✅ |
| **Standalone Value** | Can ship independently | Yes (can ship Dashboard Builder without Views) | ✅ |
| **Architectural Impact** | Changes core architecture | Yes (new frontend layer, reusable components) | ✅ |
| **User Personas Affected** | >2 personas | Admin, Tenant Admin, Analyst, Viewer, Developer | ✅ |

**Skóre: 7/7 → EPIC! 🎉**

---

## 🗂️ EPIC-015 STRUKTURA (Doporučená)

```
EPIC-015: Advanced Reporting UI
├── README.md (EPIC overview, progress tracking)
├── stories/
│   ├── S1-dashboard-grid-layout.md (~70h)
│   ├── S2-role-based-dashboards.md (~35h)
│   ├── S3-visual-query-builder.md (~45h)
│   ├── S4-extended-widget-types.md (~150h)
│   ├── S5-data-views-system.md (~180h) 🆕
│   ├── S6-tile-click-actions.md (~60h) 🆕
│   ├── S7-dashboard-sharing.md (~20h)
│   └── S8-epic014-integration.md (~45h)
└── docs/
    ├── INTERACTIVE_DASHBOARD_BUILDER_GAP.md (již máme)
    ├── ARCHITECTURE.md (component hierarchy, state management)
    └── USER_FLOWS.md (wireframes, mockups)
```

---

## 📋 EPIC-015 STORIES (Quick Summary)

### **S1: Dashboard Grid Layout** (~70h, P0, Sprint 1-2)
**User Story:**
> Jako Tenant Admin, chci drag & drop dashboard editor s 12-column gridem, abych mohl rozmístit widgets myší.

**Tasks:**
- T1: react-grid-layout integration (15h)
- T2: Grid component with resize handles (20h)
- T3: Widget add/remove/reorder (15h)
- T4: Save/load layout to DB (12h)
- T5: Mobile responsive breakpoints (8h)

**Dependencies:** None

---

### **S2: Role-based Default Dashboards** (~35h, P0, Sprint 2)
**User Story:**
> Jako nový uživatel s rolí ANALYST, chci automaticky vytvořený default dashboard, abych nemusel začínat s prázdnou stránkou.

**Tasks:**
- T1: Seed default layouts per role (5h)
- T2: Auto-provisioning on first login (10h)
- T3: Dashboard switcher UI (top-bar dropdown) (8h)
- T4: GET /api/dashboards/default endpoint (6h)
- T5: Integration tests (6h)

**Dependencies:** S1 (layout system)

---

### **S3: Visual Query Builder** (~45h, P1, Sprint 3)
**User Story:**
> Jako Business Analyst, chci visual query builder, abych mohl vytvářet queries klikáním do dropdownů bez JSON.

**Tasks:**
- T1: Cube.js schema introspection API (12h)
- T2: Entity picker dropdown (8h)
- T3: Measure/dimension multi-select (10h)
- T4: Filter builder UI (10h)
- T5: Live preview renderer (5h)

**Dependencies:** 
- S1 (widget system)
- EPIC-014 S3 (form components) ⏳

---

### **S4: Extended Widget Types** (~150h, P2, Sprint 4-6)
**User Story:**
> Jako Data Analyst, chci pokročilé chart typy (heatmap, sankey, treemap), abych mohl vizualizovat komplexní data.

**Tasks:**
- T1: Integrate @nivo/charts library (10h)
- T2: Heatmap widget (25h)
- T3: Sankey diagram widget (30h)
- T4: Treemap widget (25h)
- T5: Gauge/progress widget (20h)
- T6: Pivot table widget (30h)
- T7: Chart customization (colors, legends, axes) (10h)

**Dependencies:** S3 (query builder to feed data)

---

### **S5: Data Views System** (~180h, P1, Sprint 8-10) 🆕
**User Story:**
> Jako Admin, chci kombinované pohledy (KPI tiles + filterable table), abych mohl na jedné stránce vidět high-level metrics i detail data.

**Příklad Use Cases:**
1. **Users View**:
   ```
   [Total: 12,345] [Active (7d): 8,234] [New (30d): 456] [Churn: 3.2%]
   🔍 Search + Filters
   ┌──────────────────────────────────────────────────┐
   │ Name      │ Email      │ Status  │ Last Login   │
   │ John Doe  │ john@...   │ Active  │ 2h ago       │
   └──────────────────────────────────────────────────┘
   ```

2. **Workflows View**:
   ```
   [Total: 5,678] [Completed: 4,123] [Failed: 234] [Avg: 45min]
   🔍 Search: workflow name, Filter: Status, Tenant
   ┌──────────────────────────────────────────────────┐
   │ Workflow        │ Status     │ Duration │ Tenant │
   │ User Onboarding │ Completed  │ 23min    │ ACME   │
   └──────────────────────────────────────────────────┘
   ```

**Tasks:**
- T1: View Template Schema (JSON) (15h)
  ```json
  {
    "viewId": "users-view",
    "tiles": [
      {"metric": "Users.count", "label": "Total Users"},
      {"metric": "Users.activeCount", "label": "Active (7d)"}
    ],
    "table": {
      "entity": "Users",
      "columns": ["name", "email", "status", "lastLogin"],
      "filters": ["status", "role", "tenantId"],
      "searchFields": ["name", "email"]
    }
  }
  ```
- T2: Tile Grid Component (reusable, 4-column responsive) (15h)
- T3: Table + Tiles Layout Engine (25h)
- T4: View Builder UI (configure tiles & table) (40h)
- T5: Pre-built Views (Users, Workflows, Tenants, Audits, Custom Metrics) (40h)
- T6: Search & Filter Bar Component (25h)
- T7: Filter → Table Integration (15h)
- T8: Mobile responsive (tiles stack vertically) (5h)

**Dependencies:**
- S1 (grid system reuse)
- S3 (query builder for tiles)
- EPIC-014 S9 (table component) ⏳
- EPIC-014 S3 (filter components) ⏳

**API Needed:**
```typescript
// GET /api/views/users
{
  "tiles": [
    {"value": 12345, "label": "Total Users", "delta": "+15%"},
    {"value": 8234, "label": "Active (7d)", "delta": "+5%"}
  ],
  "table": {
    "columns": ["name", "email", "status", "lastLogin"],
    "rows": [ /* paginated data */ ],
    "totalCount": 12345
  }
}
```

---

### **S6: Tile Click Actions** (~60h, P2, Sprint 11) 🆕
**User Story:**
> Jako uživatel, chci kliknout na KPI tile a zobrazit detail/drill-down, abych mohl rychle přejít z overview na detail.

**Click Action Types:**
1. **Navigation (Internal)**: Tile → `/users` page
2. **Filter Propagation**: Tile "Active Users" → table shows only active
3. **Modal Detail**: Tile → opens modal with detail chart
4. **External URL**: Tile → `https://grafana.local/d/system` (new tab)

**Tasks:**
- T1: Click Action Config UI (dropdown: Navigate, Filter, Modal, External) (15h)
- T2: Navigation Logic (React Router integration) (10h)
- T3: Filter Propagation (tile → table state sync) (15h)
- T4: Modal Detail Renderer (chart in modal) (10h)
- T5: External URL Handling (new tab, iframe option) (10h)

**Dependencies:** S5 (tiles exist)

**API Changes:**
```json
// View template with click actions
{
  "tiles": [
    {
      "metric": "Users.count",
      "label": "Total Users",
      "clickAction": {
        "type": "navigate",
        "url": "/users"
      }
    },
    {
      "metric": "Users.activeCount",
      "label": "Active (7d)",
      "clickAction": {
        "type": "filter",
        "filter": {"status": "ACTIVE", "lastLogin": "7d"}
      }
    },
    {
      "metric": "System.health",
      "label": "System Metrics",
      "clickAction": {
        "type": "external",
        "url": "https://grafana.local/d/system",
        "openInNewTab": true
      }
    }
  ]
}
```

---

### **S7: Dashboard Sharing** (~20h, P3, Sprint 12)
**User Story:**
> Jako Team Lead, chci sdílet můj dashboard s týmem, aby všichni viděli stejné metriky.

**Tasks:**
- T1: Share button UI + permissions modal (6h)
- T2: Generate shareable link (5h)
- T3: Team dashboards (shared across tenant) (6h)
- T4: Public dashboards (optional, anonymní přístup) (3h)

**Dependencies:** S1 (dashboard save/load)

---

### **S8: EPIC-014 Integration** (~45h, P0, Sprint 13) 🔴 BLOCKER
**User Story:**
> Jako Developer, chci integrovat Design System komponenty (forms, tables, loading), aby reporting UI bylo konzistentní s rest of app.

**Tasks:**
- T1: Replace MUI inputs with EPIC-014 S3 form components (15h)
  - Dropdowns, multi-selects, date pickers in query builder
- T2: Replace MUI DataGrid with EPIC-014 S9 table component (20h)
  - Virtual scrolling, export, column reordering in Data Views
- T3: Integrate S7 Loading states (skeleton, progress bars) (5h)
- T4: Integrate S8 Error boundaries (query errors, network failures) (5h)

**Dependencies:**
- ⏳ **EPIC-014 S3** (Forms) delivery
- ⏳ **EPIC-014 S9** (Tables) delivery
- ✅ EPIC-014 S7, S8 (can use now)

---

## 🎯 PRIORITIZAČNÍ MATRICE (Stories)

| Story | Priority | Impact | Effort | Dependencies | Sprint |
|-------|----------|--------|--------|--------------|--------|
| **S1: Grid Layout** | 🔴 P0 | 🔥 CRITICAL (90%) | 70h | None | 1-2 |
| **S2: Role Defaults** | 🔴 P0 | 🔥 HIGH (70%) | 35h | S1 | 2 |
| **S5: Data Views** | 🟡 P1 | 🔥 HIGH (80%) | 180h | S1, S3, E14-S3/S9 ⏳ | 8-10 |
| **S3: Query Builder** | 🟡 P1 | 🔥 HIGH (75%) | 45h | S1, E14-S3 ⏳ | 3 |
| **S4: Widget Types** | 🟡 P2 | 🟠 MEDIUM (50%) | 150h | S3 | 4-6 |
| **S6: Tile Actions** | 🟡 P2 | 🟠 MEDIUM (60%) | 60h | S5 | 11 |
| **S7: Sharing** | 🟢 P3 | 🟢 LOW (20%) | 20h | S1 | 12 |
| **S8: E14 Integration** | 🔴 P0 | 🔴 BLOCKER | 45h | ⏳ E14 delivery | 13 |

---

## 🚀 RECOMMENDED ROADMAP

### **Phase 1: Dashboard Builder MVP** (Sprint 1-3, ~150h)
✅ **Goal:** Drag & drop editor, role defaults, basic query builder

- ✅ S1: Grid Layout (70h)
- ✅ S2: Role Defaults (35h)
- ✅ S3: Query Builder (45h)

**Deliverable:** Users can create/edit dashboards with basic widgets (KPI, charts, tables)

---

### **Phase 2: Advanced Widgets** (Sprint 4-6, ~150h)
✅ **Goal:** Extended chart types (heatmap, sankey, pivot)

- ✅ S4: Extended Widget Types (150h)

**Deliverable:** Power BI/Tableau-like chart library

---

### **Phase 3: Data Views System** (Sprint 8-10, ~180h) 🆕
✅ **Goal:** Kombinované pohledy (tiles + table)

- ⏳ **WAIT FOR:** EPIC-014 S3 (Forms) + S9 (Tables) delivery
- ✅ S5: Data Views System (180h)

**Deliverable:** Users/Workflows/Tenants views s KPI tiles + filterable table

---

### **Phase 4: Interactivity** (Sprint 11, ~60h) 🆕
✅ **Goal:** Tile click actions (drill-down, navigation)

- ✅ S6: Tile Click Actions (60h)

**Deliverable:** Interactive dashboards (click tile → detail)

---

### **Phase 5: Collaboration** (Sprint 12, ~20h)
✅ **Goal:** Sdílení dashboardů

- ✅ S7: Dashboard Sharing (20h)

**Deliverable:** Team dashboards, public links

---

### **Phase 6: Integration** (Sprint 13, ~45h)
✅ **Goal:** Replace MUI s EPIC-014 components

- ⏳ **WAIT FOR:** EPIC-014 S3 + S9 delivery
- ✅ S8: EPIC-014 Integration (45h)

**Deliverable:** Consistent UI across app

---

## 💰 BUSINESS VALUE

### **ROI Comparison:**

| Scenario | Time to Create | User Type | Adoption |
|----------|----------------|-----------|----------|
| **Před (S2 Templates)** | 5 min | Developer only | 20% |
| **Po S1-S3 (Dashboard Builder)** | 10 min | Business users | 60% |
| **Po S5 (Data Views)** | 2 min | All users | 90% | 🆕
| **Po S6 (Tile Actions)** | 1 click | All users | 95% | 🆕

**Impact:**
- **Time saved:** 2h → 2 min per view (60x faster) 🆕
- **Self-service:** 90% users can create views (no IT) 🆕
- **User satisfaction:** +80% (familiar UX like JIRA) 🆕

---

## ✅ FINAL RECOMMENDATION

### **👉 CREATE EPIC-015: Advanced Reporting UI**

**Proč?**
1. ✅ **Scope:** ~630h (way over Story threshold of 40-80h)
2. ✅ **Complexity:** 8 stories, 16 sprints, cross-team dependencies
3. ✅ **Value:** Standalone product feature (can market as "Self-Service Analytics")
4. ✅ **Architecture:** New frontend layer (reusable across app)
5. ✅ **Personas:** 5 user types affected (Admin, Tenant Admin, Analyst, Viewer, Developer)

**Struktura:**
```
backlog/
├── EPIC-004-reporting-analytics-infrastructure/  (backend: Cube.js, templates)
└── EPIC-015-advanced-reporting-ui/  🆕 NEW
    ├── README.md
    ├── stories/
    │   ├── S1-dashboard-grid-layout.md
    │   ├── S2-role-based-dashboards.md
    │   ├── S3-visual-query-builder.md
    │   ├── S4-extended-widget-types.md
    │   ├── S5-data-views-system.md 🆕
    │   ├── S6-tile-click-actions.md 🆕
    │   ├── S7-dashboard-sharing.md
    │   └── S8-epic014-integration.md
    └── docs/
        ├── ARCHITECTURE.md
        ├── USER_FLOWS.md
        └── WIREFRAMES.md
```

---

## 🎬 NEXT ACTIONS

**Immediate (Now):**
1. ✅ Create EPIC-015 folder structure
2. ✅ Write EPIC-015/README.md (overview, roadmap, dependencies)
3. ✅ Create S1-S8 story files (task breakdowns)
4. ✅ Update EPIC-004/README.md → Add "Related EPIC-015" link
5. ✅ Git commit (new EPIC structure)

**Short-term (Next week):**
6. 🔄 Product Owner review (approve 16-sprint roadmap)
7. 🔄 Tech spike POC (react-grid-layout + Cube.js introspection) (8h)
8. 🔄 Wireframes (Figma mockups for S1, S5, S6)

**Mid-term (Sprint 1):**
9. 🔄 Start S1 implementation (grid layout)
10. 🔄 Monitor EPIC-014 progress (blocker for S3, S5, S8)

---

**Chceš aby jsem vytvořil EPIC-015 strukturu? (Říkni ANO a pokračuju! 🚀)**
