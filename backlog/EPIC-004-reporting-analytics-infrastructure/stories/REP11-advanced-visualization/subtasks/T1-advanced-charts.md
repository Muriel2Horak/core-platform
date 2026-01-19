# T1: Advanced Charts

**Story:** [S11: Advanced Visualization](README.md)  
**Effort:** 15 hours  
**Priority:** P2  
**Dependencies:** None

---

## 📋 OBJECTIVE

Pokročilé chart types (heatmap, sankey, treemap).

---

## 🏗️ IMPLEMENTATION

```typescript
import { ResponsiveHeatMap } from '@nivo/heatmap';

export const HeatmapChart: React.FC = ({ data }) => (
  <ResponsiveHeatMap data={data} />
);
```

---

## ✅ Acceptance Criteria

- [ ] Implementovany jsou min. 3 nove chart typy.
- [ ] Charty podporuji tooltip + highlight.
- [ ] Data transformace z Cube.js je reusable.

---

## ✅ DELIVERABLES

- [ ] 3+ advanced chart types
- [ ] Interactive features
- [ ] Responsive design

---

**Estimated:** 15 hours
