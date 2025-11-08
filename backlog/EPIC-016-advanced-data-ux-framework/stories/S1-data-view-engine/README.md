# S1: Universal Data View Engine

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🔴 **P0 - CRITICAL**  
**Effort:** ~80 hours  
**Sprint:** 1-2  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Admin / Tenant Admin / Analyst,  
**chci** mít univerzální `<DataView>` component který funguje pro všechny entity (Users, Tenants, Workflows, Custom),  
**abych** mohl:
- Zobrazit data v různých režimech (Table, Chart, Pivot, Heatmap, Cards)
- Přepínat mezi view modes jedním klikem
- Použít stejný UX pro reporting i entity management

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Universal DataView Component
**GIVEN** generic `<DataView entity="Users">` component  
**WHEN** renderuji v aplikaci  
**THEN** automaticky:
- Načte Cube.js schema pro `Users` entitu
- Detekuje dostupné dimensions (name, email, status, tenantId)
- Detekuje dostupné measures (count, avgLastLogin)
- Zobrazí default table view s columns z schema

### AC2: View Mode Switcher
**GIVEN** DataView s načtenými daty  
**WHEN** kliknu na view mode switcher (icons: 📋 Table / 📊 Chart / 🔥 Heatmap / 🗂️ Pivot / 📇 Cards)  
**THEN** view se přepne:
- **Table**: MUI DataGrid (nebo EPIC-014 S9 table když bude ready)
- **Chart**: Bar chart (default), Recharts library
- **Heatmap**: 2D density visualization (Nivo Charts)
- **Pivot**: Kontingenční tabulka (react-pivottable)
- **Cards**: Grid of cards (responsive, 3-4 cols)

### AC3: Cube.js Schema Detection
**GIVEN** Cube.js schema pro `Users` entitu:
```javascript
cube(`Users`, {
  dimensions: {
    name: { type: 'string' },
    email: { type: 'string' },
    status: { type: 'string' },
    tenantId: { type: 'number' }
  },
  measures: {
    count: { type: 'count' },
    avgLastLogin: { type: 'avg', sql: `last_login_at` }
  }
})
```

**WHEN** DataView se mountne  
**THEN** volá `GET /api/cube/schema/Users` → vrátí schema JSON  
**AND** automaticky detekuje:
- Columns pro table: name, email, status, tenantId
- Chart options: count (bar chart), avgLastLogin (line chart)

### AC4: Responsive Design
**GIVEN** DataView otevřený na různých screen sizes  
**WHEN** změním viewport width  
**THEN** view se adaptuje:
- **Desktop (>1200px)**: Full table, multi-column charts
- **Tablet (768-1200px)**: Scrollable table, simplified charts
- **Mobile (<768px)**: Card view (table hidden), stacked charts

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: DataView Component Scaffold** (10h)

**Cíl:** Základní component struktura s prop types

**Implementation:**
```typescript
// frontend/src/components/data-ux/DataView.tsx
import React, { useState, useEffect } from 'react';
import { useDataView } from '@/hooks/useDataView';

export interface DataViewProps {
  entity: string;              // 'Users' | 'Tenants' | 'Workflows' | custom
  viewMode?: ViewMode;         // 'table' | 'chart' | 'pivot' | 'heatmap' | 'cards'
  filters?: Filter[];          // Optional pre-filters
  columns?: string[];          // Optional column override
  onRowClick?: (row: any) => void;  // Callback for detail popup
}

export type ViewMode = 'table' | 'chart' | 'pivot' | 'heatmap' | 'cards';

export const DataView: React.FC<DataViewProps> = ({
  entity,
  viewMode = 'table',
  filters = [],
  columns,
  onRowClick
}) => {
  const {
    data,
    schema,
    loading,
    error,
    refetch
  } = useDataView(entity, { filters, columns });

  if (loading) return <LoadingSkeleton />;
  if (error) return <ErrorBoundary error={error} />;

  return (
    <div className="data-view">
      <DataViewHeader entity={entity} />
      <ViewModeSwitcher
        currentMode={viewMode}
        onModeChange={handleModeChange}
      />
      <ViewRenderer
        mode={viewMode}
        data={data}
        schema={schema}
        onRowClick={onRowClick}
      />
    </div>
  );
};
```

**Deliverable:** Component scaffold s TypeScript interfaces

---

#### **T2: View Mode Switcher UI** (8h)

