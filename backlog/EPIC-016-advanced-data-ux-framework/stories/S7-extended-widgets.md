# S7: Extended Widget Types

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~150 hours  
**Sprint:** 6-8  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Dashboard Designer / Data Analyst,  
**chci** extended widget types (Heatmap, Sankey, Treemap, Network Graph, KPI Tiles),  
**abych**:
- Viděl **Heatmap** workflow activity (hour × day)
- Analyzoval **Sankey** user journey (Login → Create Workflow → Submit)
- Viděl **Treemap** tenant sizes (hierarchical space usage)
- Mapoval **Network Graph** relationships (user → tenant → workflow connections)
- Trackoval **KPI Tiles** big numbers s trendy (Revenue: €2.5M ↑ 12% vs. last week)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Heatmap Widget (@nivo/heatmap)

**GIVEN** workflow activity data (hour × day)  
**WHEN** přidám Heatmap widget  
**THEN** zobrazí se 2D density map:

```
Workflow Activity Heatmap (Last 7 Days)

        Mon   Tue   Wed   Thu   Fri   Sat   Sun
00:00   🟦    🟦    🟦    🟦    🟦    🟦    🟦   (light blue = low)
06:00   🟨    🟨    🟨    🟨    🟨    🟦    🟦   (yellow = medium)
12:00   🟥    🟥    🟥    🟥    🟥    🟨    🟨   (red = high)
18:00   🟨    🟨    🟨    🟨    🟨    🟦    🟦
```

**Configuration:**
- X-axis: Day of week / Date range
- Y-axis: Hour of day / Custom dimension
- Color scale: Sequential (Blues), Diverging (RdYlGn), Custom
- Tooltip: "Wednesday 12:00-13:00: 145 workflows created"

**Data format:**

```typescript
interface HeatmapData {
  id: string; // Row label (e.g., "12:00")
  data: Array<{ x: string; y: number }>; // Column values
}

// Example:
[
  {
    id: "00:00",
    data: [
      { x: "Mon", y: 12 },
      { x: "Tue", y: 8 },
      // ...
    ]
  }
]
```

### AC2: Sankey Diagram (@nivo/sankey)

**GIVEN** user journey data (Login → Create Workflow → Submit → Approve)  
**WHEN** přidám Sankey widget  
**THEN** zobrazí se flow diagram:

```
       Login (1,000) ──────────┐
                               │
                               ├─→ Create Workflow (850) ──┐
                               │                           │
                               └─→ Exit (150)              ├─→ Submit (700) ──┐
                                                           │                  │
                                                           └─→ Draft (150)    ├─→ Approve (600)
                                                                              │
                                                                              └─→ Reject (100)
```

**Configuration:**
- Node alignment: Justify, Center, Left, Right
- Link sorting: Ascending, Descending
- Colors: Node colors by category, link gradient
- Tooltip: "Login → Create Workflow: 850 users (85%)"

**Data format:**

```typescript
interface SankeyData {
  nodes: Array<{ id: string; nodeColor?: string }>;
  links: Array<{
    source: string; // Node ID
    target: string;
    value: number;
  }>;
}

// Example:
{
  nodes: [
    { id: "Login" },
    { id: "Create Workflow" },
    { id: "Submit" }
  ],
  links: [
    { source: "Login", target: "Create Workflow", value: 850 },
    { source: "Create Workflow", target: "Submit", value: 700 }
  ]
}
```

### AC3: Treemap Widget (@nivo/treemap)

**GIVEN** tenant storage usage (hierarchical data)  
**WHEN** přidám Treemap widget  
**THEN** zobrazí se hierarchical tile map:

