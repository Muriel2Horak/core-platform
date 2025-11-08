# INTERACTIVE DASHBOARD BUILDER - Gap Analysis

**Datum:** 8. listopadu 2025  
**Autor:** Martin Horak  
**EPIC:** [EPIC-004: Reporting & Analytics Infrastructure](README.md)

---

## 🎯 POŽADAVEK (User Story)

**Jako** uživatel různých rolí (Admin, Tenant Admin, Analyst, Viewer),  
**chci** mít **interaktivní dashboard builder** s drag & drop editorem,  
**abych** mohl:
- Vytvořit vlastní dashboard bez programování
- Přepínat mezi defaultními dashboardy pro svou roli
- Rozmístit widgets (grafy, tabulky, KPI tiles) do 12-column grid layoutu
- Konfigurovat každý widget (entita, filtry, zobrazení)
- Uložit a sdílet dashboard s týmem

**Inspirace:** JIRA Dashboards, Power BI, Tableau

---

## 📋 CO MÁME (Current State)

### ✅ **S2: Dashboard Template Engine** (Implementováno)

**Co umí:**
- 📦 **Backend Template System**: JSON templates s placeholders `{{tenantId}}`, `{{workflowId}}`
- 🏭 **Template Instantiation**: Parametrický dashboard generation
- 🎨 **5 Pre-built Templates**: Workflow, Tenant, User, Performance, Custom
- 🔌 **Grafana Integration**: Automatické vytvoření Grafana dashboardu přes API
- 🔐 **Row-Level Security**: Tenant isolation v templates

**Příklad (Workflow Overview Template):**
```json
{
  "templateId": "workflow-overview-v1",
  "panels": [
    {
      "id": "workflow-count",
      "gridPos": {"x": 0, "y": 0, "w": 6, "h": 4},  // ⚠️ Hardcoded!
      "query": {
        "measures": ["WorkflowInstances.count"],
        "filters": [{"member": "WorkflowInstances.tenantId", "operator": "equals", "values": ["{{tenantId}}"]}]
      }
    }
  ]
}
```

**Použití:**
```typescript
// Frontend volá:
POST /api/reporting/templates/workflow-overview-v1/create-dashboard
{
  "parameters": {"tenantId": 1, "workflowId": "user-onboarding"},
  "dashboardTitle": "My Workflow Dashboard"
}

// → Backend vytvoří Grafana dashboard s pre-configured panels
```

**✅ Co to řeší:**
- Developer může vytvořit reusable template
- Business user může instancovat template s parametry
- **ALE**: User **NEMŮŽE** měnit layout, přidávat panely, konfigurovat widgets

---

## ❌ CO CHYBÍ (Gaps)

### 🔴 **GAP-1: Interactive Dashboard Builder UI** (HIGH PRIORITY)

**Co chybí:**
- ❌ **Drag & Drop Grid Editor**: Uživatel nemůže přesouvat widgets myší
- ❌ **Widget Library Panel**: Žádný "Add Widget" button s výběrem typů
- ❌ **Inline Widget Editing**: Nelze editovat widget directly na dashboardu (musí přes JSON)
- ❌ **Resize Handles**: Nelze změnit velikost widgetu (6x4 → 12x6)
- ❌ **Grid Snap**: Žádné visual grid guidelines

**Požadovaná UX:**

```
┌─────────────────────────────────────────────────────────────┐
│ Dashboard: My Custom Workflow Dashboard          [+ Widget] │
├─────────────────────────────────────────────────────────────┤
│ ┌──────────┬──────────┬──────────┬──────────┬──────────┐    │
│ │ 1  │ 2  │ 3  │ 4  │ 5  │ 6  │ 7  │ 8  │ 9  │ 10 │ 11│ 12│   │  (12-column grid)
│ ├──────────┴──────────┴──────────┼──────────┴──────────┤    │
│ │ 📊 Total Workflows (KPI)      │ ✅ Completion Rate   │    │  (2 widgets @ 6 cols each)
│ │          12,345               │        87%          │    │
│ ├─────────────────────────────────────────────────────┤    │
│ │ 📈 Workflow Trend (Line Chart)                      │    │  (12 cols wide)
│ │ [Line graph with time series]                       │    │
│ ├───────────────────────┬───────────────────────────┤    │
│ │ 📋 Recent Workflows   │ 🔍 Status Breakdown (Pie) │    │  (6 cols each)
│ │ [Table with 10 rows]  │ [Pie chart]                │    │
│ └───────────────────────┴───────────────────────────┘    │
│                                                            │
│ [Widgets can be dragged, resized, deleted]                │
└─────────────────────────────────────────────────────────────┘
```

