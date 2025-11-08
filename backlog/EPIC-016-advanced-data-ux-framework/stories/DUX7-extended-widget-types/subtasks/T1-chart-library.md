# T1: Extended Chart Library

**Story:** [S7: Extended Data Widgets](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Integrovat Recharts/Chart.js/D3.js - pokročilé typy chartů.

---

## 🎯 ACCEPTANCE CRITERIA

1. Heatmap
2. Scatter plot
3. Treemap
4. Sankey diagram

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/charts/HeatmapChart.tsx
import { ResponsiveHeatMap } from '@nivo/heatmap';

export const HeatmapChart: React.FC<{ data: any[] }> = ({ data }) => {
  return (
    <ResponsiveHeatMap
      data={data}
      margin={{ top: 60, right: 60, bottom: 60, left: 60 }}
      axisTop={{ tickSize: 5, tickPadding: 5, tickRotation: -45 }}
      colors={{ type: 'sequential', scheme: 'blues' }}
    />
  );
};

// frontend/src/components/charts/ScatterChart.tsx
import { ScatterChart as RechartsScatter, Scatter, XAxis, YAxis } from 'recharts';

export const ScatterChart: React.FC<{ data: any[] }> = ({ data }) => {
  return (
    <RechartsScatter width={600} height={400}>
      <XAxis dataKey="x" />
      <YAxis dataKey="y" />
      <Scatter data={data} fill="#8884d8" />
    </RechartsScatter>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] 4 new chart types
- [ ] Chart type selector
- [ ] Responsive sizing
- [ ] Unit tests

---

**Estimated:** 20 hours
