# Reporting UI - Usage Guide

Tento dokument popisuje, jak používat nové reporting komponenty vytvořené ve FÁZI 3.

---

## 📦 Instalace

### 1. Install dependencies
```bash
cd frontend
npm install
```

Nové dependencies:
- `ag-grid-community` ^31.3.2
- `ag-grid-react` ^31.3.2
- `echarts` ^5.5.0
- `echarts-for-react` ^3.0.2

### 2. Add routing
V `frontend/src/App.tsx` přidejte:

```typescript
import { ReportingPage } from './components/Reporting';

// V routes sekci:
<Route path="/reporting" element={<ReportingPage />} />
```

---

## 🧩 Komponenty

### 1. ExplorerGrid

**Použití:**
```jsx
import { ExplorerGrid } from './components/Reporting';

function MyReportPage() {
  return (
    <ExplorerGrid 
      entity="users_directory"
      initialFilters={{ status: 'ACTIVE' }}
      onRowClick={(row) => console.log('Row clicked:', row)}
      onDrillDown={(data) => console.log('Drill down:', data)}
    />
  );
}
```

**Props:**
- `entity` (string, required): Název entity (např. 'users_directory', 'tenants_registry')
- `initialFilters` (object, optional): Počáteční filtry
- `onRowClick` (function, optional): Callback při kliknutí na řádek
- `onDrillDown` (function, optional): Callback pro drill-down navigaci

**Funkce:**
- **Server-side pagination:** 10/25/50/100 rows per page
- **Sorting:** Klikněte na column header
- **Inline editing:** Double-click na buňku → editujte → Enter/blur = auto-save
- **Bulk actions:** 
  1. Označte checkboxy u řádků
  2. Toolbar se zobrazí s počtem vybraných řádků
  3. Klikněte "Activate" nebo "Deactivate"
- **Export:** Klikněte na tlačítko Export → stáhne CSV

**API endpoints používané:**
- `GET /api/reports/metadata/{entity}/spec` - Načte spec entity
- `POST /api/reports/query` - Query data s pagination/sort/filter
- `PATCH /api/entities/{entity}/{id}` - Update jednoho řádku (Phase 4)
- `POST /api/entities/{entity}/bulk-update` - Bulk update (Phase 4)

**Optimistic Locking:**
ExplorerGrid používá If-Match header pro concurrent edit detection:
```http
PATCH /api/entities/users_directory/123-456-789
If-Match: 5
Content-Type: application/json

{
  "status": "INACTIVE"
}
```

Pokud jiný uživatel mezitím upravil záznam (verze se změnila), server vrátí:
```http
HTTP/1.1 409 Conflict
{
  "message": "Concurrent modification detected. Please reload and try again.",
  "currentVersion": 6
}
```

ExplorerGrid zobrazí chybovou hlášku a automaticky reload data.

---

### 2. ChartPanel

**Použití:**
```jsx
import { ChartPanel } from './components/Reporting';

function MyChartPage() {
  return (
    <ChartPanel 
      entity="users_directory"
      type="bar"
      xField="status"
      yField="count"
      onDrillDown={(data) => {
        console.log('Clicked on:', data.status, 'value:', data.value);
        // Navigate to filtered view
      }}
    />
  );
}
```

**Props:**
- `entity` (string, required): Název entity
- `type` (string, optional): Chart type ('bar' | 'line' | 'pie'), default: 'bar'
- `xField` (string, optional): Pole pro X-osu (kategorie), default: 'status'
- `yField` (string, optional): Pole pro Y-osu (měření), default: 'count'
- `onDrillDown` (function, optional): Callback při kliknutí na chart element

**Chart types:**
- **Bar:** Sloupcový graf (kategorie na X, hodnoty na Y)
- **Line:** Spojnicový graf (s smooth křivkami a area fill)
- **Pie:** Koláčový graf (s procenty a legendou)

**Runtime type switching:**
ChartPanel obsahuje selector pro změnu typu grafu za běhu.

**API endpoint:**
```javascript
POST /api/reports/query
{
  "entity": "users_directory",
  "dimensions": ["status"],
  "measures": [],
  "groupBy": ["status"]
}
```

Response:
```json
{
  "data": [
    { "status": "ACTIVE", "count": 150 },
    { "status": "INACTIVE", "count": 30 }
  ]
}
```

---

### 3. ChartGrid

**Použití:**
```jsx
import { ChartGrid } from './components/Reporting';

function MyDashboard() {
  return (
    <ChartGrid 
      entity="users_directory"
      charts={[
        { type: 'bar', xField: 'status', yField: 'count', title: 'By Status' },
        { type: 'pie', xField: 'department', yField: 'count', title: 'By Department' },
        { type: 'line', xField: 'created_at', yField: 'count', title: 'Signups Over Time' }
      ]}
    />
  );
}
```

**Props:**
- `entity` (string, required): Název entity
- `charts` (array, optional): Pole chart konfigurací

**Layout:**
MUI Grid - 2 columns na desktop (md=6), 1 column na mobile (xs=12)

---

### 4. ReportingPage

**Použití:**
```jsx
import { ReportingPage } from './components/Reporting';

// V App.tsx routes:
<Route path="/reporting" element={<ReportingPage />} />
```

**Funkce:**
- **Entity Selector:** Dropdown s dostupnými entitami
  - users_directory
  - tenants_registry
  - keycloak_groups
- **Tabs:**
  - Table View - ExplorerGrid
  - Charts - ChartGrid (2 default charts)
  - Pivot Table - Disabled (čeká na Perspective)
- **Breadcrumbs:** Navigace zpět v drill-down historii

