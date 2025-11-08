# T1: Share System

**Story:** [S10: Dashboard & View Sharing](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Share dashboardy a data views s uživateli/týmy.

---

## 🎯 ACCEPTANCE CRITERIA

1. Share dialog
2. Select users/teams
3. View/edit permissions
4. Share link generation

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/sharing/DashboardShare.java
@Entity
public class DashboardShare {
  @Id
  private UUID id;
  
  @ManyToOne
  private Dashboard dashboard;
  
  @ManyToOne
  private User sharedWith;
  
  @Enumerated(EnumType.STRING)
  private SharePermission permission; // VIEW, EDIT
  
  private String shareToken; // For public links
  private LocalDateTime expiresAt;
}
```

```typescript
// frontend/src/components/sharing/ShareDialog.tsx
export const ShareDialog: React.FC<{ dashboardId: string }> = ({ dashboardId }) => {
  const [selectedUsers, setSelectedUsers] = useState<User[]>([]);
  const [permission, setPermission] = useState<'VIEW' | 'EDIT'>('VIEW');
  
  const share = async () => {
    await api.post(`/api/dashboards/${dashboardId}/share`, {
      userIds: selectedUsers.map(u => u.id),
      permission
    });
  };
  
  return (
    <Dialog open>
      <UserPicker selected={selectedUsers} onChange={setSelectedUsers} />
      <Select value={permission} onChange={(e) => setPermission(e.target.value)}>
        <MenuItem value="VIEW">View only</MenuItem>
        <MenuItem value="EDIT">Can edit</MenuItem>
      </Select>
      <Button onClick={share}>Share</Button>
    </Dialog>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Share entity + API
- [ ] Share dialog
- [ ] User/team picker

---

**Estimated:** 20 hours
