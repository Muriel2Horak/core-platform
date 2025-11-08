# SCOPE ANALYSIS: Reporting vs. UX/UI EPIC

**Datum:** 8. listopadu 2025  
**Autor:** Martin Horak  
**Kontext:** Rozhodnutí mezi EPIC-015 "Advanced Reporting UI" vs. EPIC-0XX "Advanced UX/UI Framework"

---

## 🎯 PŮVODNÍ SCOPE (Dashboard Builder)

### Co jsme analyzovali:
- ✅ Dashboard grid layout (12-column)
- ✅ Widget library (grafy, KPI tiles)
- ✅ Visual query builder
- ✅ Role-based default dashboards
- ✅ Data Views (KPI tiles + tabulka)
- ✅ Tile click actions

**Zaměření:** 📊 **Reporting & Analytics** (business intelligence, metrics)

---

## 🆕 ROZŠÍŘENÝ SCOPE (Complete Data UX)

### Co vlastně chceš (úplný požadavek):

#### **1. Universal Data Views** (nejen reporting)
- Tabulky pro **všechny entity** (Users, Tenants, Workflows, Roles, Permissions, Audits, Custom)
- **Přepínání view modes**:
  - 📋 Table view (default)
  - 📊 Chart view (bar, line, pie)
  - 🔥 Heatmap view (density)
  - 🗂️ Pivot view (kontingenční tabulka)
  - 📇 Card view (grid of cards)
  - 📄 List view (compact rows)

#### **2. Cube.js Advanced Features**
- **Filtrování**: Multi-select dropdowns, date ranges, tag inputs, search
- **Řezání (Drill-down)**: Click row → see detail breakdown
- **Kontingenční tabulka**: Rows × Columns cross-tabulation
- **Stránkování**: Server-side pagination (1000+ rows)
- **Export**: XLS, CSV, PDF (with formatting)
- **Sorting**: Multi-column sort (Shift+Click)
- **Grouping**: Group by tenant, status, date

#### **3. Editační Detail Popup** (multi-window editing)

**Příklad: User Detail Popup**
```
┌────────────────────────────────────────────────────────┐
│ User Detail: John Doe                   [_] [□] [×]    │ ← Dá se přesunout na jinou obrazovku
├────────────────────────────────────────────────────────┤
│ ┌──────────────────┬──────────────────┬──────────────┐│
│ │ Basic Info (6col)│ Activity (6col)  │ Graph (12col)││ ← Drag & drop layout
│ ├──────────────────┼──────────────────┤              ││
│ │ Name: [John Doe] │ 📊 Logins/day    │ 📈 User      ││
│ │ Email: [john@..] │   [Line chart]   │   Activity   ││
│ │ Status: [Active▼]│                  │   Trend      ││
│ │ Role: [Admin  ▼] │                  │   (30 days)  ││
│ ├──────────────────┴──────────────────┴──────────────┤│
│ │ 🗂️ Recent Workflows (Table)                        ││ ← Tabulka uvnitř popupu
│ │ Name           Status      Duration                ││
│ │ User Onboard   Completed   23min                   ││
│ │ Password Reset In Progress 5min                    ││
│ └────────────────────────────────────────────────────┘│
│                                       [Cancel] [Save] │
└────────────────────────────────────────────────────────┘
```

**Features:**
- ✅ **Kombinované layout**: Fields + Charts + Tables v jednom popupu
- ✅ **Multi-instance**: Otevřít 5 userů v 5 popup oknech současně
- ✅ **Multi-monitor**: Drag popup na druhou obrazovku (Windows API / Electron)
- ✅ **Persist state**: Obnovit otevřené popupy při refresh page
- ✅ **Z-index management**: Kliknutý popup se dostane do popředí

#### **4. Customizace & Personalization**

**Uživatel může upravit:**
- ✅ **Tabulka**: Přidat/odstranit sloupce, změnit pořadí, šířku
- ✅ **Detail Popup**: Přidat vlastní grafy, přeskládat fields, přidat related tables
- ✅ **Filtry**: Uložit oblíbené filtry ("Active users from last 7 days")
- ✅ **Export templates**: Uložit CSV/XLS formát (columns, ordering)

