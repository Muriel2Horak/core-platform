# T1: Grid Layout Component

**Story:** [S3: Dashboard Grid Layout](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Implementovat 12-column drag & drop grid s react-grid-layout.

---

## 🎯 ACCEPTANCE CRITERIA

1. Responsive grid (12 columns)
2. Drag & drop repositioning
3. Resize widgets
4. Layout persistence

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dashboard/DashboardGrid.tsx
import GridLayout from 'react-grid-layout';

export const DashboardGrid: React.FC<{ widgets: Widget[] }> = ({ widgets }) => {
  const [layout, setLayout] = useState(widgets.map(w => ({
    i: w.id,
    x: w.x,
    y: w.y,
    w: w.width,
    h: w.height
  })));
  
  return (
    <GridLayout
      cols={12}
      rowHeight={50}
      layout={layout}
      onLayoutChange={setLayout}
      draggableHandle=".widget-header"
    >
      {widgets.map(w => (
        <div key={w.id}>
          <Widget {...w} />
        </div>
      ))}
    </GridLayout>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] DashboardGrid component
- [ ] Drag & drop
- [ ] Resize functionality
- [ ] Layout save/load

---

**Estimated:** 15 hours