**Click "+ Widget" → Modal:**
```
┌────────────────────────────────────────┐
│ Add Widget                        [×]  │
├────────────────────────────────────────┤
│ Step 1: Choose Type                    │
│ [ ] 📊 KPI Tile (single metric)        │
│ [ ] 📈 Chart (line, bar, pie, area)    │
│ [ ] 📋 Table (data grid)               │
│ [ ] 🔥 Heatmap                         │
│ [ ] 📊 Pivot Table (contingency)       │
│ [ ] 🔽 Filter (date, dropdown)         │
│                                        │
│            [Cancel]  [Next →]          │
└────────────────────────────────────────┘
```

```
┌────────────────────────────────────────┐
│ Add Widget - Configure Chart      [×]  │
├────────────────────────────────────────┤
│ Step 2: Data Source                    │
│ Entity:  [▼ WorkflowInstances     ]    │
│                                        │
│ Step 3: Query                          │
│ Measure: [▼ Count                 ]    │
│ Group By: [▼ Status               ]    │
│ Filters:                               │
│  + [Add Filter]                        │
│                                        │
│ Step 4: Visualization                  │
│ Chart Type: [▼ Pie Chart          ]    │
│ Colors: [Auto] [Custom Palette]        │
│                                        │
│         [← Back]  [Cancel]  [Add]      │
└────────────────────────────────────────┘
```

**Effort Estimate:** ~60-80 hours (complex frontend, state management, Cube.js integration)

---

### 🟡 **GAP-2: 12-Column Grid System** (MEDIUM PRIORITY)

**Co máme:**
- Grafana uses **24-column grid** (hardcoded in templates)
- Pozice widgets: `"gridPos": {"x": 0, "y": 0, "w": 6, "h": 4}`

**Co chybí:**
- ❌ **12-column responsive grid** (user-friendly, easier mental model)
- ❌ **Auto-layout algoritmus** (když widget nemá místo, posun dolů)
- ❌ **Breakpoints**: Desktop (12 cols), Tablet (6 cols), Mobile (1 col)
- ❌ **Gap control**: Spacing mezi widgets (default 16px)

**Požadované API:**
```typescript
interface DashboardLayout {
  columns: 12;  // ✅ User nastaví (12 for desktop, 6 for tablet, 1 for mobile)
  gap: 16;      // px spacing
  widgets: Widget[];
}

interface Widget {
  id: string;
  type: 'chart' | 'table' | 'kpi' | 'heatmap' | 'pivot' | 'filter';
  position: {
    x: number;     // 0-11 (12-column grid)
    y: number;     // Row index
    width: number; // 1-12 cols
    height: number; // Grid units (1 unit = 80px)
  };
  config: WidgetConfig;  // Chart query, colors, etc.
}
```

**Implementace:**
- Frontend: React Grid Layout library (react-grid-layout)
- Backend: Uložit layout do `dashboard_layouts` table

**Effort Estimate:** ~20-30 hours (library integration, responsive logic)

---

### 🟡 **GAP-3: Widget Configuration System** (MEDIUM PRIORITY)

**Co máme:**
- Hardcoded Cube.js queries v JSON templates
- Žádné UI pro query builder

**Co chybí:**
- ❌ **Visual Query Builder**: Uživatel nevyplňuje JSON, ale kliká dropdowny
- ❌ **Entita Picker**: Dropdown s dostupnými cube schemas (Users, Tenants, Workflows, Custom)
- ❌ **Measure Selector**: Multi-select (Count, Sum, Avg, Min, Max)
- ❌ **Dimension Selector**: Group by options (Status, Tenant, Date)
- ❌ **Filter Builder**: Inline filter editor (Status = COMPLETED AND TenantId = 1)
- ❌ **Chart Type Switcher**: Radio buttons (Line, Bar, Pie, Area, Scatter, Heatmap)