**Uložení & Sdílení:**
```json
{
  "userId": 123,
  "viewId": "users-table-custom",
  "scope": "private",  // 'private' | 'team' | 'public'
  "layout": {
    "columns": ["name", "email", "status", "lastLogin", "customField1"],
    "filters": {"status": "ACTIVE", "lastLogin": "7d"},
    "detailPopup": {
      "sections": [
        {"type": "fields", "columns": 6, "fields": ["name", "email", "status"]},
        {"type": "chart", "columns": 6, "query": {"measure": "Logins.count"}},
        {"type": "table", "columns": 12, "entity": "Workflows"}
      ]
    }
  }
}
```

**Scope options:**
- **Private**: Pouze pro mě
- **Team**: Sdíleno s mým tenant teamem
- **Public**: Default pro všechny users s danou rolí

---

## 🏗️ EPIC NAMING: Reporting vs. UX/UI

### ❌ **Option 1: EPIC-015 "Advanced Reporting UI"**

**Proč NE:**
- ❌ "Reporting" zní jako **business intelligence** (metrics, dashboards, analytics)
- ❌ Nezahrnuje **CRUD operations** (editační popupy)
- ❌ Nezahrnuje **entity management** (Users, Tenants, Workflows)
- ❌ Scope je **širší než reporting**

**Co chybí v názvu:**
- Multi-window editing (popup windows)
- Entity views (nejen reporting data)
- Customization framework (user layouts)

---

### ✅ **Option 2: EPIC-016 "Advanced Data UX Framework"** (DOPORUČENO)

**Proč ANO:**
- ✅ **"Data UX"** = práce s daty (read + write, view + edit)
- ✅ **"Framework"** = reusable systém pro všechny entity
- ✅ Zahrnuje **viewing** (tabulky, grafy) + **editing** (popupy)
- ✅ Zahrnuje **customization** (user layouts)
- ✅ Není omezeno na "reporting" (broader scope)

**Příklady use cases:**
- 📊 **Reporting**: Dashboard s KPI tiles + trend charts
- 👥 **User Management**: Tabulka users + editační popup
- 🔄 **Workflow Monitoring**: Table + detail popup s workflow graph
- 🏢 **Tenant Admin**: Tenant view s usage metrics + member list

---

### 🟡 **Option 3: EPIC-0XX "Advanced UX/UI Framework"** (user suggestion)

**Analýza:**
- ✅ **"UX/UI"** je broad (zahrnuje všechno)
- ⚠️ **Možná TOO broad** (UX/UI = celá aplikace, ne jen data views)
- ⚠️ Může být matoucí (lidé čekají general UI komponenty, ne data-specific)

**Co by do "UX/UI Framework" patřilo:**
- ✅ Button library, Form inputs, Modals, Toasts → ❌ To je **EPIC-014 Design System**!
- ✅ Navigation, Breadcrumbs, Sidebar → ❌ To je core layout!
- ✅ Data tables, charts, filters, popupy → ✅ **TO je naše EPIC!**

**Problém:** Může být matoucí že "UX/UI Framework" obsahuje jen **data-related UX**, ne general UI.

---

## 🎯 FINÁLNÍ DOPORUČENÍ

### **👉 EPIC-016: Advanced Data UX Framework**

**Popis:**
> Comprehensive framework for viewing, filtering, analyzing, and editing data across all entities with customizable layouts, multi-window support, and Cube.js-powered analytics.

**Klíčové capabilities:**
- 📊 **Universal Data Views** (table, chart, pivot, heatmap)
- 🎨 **Drag & Drop Layouts** (dashboards, detail popups)
- 🔍 **Advanced Filtering** (Cube.js integration)
- 🪟 **Multi-Window Editing** (popup windows, multi-monitor)
- 🎛️ **User Customization** (save layouts, share with team)
- 📤 **Export** (XLS, CSV, PDF)

