# T2: Pre-aggregations

**Story:** [S1: Cube.js Data Modeling](README.md)  
**Effort:** 10 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Pre-aggregations pro <100ms query performance.

---

## 🎯 ACCEPTANCE CRITERIA

1. Workflow rollup pre-agg
2. User stats pre-agg
3. Auto-refresh (10 min)
4. <100ms response time

---

## 🏗️ IMPLEMENTATION

```javascript
// cube/schema/WorkflowInstances.js
cube(`WorkflowInstances`, {
  // ... dimensions, measures
  
  preAggregations: {
    workflowRollup: {
      type: `rollup`,
      dimensions: [CUBE.status, CUBE.tenantId],
      measures: [CUBE.count],
      timeDimension: CUBE.createdAt,
      granularity: `day`,
      refreshKey: { every: `10 minutes` }
    }
  }
});
```

---

## ✅ DELIVERABLES

- [ ] 3+ pre-aggregations
- [ ] Refresh keys configured
- [ ] Performance tested (<100ms)

---

**Estimated:** 10 hours