```
┌────────────────────────────────────────────────────┐
│ Company A (5.2 TB)                                 │
│ ┌─────────────────┬──────────────┬──────────────┐ │
│ │ Dept Engineering│ Dept Sales   │ Dept HR      │ │
│ │ (2.8 TB)        │ (1.5 TB)     │ (0.9 TB)     │ │
│ └─────────────────┴──────────────┴──────────────┘ │
│                                                    │
│ Company B (3.1 TB)                                 │
│ ┌───────────────────────────┬───────────────────┐ │
│ │ Dept Marketing (1.8 TB)   │ Dept Finance (1.3)│ │
│ └───────────────────────────┴───────────────────┘ │
└────────────────────────────────────────────────────┘
```

**Configuration:**
- Tiling: Binary, Squarify, Slice, Dice, Slice-Dice
- Colors: By value (heat map), by category
- Label: Show name, value, percentage
- Tooltip: "Engineering Dept: 2.8 TB (54% of Company A)"

**Data format:**

```typescript
interface TreemapNode {
  name: string;
  value?: number; // Leaf node
  children?: TreemapNode[]; // Branch node
}

// Example:
{
  name: "Root",
  children: [
    {
      name: "Company A",
      children: [
        { name: "Engineering", value: 2800 },
        { name: "Sales", value: 1500 }
      ]
    }
  ]
}
```

### AC4: Network Graph Widget (D3.js Force-Directed)

**GIVEN** entity relationships (user → tenant → workflow)  
**WHEN** přidám Network Graph widget  
**THEN** zobrazí se interactive node-link diagram:

```
     ┌─────────┐
     │ User A  │──────┐
     └─────────┘      │
                      ▼
     ┌─────────┐  ┌───────────┐  ┌────────────┐
     │ User B  │──│ Tenant X  │──│ Workflow 1 │
     └─────────┘  └───────────┘  └────────────┘
                      │
     ┌─────────┐      │
     │ User C  │──────┘
     └─────────┘
```

**Interactions:**
- **Drag nodes**: Reposition in force simulation
- **Zoom & Pan**: Mouse wheel zoom, drag canvas
- **Click node**: Highlight connected nodes
- **Hover link**: Show relationship type & strength

**Configuration:**
- Force simulation: Charge strength, link distance, collision radius
- Node size: By degree (connections), by value
- Link width: By weight/strength
- Colors: By node type (users = blue, tenants = green, workflows = orange)

**Data format:**

```typescript
interface NetworkData {
  nodes: Array<{
    id: string;
    group?: string; // Node type (user, tenant, workflow)
    value?: number; // Size
  }>;
  links: Array<{
    source: string; // Node ID
    target: string;
    value?: number; // Link strength
  }>;
}
```

### AC5: KPI Tile Widget

**GIVEN** metric data (revenue, users, workflows)  
**WHEN** přidám KPI Tile widget  
**THEN** zobrazí se big number display:

```
┌──────────────────────────┐
│ Monthly Revenue          │
│                          │
│       €2.5M              │  ← Big number
│       ↑ 12%              │  ← Trend indicator (green arrow)
│       vs. last month     │
│                          │
│ ▁▂▃▅▆▇█                  │  ← Sparkline (last 7 days)
└──────────────────────────┘
```

**Trend calculation:**

```typescript
interface KPIData {
  current: number;
  previous: number;
  trend: 'up' | 'down' | 'neutral';
  trendPercent: number;
  sparklineData: number[]; // Last N values
}

// Example:
{
  current: 2500000,
  previous: 2230000,
  trend: 'up',
  trendPercent: 12.1, // (2.5M - 2.23M) / 2.23M * 100
  sparklineData: [2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.5]
}
```

**Configuration:**
- Number format: Currency (€, $, £), Percentage (%), Integer, Decimal
- Trend comparison: vs. last week, vs. last month, vs. last year
- Sparkline: Show/hide, line/bar, color
- Threshold alerts: Green > 10%, Yellow 0-10%, Red < 0%

### AC6: Widget Registry & Configuration

**GIVEN** 10+ widget types  
**WHEN** developer přidá nový widget  
**THEN** widget se automaticky objeví v widget palette:

