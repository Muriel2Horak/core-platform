# T1: DataView Core Component

**Story:** [S1: Universal Data View Engine](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Implementovat základní `<DataView>` React component který přijímá `entity` prop a automaticky načítá data z Cube.js.

---

## 🎯 ACCEPTANCE CRITERIA

1. `<DataView entity="Users">` prop API
2. Automatické načtení Cube.js schema
3. Default table view rendering
4. Loading states a error handling

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dataview/DataView.tsx
interface DataViewProps {
  entity: string;  // "Users", "Workflows", etc.
  defaultView?: ViewMode;
}

export const DataView: React.FC<DataViewProps> = ({ entity, defaultView = 'table' }) => {
  const { data, loading, error } = useCubeQuery({ entity });
  
  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error.message}</Alert>;
  
  return <DataViewRenderer data={data} mode={defaultView} />;
};
```

---

## ✅ DELIVERABLES

- [ ] DataView component
- [ ] Cube.js hook integration
- [ ] Error handling
- [ ] Unit tests

---

**Estimated:** 20 hours