**Požadované UX:**

```
┌────────────────────────────────────────────────────────────┐
│ Configure Widget: Workflow Status Breakdown          [×]   │
├────────────────────────────────────────────────────────────┤
│ 📊 Data Source                                             │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Entity: [▼ WorkflowInstances                        ]│   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│ 📐 Metrics                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [×] Count             (aggregation: COUNT)           │   │
│ │ [ ] Average Duration  (aggregation: AVG duration)    │   │
│ │ [ ] Sum Cost          (aggregation: SUM cost)        │   │
│ │     [+ Add Metric]                                   │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│ 📊 Group By (Dimensions)                                   │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [▼ Status           ]  [+ Add Dimension]             │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│ 🔍 Filters                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Status      [equals ▼]  [COMPLETED        ]  [×]     │   │
│ │ TenantId    [equals ▼]  [1                ]  [×]     │   │
│ │                         [+ Add Filter]               │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│ 🎨 Visualization                                           │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ Type: ( ) Line  (•) Pie  ( ) Bar  ( ) Heatmap        │   │
│ │ Colors: [Auto] [Custom Palette: 🎨]                  │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│ 📄 Preview                                                 │
│ ┌──────────────────────────────────────────────────────┐   │
│ │ [Live preview of chart with sample data]             │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                            │
│              [Cancel]  [Save Widget]                       │
└────────────────────────────────────────────────────────────┘
```

**Backend API Needed:**
```typescript
// GET /api/reporting/cubes
// → Returns list of available Cube.js schemas
[
  {
    "name": "WorkflowInstances",
    "dimensions": ["id", "status", "tenantId", "createdAt", "workflowId"],
    "measures": ["count", "avgDuration", "sumCost"]
  },
  {
    "name": "Users",
    "dimensions": ["id", "email", "status", "tenantId"],
    "measures": ["count"]
  }
]

// POST /api/reporting/widgets/{widgetId}/preview
{
  "cube": "WorkflowInstances",
  "measures": ["WorkflowInstances.count"],
  "dimensions": ["WorkflowInstances.status"],
  "filters": [
    {"member": "WorkflowInstances.status", "operator": "equals", "values": ["COMPLETED"]}
  ]
}
// → Returns sample chart data (last 100 rows)
```

**Effort Estimate:** ~40-50 hours (query builder UI, Cube.js schema introspection, preview renderer)

---

### 🔴 **GAP-4: Role-based Default Dashboards** (HIGH PRIORITY)

**Co máme:**
- ❌ Žádné role-specific dashboardy
- User musí manuálně vytvořit dashboard z template

**Co chybí:**
- ❌ **Default Dashboard per Role**:
  - `ADMIN` → System Overview (all tenants, performance metrics, error rates)
  - `TENANT_ADMIN` → Tenant Dashboard (users, workflows, usage quotas)
  - `ANALYST` → Reporting Dashboard (custom queries, exports, scheduled reports)
  - `VIEWER` → Read-only KPI Dashboard (high-level metrics)
- ❌ **Auto-provisioning**: Když user se přihlásí poprvé → auto-create default dashboard
- ❌ **Dashboard Switching**: Top-bar dropdown "Switch Dashboard" (My Dashboards, Team Dashboards, Default)

**Požadované DB Schema:**
```sql
CREATE TABLE default_dashboards (
  id SERIAL PRIMARY KEY,
  role VARCHAR(50) NOT NULL UNIQUE,  -- 'ADMIN', 'TENANT_ADMIN', 'ANALYST', 'VIEWER'
  dashboard_layout JSONB NOT NULL,   -- Grid layout + widgets
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_dashboards (
  id SERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  dashboard_name VARCHAR(255),
  is_default BOOLEAN DEFAULT FALSE,  -- User's active dashboard
  layout JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);
```