```typescript
// Widget registry pattern
interface WidgetType {
  id: string;
  name: string;
  icon: string;
  category: 'chart' | 'kpi' | 'table' | 'advanced';
  configSchema: object; // Zod schema for widget config
  component: React.ComponentType<any>;
}

const WIDGET_REGISTRY: WidgetType[] = [
  {
    id: 'heatmap',
    name: 'Heatmap',
    icon: 'GridOnIcon',
    category: 'advanced',
    configSchema: z.object({
      xAxis: z.string(),
      yAxis: z.string(),
      colorScale: z.enum(['Blues', 'Reds', 'RdYlGn'])
    }),
    component: HeatmapWidget
  },
  // ... other widgets
];
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Heatmap Widget Component** (25h)

**Implementation:**

```typescript
// frontend/src/components/widgets/HeatmapWidget.tsx
import React from 'react';
import { ResponsiveHeatMap } from '@nivo/heatmap';

interface HeatmapWidgetProps {
  data: Array<{ id: string; data: Array<{ x: string; y: number }> }>;
  config: {
    xAxis: string;
    yAxis: string;
    colorScale: 'Blues' | 'Reds' | 'RdYlGn';
  };
}

export const HeatmapWidget: React.FC<HeatmapWidgetProps> = ({ data, config }) => {
  return (
    <ResponsiveHeatMap
      data={data}
      margin={{ top: 60, right: 90, bottom: 60, left: 90 }}
      valueFormat=">-.0f"
      axisTop={{
        tickSize: 5,
        tickPadding: 5,
        legend: config.xAxis,
        legendOffset: 46
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        legend: config.yAxis,
        legendOffset: -72
      }}
      colors={{
        type: 'sequential',
        scheme: config.colorScale.toLowerCase() as any
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
```

**Data transformation example:**

```typescript
// Transform Cube.js result to Heatmap data
const transformToHeatmap = (cubeData: any[]) => {
  const hours = [...Array(24)].map((_, i) => `${i}:00`);
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  return hours.map(hour => ({
    id: hour,
    data: days.map(day => {
      const value = cubeData.find(
        d => d['WorkflowActivity.hour'] === hour && d['WorkflowActivity.day'] === day
      )?.['WorkflowActivity.count'] || 0;
      return { x: day, y: value };
    })
  }));
};
```

**Deliverable:** Heatmap widget component

---

#### **T2: Sankey Diagram Component** (30h)

**Implementation:**

```typescript
// frontend/src/components/widgets/SankeyWidget.tsx
import React from 'react';
import { ResponsiveSankey } from '@nivo/sankey';

interface SankeyWidgetProps {
  data: {
    nodes: Array<{ id: string; nodeColor?: string }>;
    links: Array<{ source: string; target: string; value: number }>;
  };
  config: {
    align: 'justify' | 'center' | 'start' | 'end';
    sort: 'auto' | 'input' | 'ascending' | 'descending';
  };
}

export const SankeyWidget: React.FC<SankeyWidgetProps> = ({ data, config }) => {
  return (
    <ResponsiveSankey
      data={data}
      margin={{ top: 40, right: 160, bottom: 40, left: 50 }}
      align={config.align}
      sort={config.sort}
      colors={{ scheme: 'category10' }}
      nodeOpacity={1}
      nodeHoverOthersOpacity={0.35}
      nodeThickness={18}
      nodeSpacing={24}
      nodeBorderWidth={0}
      linkOpacity={0.5}
      linkHoverOthersOpacity={0.1}
      linkContract={3}
      enableLinkGradient
      labelPosition="outside"
      labelOrientation="horizontal"
      labelPadding={16}
    />
  );
};
```

**Deliverable:** Sankey diagram component

---

#### **T3: Treemap Widget Component** (20h)

**Implementation:**

```typescript
// frontend/src/components/widgets/TreemapWidget.tsx
import React from 'react';
import { ResponsiveTreeMap } from '@nivo/treemap';

interface TreemapWidgetProps {
  data: {
    name: string;
    children?: TreemapNode[];
    value?: number;
  };
  config: {
    tile: 'binary' | 'squarify' | 'slice' | 'dice' | 'sliceDice';
    colorBy: 'value' | 'depth' | 'id';
  };
}

export const TreemapWidget: React.FC<TreemapWidgetProps> = ({ data, config }) => {
  return (
    <ResponsiveTreeMap
      data={data}
      identity="name"
      value="value"
      tile={config.tile}
      leavesOnly
      innerPadding={3}
      outerPadding={3}
      margin={{ top: 10, right: 10, bottom: 10, left: 10 }}
      label={(node) => `${node.id} (${node.formattedValue})`}
      labelSkipSize={12}
      colors={{ scheme: 'nivo' }}
      colorBy={config.colorBy}
      borderColor={{ from: 'color', modifiers: [['darker', 0.3]] }}
    />
  );
};
```

**Deliverable:** Treemap widget component

---

#### **T4: Network Graph Component** (40h)

**Implementation:**

```typescript
// frontend/src/components/widgets/NetworkGraphWidget.tsx
import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';

interface NetworkGraphWidgetProps {
  data: {
    nodes: Array<{ id: string; group?: string; value?: number }>;
    links: Array<{ source: string; target: string; value?: number }>;
  };
  config: {
    chargeStrength: number;
    linkDistance: number;
    nodeSize: 'degree' | 'value';
  };
}

export const NetworkGraphWidget: React.FC<NetworkGraphWidgetProps> = ({ data, config }) => {
  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    if (!svgRef.current) return;

    const svg = d3.select(svgRef.current);
    const width = svgRef.current.clientWidth;
    const height = svgRef.current.clientHeight;

    svg.selectAll('*').remove();

    // Force simulation
    const simulation = d3
      .forceSimulation(data.nodes as any)
      .force('link', d3.forceLink(data.links).id((d: any) => d.id).distance(config.linkDistance))
      .force('charge', d3.forceManyBody().strength(config.chargeStrength))
      .force('center', d3.forceCenter(width / 2, height / 2));

    // Links
    const link = svg
      .append('g')
      .selectAll('line')
      .data(data.links)
      .join('line')
      .attr('stroke', '#999')
      .attr('stroke-opacity', 0.6)
      .attr('stroke-width', (d) => Math.sqrt(d.value || 1));

    // Nodes
    const node = svg
      .append('g')
      .selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', (d) => {
        if (config.nodeSize === 'degree') {
          const degree = data.links.filter(l => l.source === d.id || l.target === d.id).length;
          return 5 + degree * 2;
        }
        return 5 + Math.sqrt(d.value || 1);
      })
      .attr('fill', (d) => {
        const colorScale = d3.scaleOrdinal(d3.schemeCategory10);
        return colorScale(d.group || '0');
      })
      .call(drag(simulation) as any);

    // Labels
    const label = svg
      .append('g')
      .selectAll('text')
      .data(data.nodes)
      .join('text')
      .text((d) => d.id)
      .attr('font-size', 10)
      .attr('dx', 12)
      .attr('dy', 4);

    simulation.on('tick', () => {
      link
        .attr('x1', (d: any) => d.source.x)
        .attr('y1', (d: any) => d.source.y)
        .attr('x2', (d: any) => d.target.x)
        .attr('y2', (d: any) => d.target.y);

      node.attr('cx', (d: any) => d.x).attr('cy', (d: any) => d.y);

      label.attr('x', (d: any) => d.x).attr('y', (d: any) => d.y);
    });

    function drag(simulation: any) {
      return d3
        .drag()
        .on('start', (event, d: any) => {
          if (!event.active) simulation.alphaTarget(0.3).restart();
          d.fx = d.x;
          d.fy = d.y;
        })
        .on('drag', (event, d: any) => {
          d.fx = event.x;
          d.fy = event.y;
        })
        .on('end', (event, d: any) => {
          if (!event.active) simulation.alphaTarget(0);
          d.fx = null;
          d.fy = null;
        });
    }

    return () => {
      simulation.stop();
    };
  }, [data, config]);

  return <svg ref={svgRef} style={{ width: '100%', height: '100%' }} />;
};
```

**Deliverable:** Interactive network graph with D3.js

---

#### **T5: KPI Tile Widget Component** (20h)

**Implementation:**

```typescript
// frontend/src/components/widgets/KPITileWidget.tsx
import React from 'react';
import { Box, Typography } from '@mui/material';
import { ArrowUpward, ArrowDownward, Remove } from '@mui/icons-material';
import { Sparklines, SparklinesLine } from 'react-sparklines';

