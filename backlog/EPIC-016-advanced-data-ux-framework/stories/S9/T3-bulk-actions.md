# T3: Bulk Actions

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 20 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Multi-select tiles + bulk actions (delete, approve, export).

---

## 🎯 ACCEPTANCE CRITERIA

1. Checkbox selection
2. Select all
3. Bulk action toolbar
4. Progress feedback

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/bulk-actions/BulkActionToolbar.tsx
export const BulkActionToolbar: React.FC<{ selectedIds: string[] }> = ({ selectedIds }) => {
  const bulkDelete = async () => {
    await Promise.all(selectedIds.map(id => api.delete(`/api/workflows/${id}`)));
  };
  
  const bulkApprove = async () => {
    await Promise.all(selectedIds.map(id => api.post(`/api/workflows/${id}/transition`, { to: 'APPROVED' })));
  };
  
  return (
    <Box>
      <Typography>{selectedIds.length} selected</Typography>
      <Button onClick={bulkDelete} color="error">Delete</Button>
      <Button onClick={bulkApprove}>Approve</Button>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Selection UI
- [ ] Bulk actions
- [ ] Progress bar

---

**Estimated:** 20 hours
