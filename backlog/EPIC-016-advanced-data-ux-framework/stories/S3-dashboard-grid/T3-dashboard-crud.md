# T3: Dashboard CRUD

**Story:** [S3: Dashboard Grid Layout](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Save/load/share dashboards - backend API + UI.

---

## 🎯 ACCEPTANCE CRITERIA

1. Save dashboard (name + widgets + layout)
2. Load dashboard from list
3. Share dashboard with team
4. Delete dashboard

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/dashboard/Dashboard.java
@Entity
public class Dashboard {
  @Id
  private UUID id;
  
  private String name;
  
  @Column(columnDefinition = "jsonb")
  private List<Widget> widgets;
  
  @Column(columnDefinition = "jsonb")
  private GridLayout layout;
  
  @ManyToOne
  private User owner;
  
  private boolean isShared;
}
```

```typescript
// frontend/src/components/dashboard/DashboardToolbar.tsx
export const DashboardToolbar: React.FC = () => {
  const saveDashboard = async () => {
    await api.post('/api/dashboards', {
      name: dashboardName,
      widgets: currentWidgets,
      layout: currentLayout
    });
  };
  
  return (
    <Box>
      <Button onClick={saveDashboard}>Save Dashboard</Button>
      <Button onClick={shareDashboard}>Share</Button>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Dashboard entity + API
- [ ] Save/load UI
- [ ] Share functionality
- [ ] Unit tests

---

**Estimated:** 10 hours