interface KPITileWidgetProps {
  data: {
    current: number;
    previous: number;
    trend: 'up' | 'down' | 'neutral';
    trendPercent: number;
    sparklineData: number[];
  };
  config: {
    title: string;
    format: 'currency' | 'percentage' | 'integer' | 'decimal';
    currencySymbol?: string;
    showSparkline: boolean;
  };
}

export const KPITileWidget: React.FC<KPITileWidgetProps> = ({ data, config }) => {
  const formatValue = (value: number) => {
    switch (config.format) {
      case 'currency':
        return `${config.currencySymbol || '€'}${(value / 1000000).toFixed(1)}M`;
      case 'percentage':
        return `${value.toFixed(1)}%`;
      case 'integer':
        return value.toLocaleString();
      case 'decimal':
        return value.toFixed(2);
    }
  };

  const getTrendColor = () => {
    switch (data.trend) {
      case 'up':
        return 'success.main';
      case 'down':
        return 'error.main';
      case 'neutral':
        return 'text.secondary';
    }
  };

  const getTrendIcon = () => {
    switch (data.trend) {
      case 'up':
        return <ArrowUpward fontSize="small" />;
      case 'down':
        return <ArrowDownward fontSize="small" />;
      case 'neutral':
        return <Remove fontSize="small" />;
    }
  };

  return (
    <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="caption" color="text.secondary">
        {config.title}
      </Typography>

      <Typography variant="h3" sx={{ my: 2 }}>
        {formatValue(data.current)}
      </Typography>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: getTrendColor() }}>
        {getTrendIcon()}
        <Typography variant="body2">
          {data.trendPercent > 0 && '+'}{data.trendPercent.toFixed(1)}%
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
          vs. last period
        </Typography>
      </Box>

      {config.showSparkline && (
        <Box sx={{ mt: 'auto', pt: 2 }}>
          <Sparklines data={data.sparklineData} width={200} height={40}>
            <SparklinesLine color={getTrendColor() === 'success.main' ? '#4caf50' : '#f44336'} />
          </Sparklines>
        </Box>
      )}
    </Box>
  );
};
```

**Deliverable:** KPI tile with trend & sparkline

---

#### **T6: Widget Configuration UI** (10h)

**Implementation:**

```typescript
// frontend/src/components/widgets/WidgetConfigPanel.tsx
import React from 'react';
import { Box, TextField, Select, MenuItem, FormControlLabel, Switch } from '@mui/material';