**API Needed:**
```typescript
// GET /api/reporting/dashboards/default
// → Returns default dashboard for current user's role
{
  "dashboardId": "admin-default-v1",
  "layout": { /* 12-col grid with widgets */ }
}

// GET /api/reporting/dashboards/my
// → Returns user's custom dashboards
[
  {"id": 1, "name": "My Workflow Dashboard", "isDefault": true},
  {"id": 2, "name": "Q4 Performance", "isDefault": false}
]

// POST /api/reporting/dashboards/switch
{"dashboardId": 2}
// → Sets dashboardId=2 as user's active dashboard
```

**Effort Estimate:** ~30-40 hours (role seeding, provisioning logic, UI switcher)

---

### 🟡 **GAP-5: Widget Type Library** (MEDIUM PRIORITY)

**Co máme v Cube.js:**
- ✅ Line Chart
- ✅ Bar Chart
- ✅ Pie Chart
- ✅ Area Chart
- ✅ Scatter Plot

**Co CHYBÍ (požadované typy):**
- ❌ **KPI Tile** (single metric s delta: "12,345 ↑ +15%")
  - Příklad: Total Users, Revenue This Month, Active Workflows
- ❌ **Heatmap** (2D density visualization)
  - Příklad: Workflow activity by day of week + hour
- ❌ **Pivot Table / Contingency Table** (cross-tabulation)
  - Příklad: Workflows by Status (rows) × Tenant (cols)
- ❌ **Gauge / Progress Bar** (single value 0-100%)
  - Příklad: Completion Rate Gauge (87%)
- ❌ **Treemap** (hierarchical visualization)
  - Příklad: Workflow duration by category
- ❌ **Sankey Diagram** (flow visualization)
  - Příklad: User journey through workflow steps

**Knihovny k integraci:**
- Recharts (✅ už máme, ale jen basic charts)
- Nivo Charts (advanced: heatmap, sankey, treemap)
- D3.js (fully custom, ale complex)

**Effort Estimate:** ~20-30 hours per chart type (6 types × 25h = 150h total)

---

### 🟢 **GAP-6: Dashboard Sharing & Permissions** (LOW PRIORITY)

**Co máme:**
- ❌ Žádné sdílení dashboardů mezi users

**Co chybí:**
- ❌ **Share Dashboard** button → Generate shareable link
- ❌ **Permissions**: View-only, Edit, Admin
- ❌ **Team Dashboards**: Shared across tenant users
- ❌ **Public Dashboards**: Anonymní přístup (bez loginu)

**Effort Estimate:** ~15-20 hours (základní sharing, no complex permissions)

---

## 📊 PRIORITIZAČNÍ MATICE

| Gap | Priority | Impact | Effort | Ratio | Sprint |
|-----|----------|--------|--------|-------|--------|
| **GAP-1: Interactive Builder UI** | 🔴 HIGH | 🔥 CRITICAL (90% value) | 70h | 1.3 | S1-S2 |
| **GAP-4: Role-based Defaults** | 🔴 HIGH | 🔥 HIGH (70% value) | 35h | 2.0 | S2 |
| **GAP-3: Widget Config System** | 🟡 MEDIUM | 🔥 HIGH (80% value) | 45h | 1.8 | S3 |
| **GAP-2: 12-Column Grid** | 🟡 MEDIUM | 🟠 MEDIUM (40% value) | 25h | 1.6 | S3 |
| **GAP-5: Widget Library** | 🟡 MEDIUM | 🟠 MEDIUM (50% value) | 150h | 0.3 | S4-S6 |
| **GAP-6: Sharing** | 🟢 LOW | 🟢 LOW (20% value) | 20h | 1.0 | S7 |

**Total Effort:** ~345 hours (~8-9 sprints @ 40h/sprint)

---

## 🎯 DOPORUČENÁ ROADMAP

### **Phase 1: MVP Interactive Builder** (Sprint 1-2, ~105h)
- ✅ GAP-1: Drag & Drop Grid Editor (react-grid-layout)
- ✅ GAP-4: Role-based Default Dashboards
- ✅ Basic Widget Types: KPI Tile, Chart (Line/Bar/Pie), Table
- ✅ Save/Load dashboard layouts