**Co NENÍ v tomto EPICu:**
- ❌ General UI components (buttons, inputs) → **EPIC-014**
- ❌ Navigation, layout, sidebar → Core app structure
- ❌ Backend Cube.js schemas → **EPIC-004** (už hotové)

---

## 📋 EPIC-016 STORIES (Updated)

### **S1: Universal Data View Engine** (~80h, P0)
**Scope:**
- Generic `<DataView>` component (works for any entity: Users, Tenants, Workflows)
- View mode switcher: Table ↔ Chart ↔ Pivot ↔ Heatmap ↔ Cards
- Cube.js integration (dynamic schema detection)

**User Story:**
> Jako Admin, chci otevřít "Users" view a přepnout z tabulky na graf, abych viděl data jinak.

**Tasks:**
- T1: `<DataView>` component scaffold (10h)
- T2: View mode switcher UI (8h)
- T3: Table renderer (integrate EPIC-014 S9) (15h)
- T4: Chart renderer (integrate existing chart library) (12h)
- T5: Pivot renderer (20h)
- T6: Heatmap renderer (10h)
- T7: Card/List view (5h)

---

### **S2: Advanced Filtering & Search** (~60h, P0)
**Scope:**
- Multi-select filters (Status, Role, Tenant)
- Date range picker
- Tag input (multi-value search)
- Advanced filter builder (AND/OR conditions)
- Saved filters (favorites)

**User Story:**
> Jako Analyst, chci filtrovat users na "Active" AND "Role=Admin" AND "Last Login > 7 days", abych našel neaktivní adminy.

**Tasks:**
- T1: Filter bar UI component (15h)
- T2: Multi-select dropdown (EPIC-014 S3 integration) (10h)
- T3: Date range picker (8h)
- T4: Advanced filter builder (AND/OR logic) (15h)
- T5: Saved filters (DB + UI) (12h)

---

### **S3: Dashboard Grid Layout** (~70h, P0)
**Scope:**
- 12-column drag & drop grid
- Widget library (KPI tiles, charts, tables)
- Role-based default dashboards

**User Story:**
> Jako Tenant Admin, chci vytvořit dashboard s KPI tiles (Total Users, Active Users) a tabulkou Recent Activity.

**Tasks:**
- T1: react-grid-layout integration (15h)
- T2: Widget library panel (12h)
- T3: KPI Tile component (10h)
- T4: Chart widget (8h)
- T5: Table widget (10h)
- T6: Save/load layout (10h)
- T7: Role-based defaults (5h)

---

### **S4: Visual Query Builder** (~45h, P1)
**Scope:**
- Cube.js schema introspection
- Drag & drop query builder (measures, dimensions, filters)
- Live preview

**User Story:**
> Jako Business Analyst bez SQL znalostí, chci vytvořit query "Count of Workflows grouped by Status" klikáním.

**Tasks:**
- T1: Cube.js schema API (12h)
- T2: Entity picker (6h)
- T3: Measure/dimension selector (10h)
- T4: Filter builder (10h)
- T5: Live preview (7h)

---

### **S5: Multi-Window Detail Popups** (~100h, P0) 🆕
**Scope:**
- Popup window component (draggable, resizable)
- Multi-instance support (otevřít 5 popupů současně)
- Multi-monitor support (drag na jinou obrazovku)
- Z-index management (active window foreground)
- State persistence (obnovit popupy při reload)

**User Story:**
> Jako Support Agent, chci otevřít 3 user detail popupy vedle sebe, abych mohl porovnávat jejich data a editovat je paralelně.

**Příklad:**
```
Screen 1 (main)              Screen 2 (secondary)
┌─────────────────────┐      ┌─────────────────────┐
│ Users Table         │      │ User Detail: Jane   │
│ [List of users]     │      │ [Edit form]         │
└─────────────────────┘      └─────────────────────┘
  │                              │
  ├─ Popup: John Doe            │
  └─ Popup: Alice Smith         │
```