interface WidgetConfigPanelProps {
  widgetType: string;
  config: any;
  onChange: (newConfig: any) => void;
}

export const WidgetConfigPanel: React.FC<WidgetConfigPanelProps> = ({
  widgetType,
  config,
  onChange
}) => {
  // Render config UI based on widget type
  switch (widgetType) {
    case 'heatmap':
      return (
        <Box>
          <TextField
            label="X-Axis Dimension"
            value={config.xAxis}
            onChange={(e) => onChange({ ...config, xAxis: e.target.value })}
            fullWidth
            sx={{ mb: 2 }}
          />
          <TextField
            label="Y-Axis Dimension"
            value={config.yAxis}
            onChange={(e) => onChange({ ...config, yAxis: e.target.value })}
            fullWidth
            sx={{ mb: 2 }}
          />
          <Select
            label="Color Scale"
            value={config.colorScale}
            onChange={(e) => onChange({ ...config, colorScale: e.target.value })}
            fullWidth
          >
            <MenuItem value="Blues">Blues</MenuItem>
            <MenuItem value="Reds">Reds</MenuItem>
            <MenuItem value="RdYlGn">Red-Yellow-Green</MenuItem>
          </Select>
        </Box>
      );

    case 'kpi':
      return (
        <Box>
          <TextField
            label="Title"
            value={config.title}
            onChange={(e) => onChange({ ...config, title: e.target.value })}
            fullWidth
            sx={{ mb: 2 }}
          />
          <Select
            label="Format"
            value={config.format}
            onChange={(e) => onChange({ ...config, format: e.target.value })}
            fullWidth
            sx={{ mb: 2 }}
          >
            <MenuItem value="currency">Currency</MenuItem>
            <MenuItem value="percentage">Percentage</MenuItem>
            <MenuItem value="integer">Integer</MenuItem>
          </Select>
          <FormControlLabel
            control={
              <Switch
                checked={config.showSparkline}
                onChange={(e) => onChange({ ...config, showSparkline: e.target.checked })}
              />
            }
            label="Show Sparkline"
          />
        </Box>
      );

    // ... other widget types
    default:
      return null;
  }
};
```

**Deliverable:** Dynamic configuration panel

---

#### **T7: Widget Registry Service** (10h)

**Implementation:**

```typescript
// frontend/src/services/widgetRegistry.ts
import { z } from 'zod';
import { HeatmapWidget } from '@/components/widgets/HeatmapWidget';
import { SankeyWidget } from '@/components/widgets/SankeyWidget';
import { TreemapWidget } from '@/components/widgets/TreemapWidget';
import { NetworkGraphWidget } from '@/components/widgets/NetworkGraphWidget';
import { KPITileWidget } from '@/components/widgets/KPITileWidget';