**Cíl:** Toggle buttons pro přepínání view modes

**Implementation:**
```typescript
// frontend/src/components/data-ux/ViewModeSwitcher.tsx
import React from 'react';
import { ToggleButtonGroup, ToggleButton } from '@mui/material';
import {
  TableChart,
  BarChart,
  GridOn,
  Heatmap,
  ViewModule
} from '@mui/icons-material';

interface ViewModeSwitcherProps {
  currentMode: ViewMode;
  onModeChange: (mode: ViewMode) => void;
  availableModes?: ViewMode[];  // Optional: some entities may not support all modes
}

export const ViewModeSwitcher: React.FC<ViewModeSwitcherProps> = ({
  currentMode,
  onModeChange,
  availableModes = ['table', 'chart', 'pivot', 'heatmap', 'cards']
}) => {
  return (
    <ToggleButtonGroup
      value={currentMode}
      exclusive
      onChange={(_, newMode) => newMode && onModeChange(newMode)}
      aria-label="view mode"
    >
      {availableModes.includes('table') && (
        <ToggleButton value="table" aria-label="table view">
          <TableChart /> Table
        </ToggleButton>
      )}
      {availableModes.includes('chart') && (
        <ToggleButton value="chart" aria-label="chart view">
          <BarChart /> Chart
        </ToggleButton>
      )}
      {availableModes.includes('pivot') && (
        <ToggleButton value="pivot" aria-label="pivot view">
          <GridOn /> Pivot
        </ToggleButton>
      )}
      {availableModes.includes('heatmap') && (
        <ToggleButton value="heatmap" aria-label="heatmap view">
          <Heatmap /> Heatmap
        </ToggleButton>
      )}
      {availableModes.includes('cards') && (
        <ToggleButton value="cards" aria-label="card view">
          <ViewModule /> Cards
        </ToggleButton>
      )}
    </ToggleButtonGroup>
  );
};
```

**Deliverable:** Toggle button group s ikonami

---

#### **T3: Table View Renderer** (15h)

**Cíl:** Integrace s EPIC-014 S9 table (nebo MUI DataGrid fallback)

**Implementation:**
```typescript
// frontend/src/components/data-ux/renderers/TableViewRenderer.tsx
import React from 'react';
import { DataGrid, GridColDef } from '@mui/x-data-grid';  // ⚠️ Temporary (replace in S11)

interface TableViewRendererProps {
  data: any[];
  schema: CubeSchema;
  onRowClick?: (row: any) => void;
}

export const TableViewRenderer: React.FC<TableViewRendererProps> = ({
  data,
  schema,
  onRowClick
}) => {
  // Auto-generate columns from schema
  const columns: GridColDef[] = schema.dimensions.map(dim => ({
    field: dim.name,
    headerName: dim.title || toTitleCase(dim.name),
    width: 150,
    type: getColumnType(dim.type)  // 'string' | 'number' | 'date'
  }));

  return (
    <DataGrid
      rows={data}
      columns={columns}
      pageSize={25}
      rowsPerPageOptions={[10, 25, 50, 100]}
      checkboxSelection
      disableSelectionOnClick
      onRowClick={(params) => onRowClick?.(params.row)}
      autoHeight
      density="comfortable"
    />
  );
};

function toTitleCase(str: string): string {
  return str.replace(/([A-Z])/g, ' $1').trim()
    .split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
}

function getColumnType(cubeType: string): 'string' | 'number' | 'date' {
  if (cubeType === 'time') return 'date';
  if (cubeType === 'number') return 'number';
  return 'string';
}
```

**Deliverable:** Table renderer s auto-generated columns

---

#### **T4: Chart View Renderer** (12h)

**Cíl:** Bar chart (default), integrace Recharts

**Implementation:**
```typescript
// frontend/src/components/data-ux/renderers/ChartViewRenderer.tsx
import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

interface ChartViewRendererProps {
  data: any[];
  schema: CubeSchema;
  chartType?: 'bar' | 'line' | 'pie' | 'area';  // Default: bar
}

export const ChartViewRenderer: React.FC<ChartViewRendererProps> = ({
  data,
  schema,
  chartType = 'bar'
}) => {
  // Auto-detect X axis (first dimension) and Y axis (first measure)
  const xField = schema.dimensions[0]?.name || 'name';
  const yField = schema.measures[0]?.name || 'count';

  return (
    <ResponsiveContainer width="100%" height={400}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey={xField} />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey={yField} fill="#8884d8" />
      </BarChart>
    </ResponsiveContainer>
  );
};
```