**Tasks:**
- T1: Popup window component (react-draggable, react-resizable) (20h)
- T2: Multi-instance state management (Redux/Zustand) (15h)
- T3: Multi-monitor support (Window API / Electron) (25h)
  - Detect screens: `window.screen.availLeft`, `availTop`
  - Open popup in specific position
  - Handle window.open() for separate windows (optional)
- T4: Z-index management (focus/blur events) (10h)
- T5: State persistence (localStorage, reopen on refresh) (15h)
- T6: Popup layout grid (drag & drop sections inside popup) (15h)

**Technical Challenges:**
- ⚠️ **Multi-monitor** v browseru je limitováno (nelze force window na jiný screen)
- ✅ **Workaround**: Použít Window.open() → otevře native OS window (can drag anywhere)
- ✅ **Alternative**: Electron app (full screen API)

---

### **S6: Customizable Popup Layouts** (~70h, P1) 🆕
**Scope:**
- Drag & drop layout builder pro detail popup
- Add sections: Fields, Charts, Tables
- Resize sections (6-col, 12-col)
- Save layout (private, team, public)

**User Story:**
> Jako Tenant Admin, chci upravit User Detail popup - přidat graf "Login Activity" a tabulku "Recent Workflows", a uložit to pro celý tým.

**Příklad UI:**
```
┌────────────────────────────────────────────────────────┐
│ Customize User Detail Popup               [Edit Mode] │
├────────────────────────────────────────────────────────┤
│ + Add Section: [▼ Fields] [▼ Chart] [▼ Table]         │
├────────────────────────────────────────────────────────┤
│ ┌──────────────────┬──────────────────┐               │
│ │ 📝 Basic Info    │ 📊 Activity Chart│  ← Drag to resize
│ │ [6 columns]      │ [6 columns]      │               │
│ ├──────────────────┴──────────────────┤               │
│ │ 🗂️ Recent Workflows Table            │               │
│ │ [12 columns]                         │               │
│ └──────────────────────────────────────┘               │
│                                                        │
│ Save as: ( ) Private  (•) Team  ( ) Public            │
│                             [Cancel] [Save Layout]    │
└────────────────────────────────────────────────────────┘
```

**Tasks:**
- T1: Layout editor mode (enable drag & drop) (12h)
- T2: Section library panel (Fields, Chart, Table) (10h)
- T3: Add/remove sections (8h)
- T4: Section configuration (choose fields, chart query, table entity) (20h)
- T5: Save layout API (POST /api/layouts/user-detail) (10h)
- T6: Load layout (private vs. team vs. public precedence) (10h)

---

### **S7: Export & Pagination** (~40h, P1)
**Scope:**
- Export table to XLS, CSV, PDF
- Server-side pagination (large datasets)
- Infinite scroll (optional)

**User Story:**
> Jako Analyst, chci exportovat filtered user list do Excel s custom columns (Name, Email, Last Login).

**Tasks:**
- T1: Export button UI (5h)
- T2: Backend export endpoint (XLS using Apache POI) (12h)
- T3: CSV export (5h)
- T4: PDF export (using iText) (10h)
- T5: Server-side pagination (8h)

---

### **S8: Kontingenční Tabulka (Pivot)** (~50h, P2)
**Scope:**
- Cross-tabulation (rows × columns)
- Cube.js pivot query
- Interactive drill-down

**User Story:**
> Jako Analyst, chci kontingenční tabulku "Workflows by Status (rows) × Tenant (columns)", abych viděl breakdown.

**Příklad:**
```
           │ Tenant A │ Tenant B │ Tenant C │ Total
───────────┼──────────┼──────────┼──────────┼───────
Completed  │   123    │   456    │   789    │ 1,368
In Progress│    45    │    67    │    89    │   201
Failed     │    12    │    23    │    34    │    69
───────────┼──────────┼──────────┼──────────┼───────
Total      │   180    │   546    │   912    │ 1,638
```

**Tasks:**
- T1: Pivot table component (react-pivottable) (15h)
- T2: Cube.js pivot query builder (15h)
- T3: Drill-down click (expand row → see detail) (12h)
- T4: Export pivot to XLS (8h)