export interface WidgetType {
  id: string;
  name: string;
  description: string;
  icon: string;
  category: 'chart' | 'kpi' | 'table' | 'advanced';
  configSchema: z.ZodObject<any>;
  component: React.ComponentType<any>;
}

export const WIDGET_REGISTRY: WidgetType[] = [
  {
    id: 'heatmap',
    name: 'Heatmap',
    description: '2D density visualization',
    icon: 'GridOnIcon',
    category: 'advanced',
    configSchema: z.object({
      xAxis: z.string(),
      yAxis: z.string(),
      colorScale: z.enum(['Blues', 'Reds', 'RdYlGn'])
    }),
    component: HeatmapWidget
  },
  {
    id: 'sankey',
    name: 'Sankey Diagram',
    description: 'Flow visualization',
    icon: 'AccountTreeIcon',
    category: 'advanced',
    configSchema: z.object({
      align: z.enum(['justify', 'center', 'start', 'end']),
      sort: z.enum(['auto', 'input', 'ascending', 'descending'])
    }),
    component: SankeyWidget
  },
  {
    id: 'treemap',
    name: 'Treemap',
    description: 'Hierarchical data tiles',
    icon: 'GridViewIcon',
    category: 'advanced',
    configSchema: z.object({
      tile: z.enum(['binary', 'squarify', 'slice', 'dice', 'sliceDice']),
      colorBy: z.enum(['value', 'depth', 'id'])
    }),
    component: TreemapWidget
  },
  {
    id: 'network',
    name: 'Network Graph',
    description: 'Node-link relationships',
    icon: 'BubbleChartIcon',
    category: 'advanced',
    configSchema: z.object({
      chargeStrength: z.number().min(-1000).max(0),
      linkDistance: z.number().min(10).max(500),
      nodeSize: z.enum(['degree', 'value'])
    }),
    component: NetworkGraphWidget
  },
  {
    id: 'kpi',
    name: 'KPI Tile',
    description: 'Big number + trend',
    icon: 'TrendingUpIcon',
    category: 'kpi',
    configSchema: z.object({
      title: z.string(),
      format: z.enum(['currency', 'percentage', 'integer', 'decimal']),
      currencySymbol: z.string().optional(),
      showSparkline: z.boolean()
    }),
    component: KPITileWidget
  }
];

