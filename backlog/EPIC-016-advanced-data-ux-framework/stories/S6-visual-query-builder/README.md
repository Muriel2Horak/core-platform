# S6: Visual Query Builder

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~65 hours  
**Sprint:** 3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Business Analyst / Power User bez SQL znalostí,  
**chci** visual drag-and-drop query builder pro vytváření custom reportů,  
**abych**:
- Vytvořil query "All users from Tenant XYZ who logged in last 7 days" bez SQL
- Viděl preview results před uložením reportu
- Sdílel query s kolegy jako saved report
- Editoval existing queries vizuálně (ne v JSON/SQL)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Schema Introspection (Cube.js Integration)

**GIVEN** Cube.js schemas z EPIC-004 S1  
**WHEN** otevřu Query Builder  
**THEN** zobrazí se:
- **Available Data Sources**: Users, Tenants, Workflows, Orders (dropdown)
- **Dimensions** (fields pro grouping): email, role, tenantName, createdAt
- **Measures** (aggregace): count, sum, avg, min, max
- **Time Dimensions**: createdAt, updatedAt (s granularity: day, week, month)

**AND** schema loading:

```http
GET /api/cube/meta
→ {
  "cubes": {
    "Users": {
      "dimensions": {
        "email": {"type": "string"},
        "role": {"type": "string"},
        "tenantName": {"type": "string"}
      },
      "measures": {
        "count": {"type": "number"}
      },
      "timeDimensions": {
        "createdAt": {"type": "time"}
      }
    }
  }
}
```

### AC2: Drag & Drop Query Construction

**GIVEN** schema loaded  
**WHEN** drag "tenantName" dimension na "Group By" zone  
**THEN**:
- Zobrazí se panel "Group By: tenantName"
- Query builder state:

```typescript
{
  "measures": [],
  "dimensions": ["Users.tenantName"],
  "timeDimensions": [],
  "filters": []
}
```

**WHEN** drag "count" measure na "Measures" zone  
**THEN** query aktualizován:

```typescript
{
  "measures": ["Users.count"],
  "dimensions": ["Users.tenantName"],
  // ...
}
```

**AND** drag zones:
- **Measures** (max 5 metrics)
- **Dimensions** (Group By, max 3)
- **Time Dimensions** (Date Range, 1 time field)
- **Filters** (WHERE conditions, unlimited)

### AC3: Filter Builder UI

**GIVEN** query s "Users" data source  
**WHEN** click "Add Filter"  
**THEN** zobrazí se filter row:
- **Field dropdown**: email, role, tenantName, createdAt
- **Operator dropdown**: equals, contains, greater than, less than, between, in
- **Value input**: text field / date picker / multi-select

**Example filters:**

```typescript
// Filter 1: role equals ADMIN
{
  "member": "Users.role",
  "operator": "equals",
  "values": ["ADMIN"]
}

// Filter 2: createdAt in last 7 days
{
  "member": "Users.createdAt",
  "operator": "inDateRange",
  "values": ["last 7 days"]
}

// Filter 3: email contains "@example.com"
{
  "member": "Users.email",
  "operator": "contains",
  "values": ["@example.com"]
}
```

**AND** filter composition:
- **AND/OR toggle**: Combine filters with logical operators
- **Filter groups**: Nested filters (advanced users)

### AC4: Live Preview Results

**GIVEN** query constructed:

```typescript
{
  "measures": ["Users.count"],
  "dimensions": ["Users.tenantName"],
  "filters": [
    {"member": "Users.role", "operator": "equals", "values": ["ADMIN"]}
  ]
}
```

**WHEN** click "Preview"  
**THEN**:
- Loading state (skeleton table)
- Cube.js API call:

```http
POST /api/cube/v1/load
{
  "query": { /* ... */ }
}
```

- Results zobrazeny v table:

| Tenant Name | User Count |
|-------------|------------|
| Acme Corp   | 12         |
| TechStart   | 5          |
| **Total**   | **17**     |

