# T2: Export Progress Tracking

**Story:** [S8: Frontend-Backend Integration](README.md)  
**Effort:** 6 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Real-time progress bar pro PDF/Excel export.

---

## 🏗️ IMPLEMENTATION

```typescript
export const ExportProgress: React.FC<{ jobId: string }> = ({ jobId }) => {
  const { progress } = useExportProgress(jobId);
  
  return (
    <Box>
      <LinearProgress value={progress.percentage} />
      <Typography>{progress.status}</Typography>
    </Box>
  );
};
```

---

## ✅ Acceptance Criteria

- [ ] API vraci `status` a `progress` (0-100) pro export job.
- [ ] UI zobrazuje progress bar + stavovy text + ETA (pokud dostupne).
- [ ] Po dokonceni zobrazuje download tlacitko.
- [ ] Pri selhani nabizi retry + zobrazi error message.

---

## ✅ DELIVERABLES

- [ ] Progress tracking
- [ ] WebSocket updates
- [ ] Estimated completion time

---

**Estimated:** 6 hours
