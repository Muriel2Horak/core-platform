# T3: Live Preview

**Story:** [S6: Visual Query Builder](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Realtime preview table/chart při build query.

---

## 🎯 ACCEPTANCE CRITERIA

1. Execute query on change
2. Display table preview
3. Display chart preview
4. Loading states

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/query-builder/QueryPreview.tsx
export const QueryPreview: React.FC<{ query: QueryBuilderState }> = ({ query }) => {
  const { resultSet, isLoading, error } = useCubeQuery({
    dimensions: query.dimensions,
    measures: query.measures,
    filters: query.filters
  });
  
  const [viewMode, setViewMode] = useState<'table' | 'chart'>('table');
  
  if (isLoading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error.message}</Alert>;
  
  return (
    <Box>
      <ToggleButtonGroup value={viewMode} onChange={(e, v) => setViewMode(v)}>
        <ToggleButton value="table">Table</ToggleButton>
        <ToggleButton value="chart">Chart</ToggleButton>
      </ToggleButtonGroup>
      
      {viewMode === 'table' && <DataTable data={resultSet.tablePivot()} />}
      {viewMode === 'chart' && <ChartRenderer data={resultSet.chartPivot()} />}
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Preview component
- [ ] Table/chart toggle
- [ ] Live updates
- [ ] Unit tests

---

**Estimated:** 15 hours