**AND** preview controls:
- **Limit rows**: 10, 50, 100 (default 10)
- **Export preview**: CSV / Excel

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Schema Introspection API** (12h)

**Implementation:**

```typescript
// frontend/src/api/cubeSchema.ts
import { useCubeQuery } from '@/hooks/useCubeQuery';

export interface CubeSchema {
  cubes: Record<string, CubeDefinition>;
}

interface CubeDefinition {
  name: string;
  title: string;
  dimensions: Record<string, DimensionMeta>;
  measures: Record<string, MeasureMeta>;
  timeDimensions?: Record<string, TimeDimensionMeta>;
}

interface DimensionMeta {
  type: 'string' | 'number' | 'boolean';
  title?: string;
}

interface MeasureMeta {
  type: 'number';
  aggType: 'count' | 'sum' | 'avg' | 'min' | 'max';
  title?: string;
}

interface TimeDimensionMeta {
  type: 'time';
  title?: string;
}

export const useCubeSchema = () => {
  const [schema, setSchema] = useState<CubeSchema | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchSchema = async () => {
      try {
        const response = await fetch('/api/cube/meta');
        const data = await response.json();
        setSchema(data);
      } catch (error) {
        console.error('Failed to load Cube.js schema:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchSchema();
  }, []);

  return { schema, loading };
};
```

**Deliverable:** Schema loading hook

---

#### **T2: Drag & Drop Query UI** (20h)

**Implementation:**

```typescript
// frontend/src/components/query-builder/QueryBuilder.tsx
import React, { useState } from 'react';
import { DndContext, DragEndEvent, useDraggable, useDroppable } from '@dnd-kit/core';
import { Box, Paper, Typography, Chip } from '@mui/material';

interface QueryState {
  measures: string[];
  dimensions: string[];
  timeDimensions: Array<{
    dimension: string;
    granularity: 'day' | 'week' | 'month';
    dateRange?: string[];
  }>;
  filters: Filter[];
}

export const QueryBuilder: React.FC = () => {
  const { schema } = useCubeSchema();
  const [query, setQuery] = useState<QueryState>({
    measures: [],
    dimensions: [],
    timeDimensions: [],
    filters: []
  });

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;

    const fieldName = active.id as string;
    const zone = over.id as keyof QueryState;

    // Add field to appropriate zone
    if (zone === 'measures' && !query.measures.includes(fieldName)) {
      setQuery(prev => ({
        ...prev,
        measures: [...prev.measures, fieldName]
      }));
    } else if (zone === 'dimensions' && !query.dimensions.includes(fieldName)) {
      setQuery(prev => ({
        ...prev,
        dimensions: [...prev.dimensions, fieldName]
      }));
    }
  };

  return (
    <DndContext onDragEnd={handleDragEnd}>
      <Box sx={{ display: 'flex', gap: 2 }}>
        {/* Left sidebar: Available fields */}
        <Paper sx={{ width: 250, p: 2 }}>
          <Typography variant="h6">Available Fields</Typography>
          {schema && (
            <>
              <Typography variant="subtitle2" sx={{ mt: 2 }}>Measures</Typography>
              {Object.entries(schema.cubes.Users.measures).map(([key, measure]) => (
                <DraggableField key={key} id={`Users.${key}`} label={measure.title || key} />
              ))}

              <Typography variant="subtitle2" sx={{ mt: 2 }}>Dimensions</Typography>
              {Object.entries(schema.cubes.Users.dimensions).map(([key, dim]) => (
                <DraggableField key={key} id={`Users.${key}`} label={dim.title || key} />
              ))}
            </>
          )}
        </Paper>

        {/* Right area: Drop zones */}
        <Box sx={{ flex: 1 }}>
          <DropZone id="measures" label="Measures" items={query.measures} onRemove={(item) => {
            setQuery(prev => ({
              ...prev,
              measures: prev.measures.filter(m => m !== item)
            }));
          }} />

          <DropZone id="dimensions" label="Group By" items={query.dimensions} onRemove={(item) => {
            setQuery(prev => ({
              ...prev,
              dimensions: prev.dimensions.filter(d => d !== item)
            }));
          }} />

          <Box sx={{ mt: 2 }}>
            <Typography variant="h6">Filters</Typography>
            <FilterBuilder
              filters={query.filters}
              schema={schema}
              onChange={(filters) => setQuery(prev => ({ ...prev, filters }))}
            />
          </Box>

          <Box sx={{ mt: 3 }}>
            <Button variant="contained" onClick={() => handlePreview(query)}>
              Preview Results
            </Button>
          </Box>
        </Box>
      </Box>
    </DndContext>
  );
};

const DraggableField: React.FC<{ id: string; label: string }> = ({ id, label }) => {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({ id });

  return (
    <Chip
      ref={setNodeRef}
      label={label}
      {...listeners}
      {...attributes}
      sx={{
        cursor: 'grab',
        m: 0.5,
        transform: transform ? `translate3d(${transform.x}px, ${transform.y}px, 0)` : undefined
      }}
    />
  );
};

const DropZone: React.FC<{
  id: string;
  label: string;
  items: string[];
  onRemove: (item: string) => void;
}> = ({ id, label, items, onRemove }) => {
  const { setNodeRef } = useDroppable({ id });

  return (
    <Paper
      ref={setNodeRef}
      sx={{
        p: 2,
        minHeight: 100,
        backgroundColor: items.length > 0 ? 'action.hover' : 'background.paper'
      }}
    >
      <Typography variant="subtitle2">{label}</Typography>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
        {items.map(item => (
          <Chip
            key={item}
            label={item.split('.')[1]}
            onDelete={() => onRemove(item)}
          />
        ))}
      </Box>
    </Paper>
  );
};
```