**Deliverable:** Basic bar chart renderer

---

#### **T5: Pivot View Renderer** (20h)

**Cíl:** Kontingenční tabulka (cross-tabulation)

**Implementation:**
```typescript
// frontend/src/components/data-ux/renderers/PivotViewRenderer.tsx
import React from 'react';
import PivotTableUI from 'react-pivottable/PivotTableUI';
import 'react-pivottable/pivottable.css';

interface PivotViewRendererProps {
  data: any[];
  schema: CubeSchema;
}

export const PivotViewRenderer: React.FC<PivotViewRendererProps> = ({
  data,
  schema
}) => {
  const [pivotState, setPivotState] = React.useState({});

  return (
    <PivotTableUI
      data={data}
      onChange={setPivotState}
      {...pivotState}
      // Default config: rows = first dimension, cols = second dimension
      rows={[schema.dimensions[0]?.name]}
      cols={[schema.dimensions[1]?.name]}
      vals={[schema.measures[0]?.name]}
      aggregatorName="Sum"
    />
  );
};
```

**Deliverable:** Interactive pivot table

---

#### **T6: Heatmap View Renderer** (10h)

**Cíl:** 2D density visualization (Nivo Charts)

**Implementation:**
```typescript
// frontend/src/components/data-ux/renderers/HeatmapViewRenderer.tsx
import React from 'react';
import { ResponsiveHeatMap } from '@nivo/heatmap';

interface HeatmapViewRendererProps {
  data: any[];
  schema: CubeSchema;
}

export const HeatmapViewRenderer: React.FC<HeatmapViewRendererProps> = ({
  data,
  schema
}) => {
  // Transform data to Nivo heatmap format
  // Example: Workflows by Day of Week × Hour
  const heatmapData = transformToHeatmapFormat(data, schema);

  return (
    <ResponsiveHeatMap
      data={heatmapData}
      margin={{ top: 60, right: 90, bottom: 60, left: 90 }}
      valueFormat=">-.2s"
      axisTop={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: -90,
        legend: '',
        legendOffset: 46
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: 'Day of Week',
        legendPosition: 'middle',
        legendOffset: -72
      }}
      colors={{
        type: 'diverging',
        scheme: 'red_yellow_blue',
        divergeAt: 0.5
      }}
      emptyColor="#555555"
      legends={[
        {
          anchor: 'bottom',
          translateX: 0,
          translateY: 30,
          length: 400,
          thickness: 8,
          direction: 'row',
          tickPosition: 'after',
          tickSize: 3,
          tickSpacing: 4,
          tickOverlap: false,
          title: 'Value →',
          titleAlign: 'start',
          titleOffset: 4
        }
      ]}
    />
  );
};

function transformToHeatmapFormat(data: any[], schema: CubeSchema) {
  // Implementation depends on data structure
  // Example: Group by 2 dimensions, aggregate measure
  return data;  // TODO: Transform logic
}
```

**Deliverable:** Heatmap visualization

---

#### **T7: Card View Renderer** (5h)

**Cíl:** Responsive grid of cards (mobile-friendly)

**Implementation:**
```typescript
// frontend/src/components/data-ux/renderers/CardViewRenderer.tsx
import React from 'react';
import { Grid, Card, CardContent, Typography } from '@mui/material';

interface CardViewRendererProps {
  data: any[];
  schema: CubeSchema;
  onCardClick?: (item: any) => void;
}

export const CardViewRenderer: React.FC<CardViewRendererProps> = ({
  data,
  schema,
  onCardClick
}) => {
  // Auto-select primary field (first dimension) for card title
  const titleField = schema.dimensions[0]?.name || 'name';
  const subtitleField = schema.dimensions[1]?.name || 'email';

  return (
    <Grid container spacing={2}>
      {data.map((item, index) => (
        <Grid item xs={12} sm={6} md={4} lg={3} key={index}>
          <Card
            onClick={() => onCardClick?.(item)}
            sx={{ cursor: onCardClick ? 'pointer' : 'default' }}
          >
            <CardContent>
              <Typography variant="h6">{item[titleField]}</Typography>
              <Typography variant="body2" color="text.secondary">
                {item[subtitleField]}
              </Typography>
              {/* Display other fields */}
              {schema.dimensions.slice(2, 5).map(dim => (
                <Typography key={dim.name} variant="caption" display="block">
                  {dim.title}: {item[dim.name]}
                </Typography>
              ))}
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};
```