**Deliverable:** User může:
- Otevřít default dashboard pro svou roli
- Přidat/odstranit widgets drag & drop
- Konfigurovat základní grafy (entity, measure, filter)
- Uložit vlastní dashboard

---

### **Phase 2: Advanced Configuration** (Sprint 3, ~70h)
- ✅ GAP-3: Visual Query Builder UI
- ✅ GAP-2: 12-Column Responsive Grid
- ✅ Preview Mode (live chart preview při konfiguraci)

**Deliverable:** User může:
- Vytvářet komplexní queries bez JSON
- Vidět preview dat před uložením widgetu
- Dashboard funguje na tabletu/mobilu

---

### **Phase 3: Extended Widget Types** (Sprint 4-6, ~150h)
- ✅ GAP-5: Heatmap, Pivot Table, Gauge, Treemap, Sankey
- ✅ Chart customization (colors, legends, axes)

**Deliverable:** User má k dispozici všechny chart typy z Power BI/Tableau

---

### **Phase 4: Collaboration** (Sprint 7, ~20h)
- ✅ GAP-6: Dashboard Sharing
- ✅ Team Dashboards

**Deliverable:** Dashboardy lze sdílet s týmem

---

## 📋 USER STORIES (Nové)

### **US-1: Interactive Dashboard Builder (GAP-1)**

**Jako** Tenant Admin,  
**chci** mít drag & drop dashboard editor,  
**abych** mohl vytvořit vlastní dashboard bez programování.

**Acceptance Criteria:**
- AC1: Kliknu "+ Widget" → Otevře se modal s výběrem typů (KPI, Chart, Table, Heatmap)
- AC2: Vyberu "Chart" → Krok 2: Vyber entitu (WorkflowInstances)
- AC3: Krok 3: Query (Measure: Count, Group By: Status, Filter: TenantId = current)
- AC4: Krok 4: Chart Type: Pie → Preview zobrazí live data
- AC5: Kliknu "Add" → Widget se přidá do gridu na pozici (x: 0, y: 0, w: 6, h: 4)
- AC6: Drag widget myší → Pozice se update (x: 6, y: 0)
- AC7: Resize handle (⌘ v pravém dolním rohu) → Šířka/výška se změní
- AC8: Kliknu "Save Dashboard" → Layout se uloží do DB
- AC9: Refresh page → Dashboard se načte se správným layoutem

**Tasks:**
- T1: Integrate react-grid-layout library (8h)
- T2: Widget Library Panel UI (12h)
- T3: Widget Configuration Modal (20h)
- T4: Dashboard Save/Load API (15h)
- T5: Grid State Management (Redux/Zustand) (15h)

**Effort:** ~70 hours

---

### **US-2: Role-based Default Dashboards (GAP-4)**

**Jako** nový uživatel s rolí ANALYST,  
**chci** mít automaticky vytvořený default dashboard s reporting widgets,  
**abych** nemusel začínat s prázdnou stránkou.

**Acceptance Criteria:**
- AC1: První přihlášení → Backend auto-creates dashboard z `default_dashboards` table (role='ANALYST')
- AC2: Dashboard obsahuje: 
  - Widget 1: Total Reports Created (KPI Tile, 6 cols)
  - Widget 2: Report Exports by Type (Pie Chart, 6 cols)
  - Widget 3: Recent Reports (Table, 12 cols)
- AC3: Top-bar má dropdown "Switch Dashboard" s options: "Default (Analyst)", "My Dashboards"
- AC4: User může kliknout "Customize" → Enters edit mode (GAP-1)

**Tasks:**
- T1: Seed default_dashboards table (SQL inserts pro 4 roles) (4h)
- T2: Auto-provisioning logic (on first login) (8h)
- T3: Dashboard Switcher UI (top-bar dropdown) (6h)
- T4: GET /api/reporting/dashboards/default endpoint (5h)
- T5: Integration tests (4 roles × 3 scenarios) (12h)

**Effort:** ~35 hours

---

### **US-3: Visual Query Builder (GAP-3)**

**Jako** Business Analyst bez SQL znalostí,  
**chci** vytvářet dashboard queries klikáním do dropdownů,  
**abych** nepotřeboval psát JSON nebo SQL.