**Deliverable:** Drag & drop query construction UI

---

#### **T3: Filter Builder Component** (15h)

**Implementation:**

```typescript
// frontend/src/components/query-builder/FilterBuilder.tsx
import React from 'react';
import { Box, Button, Select, MenuItem, TextField, IconButton } from '@mui/material';
import { Delete, Add } from '@mui/icons-material';

interface Filter {
  id: string;
  member: string;
  operator: string;
  values: string[];
}

interface FilterBuilderProps {
  filters: Filter[];
  schema: CubeSchema;
  onChange: (filters: Filter[]) => void;
}

export const FilterBuilder: React.FC<FilterBuilderProps> = ({ filters, schema, onChange }) => {
  const addFilter = () => {
    const newFilter: Filter = {
      id: `filter-${Date.now()}`,
      member: '',
      operator: 'equals',
      values: []
    };
    onChange([...filters, newFilter]);
  };

  const updateFilter = (id: string, updates: Partial<Filter>) => {
    onChange(filters.map(f => f.id === id ? { ...f, ...updates } : f));
  };

  const removeFilter = (id: string) => {
    onChange(filters.filter(f => f.id !== id));
  };

  const getOperatorsForField = (member: string): string[] => {
    if (!member) return [];
    const [cube, field] = member.split('.');
    const fieldMeta = schema?.cubes[cube]?.dimensions[field];

    if (fieldMeta?.type === 'string') {
      return ['equals', 'notEquals', 'contains', 'notContains', 'startsWith', 'endsWith'];
    } else if (fieldMeta?.type === 'number') {
      return ['equals', 'notEquals', 'gt', 'gte', 'lt', 'lte', 'between'];
    } else if (fieldMeta?.type === 'time') {
      return ['inDateRange', 'notInDateRange', 'beforeDate', 'afterDate'];
    }
    return ['equals'];
  };

  return (
    <Box>
      {filters.map(filter => (
        <Box key={filter.id} sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'center' }}>
          {/* Field dropdown */}
          <Select
            value={filter.member}
            onChange={(e) => updateFilter(filter.id, { member: e.target.value })}
            displayEmpty
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="">Select field...</MenuItem>
            {schema && Object.entries(schema.cubes.Users.dimensions).map(([key, dim]) => (
              <MenuItem key={key} value={`Users.${key}`}>
                {dim.title || key}
              </MenuItem>
            ))}
          </Select>

          {/* Operator dropdown */}
          <Select
            value={filter.operator}
            onChange={(e) => updateFilter(filter.id, { operator: e.target.value })}
            sx={{ minWidth: 150 }}
          >
            {getOperatorsForField(filter.member).map(op => (
              <MenuItem key={op} value={op}>{op}</MenuItem>
            ))}
          </Select>

          {/* Value input */}
          <TextField
            value={filter.values[0] || ''}
            onChange={(e) => updateFilter(filter.id, { values: [e.target.value] })}
            placeholder="Value"
            sx={{ flex: 1 }}
          />

          {/* Remove button */}
          <IconButton onClick={() => removeFilter(filter.id)} size="small">
            <Delete />
          </IconButton>
        </Box>
      ))}

      <Button startIcon={<Add />} onClick={addFilter} sx={{ mt: 1 }}>
        Add Filter
      </Button>
    </Box>
  );
};
```