**Deliverable:** Card grid responsive layout

---

### Backend API

#### **GET /api/cube/schema/:entity**

**Purpose:** Vrátí Cube.js schema pro entitu

**Request:**
```http
GET /api/cube/schema/Users
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "entity": "Users",
  "dimensions": [
    {"name": "id", "type": "number", "title": "ID", "primaryKey": true},
    {"name": "name", "type": "string", "title": "Name"},
    {"name": "email", "type": "string", "title": "Email"},
    {"name": "status", "type": "string", "title": "Status"},
    {"name": "tenantId", "type": "number", "title": "Tenant ID"},
    {"name": "createdAt", "type": "time", "title": "Created At"}
  ],
  "measures": [
    {"name": "count", "type": "count", "title": "Count"},
    {"name": "avgLastLogin", "type": "avg", "title": "Avg Last Login"}
  ],
  "segments": [
    {"name": "activeUsers", "title": "Active Users", "filter": "status = 'ACTIVE'"}
  ]
}
```

**Implementation:**
```java
// backend/src/main/java/cz/muriel/core/dataview/CubeSchemaController.java
@RestController
@RequestMapping("/api/cube/schema")
@RequiredArgsConstructor
public class CubeSchemaController {

    private final CubeJsClient cubeClient;

    @GetMapping("/{entity}")
    public CubeSchemaResponse getSchema(@PathVariable String entity) {
        // Call Cube.js /meta endpoint
        Map<String, Object> meta = cubeClient.getMeta();
        Map<String, Object> cubeMeta = (Map) meta.get(entity);

        return CubeSchemaResponse.builder()
            .entity(entity)
            .dimensions(extractDimensions(cubeMeta))
            .measures(extractMeasures(cubeMeta))
            .segments(extractSegments(cubeMeta))
            .build();
    }
}
```

---

## 🧪 TESTING

### Unit Tests

```typescript
// frontend/src/components/data-ux/__tests__/DataView.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { DataView } from '../DataView';

describe('DataView', () => {
  it('renders table view by default', () => {
    render(<DataView entity="Users" />);
    expect(screen.getByRole('grid')).toBeInTheDocument();
  });

  it('switches to chart view when mode changes', () => {
    render(<DataView entity="Users" />);
    fireEvent.click(screen.getByLabelText('chart view'));
    expect(screen.getByRole('img')).toBeInTheDocument();  // Recharts SVG
  });

  it('loads schema from API', async () => {
    render(<DataView entity="Users" />);
    await screen.findByText('Name');  // Column header
    expect(screen.getByText('Email')).toBeInTheDocument();
  });
});
```

### E2E Tests

```typescript
// e2e/specs/data-ux/data-view.spec.ts
import { test, expect } from '@playwright/test';

test('User can switch between table and chart view', async ({ page }) => {
  await page.goto('/users');

  // Default: Table view
  await expect(page.locator('role=grid')).toBeVisible();

  // Switch to chart
  await page.click('button:has-text("Chart")');
  await expect(page.locator('.recharts-wrapper')).toBeVisible();

  // Switch to pivot
  await page.click('button:has-text("Pivot")');
  await expect(page.locator('.pvtTable')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ DataView funguje pro 5+ entit (Users, Tenants, Workflows, Roles, Audits)
- ✅ View mode switch < 500ms
- ✅ Table renderuje 1000 rows < 2 seconds
- ✅ Chart renderuje < 1 second
- ✅ Mobile responsive (card view na mobilu)

---

## 🔗 DEPENDENCIES

- **EPIC-004 S1:** Cube.js schemas ✅ DONE
- **EPIC-014 S9:** Table component ⏳ TODO (fallback: MUI DataGrid)
- **Libraries:** Recharts, Nivo Charts, react-pivottable

---

## 📚 DOCUMENTATION

- [ ] User Guide: How to use Data Views (table, chart, pivot modes)
- [ ] Developer Guide: How to add new entity to DataView system
- [ ] API Doc: GET /api/cube/schema/:entity

---

**Status:** 📋 TODO → Ready for implementation  
**Next:** S2: Advanced Filtering & Search
