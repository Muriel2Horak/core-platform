# T4: Dashboard Integration

**Story:** [S6: Visual Query Builder](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1, T2, T3

---

## 📋 OBJECTIVE

Save query jako dashboard widget.

---

## 🎯 ACCEPTANCE CRITERIA

1. "Add to Dashboard" button
2. Select dashboard
3. Save query config
4. Display widget on dashboard

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/query-builder/QueryBuilder.tsx (extension)
export const QueryBuilder: React.FC = () => {
  const [query, setQuery] = useState<QueryBuilderState>({...});
  
  const addToDashboard = async (dashboardId: string) => {
    const widget = {
      type: 'query',
      config: {
        dimensions: query.dimensions,
        measures: query.measures,
        filters: query.filters,
        chartType: 'bar'
      }
    };
    
    await api.post(`/api/dashboards/${dashboardId}/widgets`, widget);
  };
  
  return (
    <Box>
      <QueryBuilderUI />
      <QueryPreview query={query} />
      
      <Button onClick={() => setDashboardDialogOpen(true)}>
        Add to Dashboard
      </Button>
      
      <Dialog open={dashboardDialogOpen}>
        <DialogTitle>Select Dashboard</DialogTitle>
        <List>
          {dashboards.map(d => (
            <ListItem button onClick={() => addToDashboard(d.id)}>
              {d.name}
            </ListItem>
          ))}
        </List>
      </Dialog>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Add to dashboard
- [ ] Dashboard selector
- [ ] Widget save
- [ ] Unit tests

---

**Estimated:** 15 hours