**Deliverable:** Filter builder with operators

---

#### **T4: Live Preview Component** (12h)

**Implementation:**

```typescript
// frontend/src/components/query-builder/QueryPreview.tsx
import React, { useState } from 'react';
import { useCubeQuery } from '@cubejs-client/react';
import { DataGrid } from '@mui/x-data-grid';
import { Box, Button, CircularProgress, Alert } from '@mui/material';

interface QueryPreviewProps {
  query: QueryState;
}

export const QueryPreview: React.FC<QueryPreviewProps> = ({ query }) => {
  const [limit, setLimit] = useState(10);
  const { resultSet, isLoading, error } = useCubeQuery(query, { skip: !query.measures.length });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">Query failed: {error.toString()}</Alert>;
  }

  if (!resultSet) {
    return <Alert severity="info">Configure query and click Preview</Alert>;
  }

  const columns = resultSet.tableColumns().map(col => ({
    field: col.key,
    headerName: col.title,
    width: 200
  }));

  const rows = resultSet.tablePivot().slice(0, limit).map((row, index) => ({
    id: index,
    ...row
  }));

  return (
    <Box sx={{ height: 400, mt: 2 }}>
      <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
        <Button size="small" onClick={() => setLimit(10)}>10 rows</Button>
        <Button size="small" onClick={() => setLimit(50)}>50 rows</Button>
        <Button size="small" onClick={() => setLimit(100)}>100 rows</Button>
      </Box>
      <DataGrid columns={columns} rows={rows} />
    </Box>
  );
};
```

**Deliverable:** Live preview table

---

#### **T5: Save & Share Query** (6h)

**Implementation:**

```typescript
// Save query as report
const saveQuery = async (query: QueryState, name: string) => {
  await fetch('/api/reports', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name,
      query,
      createdBy: user.id
    })
  });
};

// Share report with users
const shareReport = async (reportId: string, userIds: number[]) => {
  await fetch(`/api/reports/${reportId}/share`, {
    method: 'POST',
    body: JSON.stringify({ userIds })
  });
};
```

---

## 🧪 TESTING

```typescript
// e2e/specs/query-builder/visual-query.spec.ts
test('Build query with drag & drop', async ({ page }) => {
  await page.goto('/query-builder');

  // Drag "count" measure to Measures zone
  await page.dragAndDrop('[data-field="Users.count"]', '[data-zone="measures"]');

  // Verify measure added
  await expect(page.locator('[data-zone="measures"] >> text=count')).toBeVisible();

  // Preview results
  await page.click('button:has-text("Preview")');
  await expect(page.locator('.data-grid')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Query construction < 2min (first-time user)
- ✅ Preview results < 1s
- ✅ 70% users prefer visual builder over SQL

---

## 🔗 DEPENDENCIES

- **EPIC-004 S1:** Cube.js schemas
- **@dnd-kit/core:** Drag & drop library

---

**Status:** 📋 TODO  
**Next:** S7: Extended Widget Types