**Acceptance Criteria:**
- AC1: Widget config modal má tabs: Data, Visualization, Filters
- AC2: Tab "Data":
  - Dropdown "Entity" → Shows: Users, Tenants, WorkflowInstances (z Cube.js schema)
  - Multi-select "Metrics" → Shows: Count, Avg Duration, Sum Cost
  - Multi-select "Dimensions" → Shows: Status, TenantId, CreatedAt
- AC3: Tab "Filters":
  - Row 1: [Status] [equals ▼] [COMPLETED] [×]
  - Row 2: [TenantId] [equals ▼] [1] [×]
  - Button "+ Add Filter"
- AC4: Tab "Visualization":
  - Radio buttons: Line / Bar / Pie / Area / Heatmap
  - Color picker: Auto / Custom Palette
- AC5: Preview Section: Live chart s sample data (last 100 rows)
- AC6: Click "Save" → Widget se přidá s correct Cube.js query JSON

**Tasks:**
- T1: GET /api/reporting/cubes endpoint (schema introspection) (10h)
- T2: Query Builder UI components (dropdowns, multi-selects, filter rows) (20h)
- T3: Query → Cube.js JSON converter (15h)
- T4: Live Preview renderer (calls Cube.js API) (10h)
- T5: Integration with GAP-1 widget system (5h)

**Effort:** ~45 hours

---

## 🔗 DEPENDENCIES

- **EPIC-014 (Design System):**
  - ⏳ **S3: Forms** → Potřebujeme pro widget config modal (input fields, dropdowns, validation)
  - ⏳ **S9: Tables** → Potřebujeme pro Table widget type
  - ✅ **S7: Loading** → Můžeme použít loading states
  - ✅ **S8: Errors** → Error handling v query builder

- **EPIC-004 S1 (Cube.js):** ✅ Already done → Data modeling ready

- **External Libraries:**
  - `react-grid-layout` (MIT license, 12k stars, production-ready)
  - `@nivo/charts` (heatmap, sankey, treemap)
  - `recharts` (✅ already used)

---

## 💰 BUSINESS VALUE

### **Před (Current State):**
- ❌ Pouze developer může vytvořit dashboard (2h práce)
- ❌ Business users závislí na IT
- ❌ Žádné role-specific dashboardy
- ❌ Grafana je tech-heavy (JSON config)

### **Po (With Interactive Builder):**
- ✅ Business user vytvoří dashboard za 10 minut
- ✅ Self-service reporting (no IT bottleneck)
- ✅ Role-based defaults (immediate value on first login)
- ✅ JIRA-like UX (familiar, easy to learn)

### **ROI:**
- **Time Saved:** 2h → 10min per dashboard (12x faster)
- **User Adoption:** +200% (business users can now use it)
- **IT Requests:** -80% (self-service instead of tickets)

---

## 🚀 NEXT ACTIONS

1. **Product Owner Review** (1h meeting):
   - Potvrdit priority GAP-1 + GAP-4 (MVP)
   - Schválit 8-9 sprint roadmap
   - User research: Které chart types jsou most requested?

2. **Tech Spike** (8h):
   - POC: react-grid-layout integration
   - POC: Cube.js schema introspection API
   - POC: Live preview renderer

3. **Create Stories** (4h):
   - US-1, US-2, US-3 → Break down into tasks
   - Add to backlog/EPIC-004/stories/S12-interactive-builder.md

4. **Update README** (30min):
   - Add S12 to story list
   - Update "Future Enhancements" → "In Progress"

---

## 📚 REFERENCES

- **Inspirace:** JIRA Dashboards, Power BI, Tableau, Grafana
- **Libraries:**
  - [react-grid-layout](https://github.com/react-grid-layout/react-grid-layout)
  - [Nivo Charts](https://nivo.rocks/)
  - [Cube.js Playground](https://cube.dev/docs/query-format) (query builder reference)

---

**Status:** 📋 **GAP ANALYSIS COMPLETE** → Ready for Product Owner approval  
**Next:** Create S12 story with task breakdown (~345h total)
