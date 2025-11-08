# T4: Chart Rendering

**Story:** [S1: Universal Data View Engine](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** T3

---

## 📋 OBJECTIVE

Implementovat chart rendering s Recharts library (Bar, Line, Pie charts).

---

## 🎯 ACCEPTANCE CRITERIA

1. Bar chart renderer
2. Line chart renderer  
3. Pie chart renderer
4. Chart configuration (colors, legends, axes)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dataview/ChartRenderer.tsx
import { BarChart, Bar, XAxis, YAxis } from 'recharts';

export const ChartRenderer: React.FC<{ data: any[]; chartType: ChartType }> = ({ data, chartType }) => {
  if (chartType === 'bar') {
    return (
      <BarChart data={data}>
        <XAxis dataKey="name" />
        <YAxis />
        <Bar dataKey="value" fill="#8884d8" />
      </BarChart>
    );
  }
  // Similar for Line, Pie...
};
```

---

## ✅ DELIVERABLES

- [ ] ChartRenderer component
- [ ] Bar/Line/Pie support
- [ ] Chart customization
- [ ] Unit tests

---

**Estimated:** 15 hours
