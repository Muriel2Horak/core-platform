# T1: Dashboard Components

**Story:** [S2: Frontend Dashboard Components](README.md)  
**Effort:** 12 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

React komponenty pro dashboardy (ChartWidget, TableWidget, MetricCard).

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/reporting/ChartWidget.tsx
export const ChartWidget: React.FC<{ query: CubeQuery }> = ({ query }) => {
  const { resultSet } = useCubeQuery(query);
  
  return (
    <Card>
      <BarChart data={resultSet.chartPivot()} />
    </Card>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] ChartWidget
- [ ] TableWidget  
- [ ] MetricCard
- [ ] Responsive grid

---

## ✅ Acceptance Criteria

- [ ] Deliverables listed above are completed and reviewed.
- [ ] Relevant code/tests/docs updated for this task.
- [ ] Outcome verified locally (or in CI where applicable).

**Estimated:** 12 hours