**Navigation flow:**
1. Uživatel vybere entity (např. "Users")
2. Zobrazí se Table View s daty
3. Uživatel klikne na hodnotu → drill-down
4. Přidá se breadcrumb
5. Uživatel může kliknout na breadcrumb → vrátí se zpět

---

## 🧪 Testování

### Storybook
```bash
cd frontend
npm run storybook
```

Navigace: `Reporting > ExplorerGrid`

**Dostupné stories:**
- Default - Základní view s users_directory
- Filtered By Status - Pre-filtered (status=ACTIVE)
- Tenants View - Alternative entity
- Groups View - Alternative entity
- With Drill Down - S drill-down handlerem

### E2E Tests (Playwright)
```bash
cd frontend
npm run test:e2e
```

**Testy:**
1. should load reporting page with default entity
2. should display data grid with users
3. should switch between table and chart views
4. should change entity in selector
5. should paginate through data
6. should sort by column
7. should select multiple rows for bulk action
8. should export data to CSV
9. should handle inline cell editing
10. should show error on concurrent edit conflict
11. should render chart with data
12. should switch between chart types

---

## 🔧 Customization

### Přidání nové entity

1. **Přidejte Cube schema:**
```javascript
// docker/cube/schema/NewEntity.js
cube('NewEntity', {
  sql: `SELECT * FROM new_entity WHERE tenant_id = ${SECURITY_CONTEXT.tenantId.filter()}`,
  // dimensions, measures...
});
```

2. **Přidejte do entity selectoru:**
```typescript
// ReportingPage.tsx
const availableEntities = [
  { value: 'users_directory', label: 'Users' },
  { value: 'tenants_registry', label: 'Tenants' },
  { value: 'keycloak_groups', label: 'Groups' },
  { value: 'new_entity', label: 'New Entity' } // ← NEW
];
```

3. **Backend endpoint vrátí spec:**
```bash
curl http://localhost:8080/api/reports/metadata/NewEntity/spec
```

ExplorerGrid automaticky vygeneruje columns z spec!

### Custom columns v ExplorerGrid

ExplorerGrid auto-generuje columns z entity spec, ale můžete je customize:

```jsx
<ExplorerGrid 
  entity="users_directory"
  columnOverrides={{
    'status': {
      headerName: 'User Status',
      width: 150,
      cellRenderer: (params) => (
        <Chip 
          label={params.value} 
          color={params.value === 'ACTIVE' ? 'success' : 'default'}
        />
      )
    }
  }}
/>
```

(Poznámka: `columnOverrides` není aktuálně implementováno, ale lze snadno přidat)

### Custom chart config

```jsx
<ChartPanel 
  entity="users_directory"
  type="bar"
  xField="department"
  yField="salary"
  chartOptions={{
    title: { text: 'Average Salary by Department' },
    yAxis: { name: 'Salary (USD)' },
    series: [{
      itemStyle: { color: '#2196f3' }
    }]
  }}
/>
```

(Poznámka: `chartOptions` není aktuálně implementováno, ale lze snadno přidat)

---

## 🐛 Troubleshooting

### ExplorerGrid nezobrazuje data

**Příčina:** Backend endpoint `/api/reports/query` neexistuje nebo vrací chybu

**Řešení:**
1. Zkontrolujte, že backend běží: `curl http://localhost:8080/actuator/health`
2. Zkontrolujte Cube.js: `curl http://localhost:4000/readyz`
3. Otevřete Browser DevTools → Network → najděte failed request
4. Zkontrolujte backend logy

### Inline edit nefunguje (PATCH endpoint)

**Příčina:** PATCH `/api/entities/{entity}/{id}` endpoint neexistuje (bude implementován v PHASE 4)

**Řešení:**
- Aktuálně ExplorerGrid volá tento endpoint, ale backend ještě nevrací response
- PHASE 4 implementuje EntityCrudController s tímto endpointem
- Do té doby inline edit zobrazí error

### Bulk actions nefungují

**Příčina:** POST `/api/entities/{entity}/bulk-update` endpoint neexistuje (PHASE 4)

**Řešení:**
- Stejně jako inline edit, čeká na PHASE 4 implementaci
- BulkUpdateController bude vytvořen v PHASE 4

### ChartPanel nezobrazuje graf

**Příčina 1:** ECharts dependencies nejsou nainstalovány

**Řešení:**
```bash
cd frontend
npm install
```

**Příčina 2:** Data z API jsou prázdná

**Řešení:**
- Zkontrolujte POST /api/reports/query response
- Zkontrolujte, že Cube.js má data v cache
- Zkontrolujte browser console pro errors

### Storybook nefunguje

**Příčina:** `@storybook/react` není nainstalován

**Řešení:**
```bash
cd frontend
npm install --save-dev @storybook/react @storybook/addon-essentials
```

Nebo použijte existující Storybook setup pokud už je v projektu.

---

## 📚 Další zdroje

- [AG Grid Documentation](https://www.ag-grid.com/react-data-grid/)
- [ECharts Examples](https://echarts.apache.org/examples/en/index.html)
- [MUI Components](https://mui.com/material-ui/getting-started/)
- [Playwright Testing](https://playwright.dev/docs/intro)

---

## 🚀 Next Steps

Po dokončení PHASE 3 (npm install + routing):

### PHASE 4: Inline Edit & Bulk Operations API (16h)

Backend endpointy:
1. `EntityCrudController.java` - PATCH /api/entities/{entity}/{id}
2. `BulkUpdateController.java` - POST /api/entities/{entity}/bulk-update
3. Integration tests

Poté ExplorerGrid bude plně funkční s inline editing a bulk operations!