---

### **S9: Extended Widget Types** (~150h, P2)
**Scope:**
- Advanced chart types: Heatmap, Sankey, Treemap, Gauge
- Chart customization (colors, legends)

**User Story:**
> Jako Data Analyst, chci heatmap "Workflow activity by Day of Week × Hour", abych viděl peak times.

**Tasks:**
- T1: Integrate @nivo/charts (10h)
- T2: Heatmap widget (25h)
- T3: Sankey diagram (30h)
- T4: Treemap (25h)
- T5: Gauge (20h)
- T6: Chart customization UI (40h)

---

### **S10: Sharing & Collaboration** (~30h, P3)
**Scope:**
- Share dashboard/view with team
- Public dashboards (anonymní přístup)
- Permissions (view-only, edit)

**Tasks:**
- T1: Share button UI (6h)
- T2: Generate share link (8h)
- T3: Team dashboards (10h)
- T4: Public dashboards (6h)

---

### **S11: EPIC-014 Integration** (~45h, P0)
**Scope:**
- Replace MUI components with Design System
- Forms (S3), Tables (S9), Loading (S7), Errors (S8)

**Tasks:**
- T1: Form components integration (15h)
- T2: Table component integration (20h)
- T3: Loading states (5h)
- T4: Error handling (5h)

---

## 📊 EFFORT SUMMARY

| Story | Effort | Priority | Sprint |
|-------|--------|----------|--------|
| S1: Data View Engine | 80h | P0 | 1-2 |
| S2: Filtering & Search | 60h | P0 | 2-3 |
| S3: Dashboard Grid | 70h | P0 | 3-4 |
| S4: Query Builder | 45h | P1 | 5 |
| S5: Multi-Window Popups | 100h | P0 | 6-8 | 🆕
| S6: Customizable Popups | 70h | P1 | 9-10 | 🆕
| S7: Export & Pagination | 40h | P1 | 11 |
| S8: Pivot Table | 50h | P2 | 12 |
| S9: Extended Widgets | 150h | P2 | 13-16 |
| S10: Sharing | 30h | P3 | 17 |
| S11: EPIC-014 Integration | 45h | P0 | 18 |

**TOTAL: ~740 hours (~18-19 sprints)**

---

## 🎯 FINAL RECOMMENDATION

### **👉 CREATE: EPIC-016 "Advanced Data UX Framework"**

**Proč tento název:**
- ✅ **"Data UX"** = jasně říká že jde o práci s daty (ne general UI)
- ✅ **"Framework"** = reusable systém pro všechny entity
- ✅ **"Advanced"** = pokročilé features (multi-window, pivot, customization)
- ✅ Zahrnuje **viewing + editing + customizing**

**Alternativní názvy (méně vhodné):**
- ❌ "Advanced Reporting UI" → příliš úzké (nezahrnuje CRUD)
- ❌ "UX/UI Framework" → příliš široké (matoucí, konflikt s EPIC-014)
- ⚠️ "Data Management UI" → ok, ale "management" zní více jako admin console

---

## 🔗 DEPENDENCIES

| Epic | Relationship | Status |
|------|--------------|--------|
| **EPIC-004** | Provides Cube.js schemas | ✅ S1 DONE (schemas ready) |
| **EPIC-014** | Provides Design System components | ⏳ S3, S9 BLOCKER |
| **EPIC-005** | Workflow engine integration | ✅ Can use for workflow views |

---

## 🚀 NEXT ACTIONS

1. ✅ Create folder: `backlog/EPIC-016-advanced-data-ux-framework/`
2. ✅ Write README.md (overview, roadmap, 11 stories)
3. ✅ Create S1-S11 story files
4. ✅ Update EPIC-004 README (add link to EPIC-016)
5. ✅ Update EPIC-014 README (add EPIC-016 as dependent)
6. ✅ Git commit

---

**Chceš aby jsem vytvořil EPIC-016 s tímto scopem? (Říkni ANO! 🚀)**
