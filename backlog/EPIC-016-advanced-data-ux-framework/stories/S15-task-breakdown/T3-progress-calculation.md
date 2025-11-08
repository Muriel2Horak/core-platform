# T3: Progress Calculation

**Story:** [S15: Task Breakdown](README.md)  
**Effort:** 6 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Rekurzivní výpočet progress pro parent tasky - `(completedChildren / totalChildren) * 100`.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Progress bar** - zobrazit u parent tasks
2. **Recursive calculation** - children of children započítávají
3. **Status aggregation** - DONE children = completed
4. **Real-time update** - změna child → update parent

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/utils/progressCalculator.ts
export const calculateProgress = (node: HierarchicalTask): number => {
  if (!node.children || node.children.length === 0) {
    return node.status === 'DONE' ? 100 : 0;
  }

  const childProgress = node.children.map(calculateProgress);
  const totalProgress = childProgress.reduce((sum, p) => sum + p, 0);
  
  return totalProgress / node.children.length;
};
```

### UI Integration

```typescript
// TreeNode.tsx (update)
const progress = useMemo(() => calculateProgress(node), [node]);

<Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
  <Typography>{node.title}</Typography>
  
  {node.children?.length > 0 && (
    <Box sx={{ minWidth: 100 }}>
      <LinearProgress variant="determinate" value={progress} />
      <Typography variant="caption">{Math.round(progress)}%</Typography>
    </Box>
  )}
</Box>
```

---

## 📦 DELIVERABLES

- [ ] Progress calculation logic
- [ ] Progress bar UI
- [ ] Real-time updates
- [ ] Unit tests (70%+ coverage)

---

**Estimated:** 6 hours