export const getWidgetType = (id: string) =>
  WIDGET_REGISTRY.find((w) => w.id === id);
```

**Deliverable:** Centralized widget registry

---

#### **T8: Testing & Performance** (10h)

**E2E tests:**

```typescript
// e2e/specs/widgets/extended-widgets.spec.ts
test('Heatmap widget renders correctly', async ({ page }) => {
  await page.goto('/dashboard');
  await page.click('button:has-text("Add Widget")');
  await page.click('button:has-text("Heatmap")');

  // Verify heatmap rendered
  await expect(page.locator('[data-testid="heatmap-widget"]')).toBeVisible();
  await expect(page.locator('svg')).toBeVisible();

  // Verify tooltip on hover
  await page.hover('rect[data-value]');
  await expect(page.locator('text=/.*workflows.*/i')).toBeVisible();
});

test('KPI Tile displays trend correctly', async ({ page }) => {
  await page.goto('/dashboard');
  await page.click('button:has-text("Add Widget")');
  await page.click('button:has-text("KPI Tile")');

  // Verify big number
  await expect(page.locator('text=€2.5M')).toBeVisible();

  // Verify trend indicator
  await expect(page.locator('[data-testid="trend-icon"]')).toBeVisible();
  await expect(page.locator('text=+12%')).toBeVisible();
});
```

**Performance benchmarks:**

- Heatmap: 100×100 cells render < 1s
- Network Graph: 1,000 nodes render < 2s
- KPI Tile: Update instantly (<50ms)

**Deliverable:** E2E tests + performance validation

---

## 🧪 TESTING

### Unit Tests

```typescript
// frontend/src/components/widgets/__tests__/KPITileWidget.test.tsx
import { render } from '@testing-library/react';
import { KPITileWidget } from '../KPITileWidget';

test('renders KPI with upward trend', () => {
  const data = {
    current: 2500000,
    previous: 2230000,
    trend: 'up' as const,
    trendPercent: 12.1,
    sparklineData: [2.1, 2.2, 2.3, 2.4, 2.5]
  };

  const config = {
    title: 'Revenue',
    format: 'currency' as const,
    currencySymbol: '€',
    showSparkline: true
  };

  const { getByText } = render(<KPITileWidget data={data} config={config} />);

  expect(getByText('€2.5M')).toBeInTheDocument();
  expect(getByText('+12.1%')).toBeInTheDocument();
});
```

---

## 📊 SUCCESS METRICS

- ✅ 10+ widget types available (Heatmap, Sankey, Treemap, Network, KPI + existing)
- ✅ Widget render performance:
  - Heatmap 100×100: <1s
  - Network 1,000 nodes: <2s
  - KPI Tile update: <50ms
- ✅ Widget configuration UI: Save config < 200ms

---

## 🔗 DEPENDENCIES

- **EPIC-014 S9:** Table component (for data grids in widgets)
- **Libraries:**
  - `@nivo/heatmap` (Heatmap widget)
  - `@nivo/sankey` (Sankey diagram)
  - `@nivo/treemap` (Treemap widget)
  - `d3` (Network graph)
  - `react-sparklines` (KPI sparklines)

---

**Status:** 📋 TODO  
**Effort:** ~150 hours (~4 sprints)  
**Next:** Integrate with S1 (DataView) as widget options

