# T1: Row-Level Security

**Story:** [S5: Security & Permissions](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Row-level security pro multi-tenant isolation.

---

## 🏗️ IMPLEMENTATION

```javascript
// cube/schema/Users.js
cube(`Users`, {
  sql: `SELECT * FROM users WHERE tenant_id = ${SECURITY_CONTEXT.tenant_id}`,
  // ...
});
```

---

## ✅ DELIVERABLES

- [ ] Security context
- [ ] Tenant isolation
- [ ] Permission checks

---

## ✅ Acceptance Criteria

- [ ] Deliverables listed above are completed and reviewed.
- [ ] Relevant code/tests/docs updated for this task.
- [ ] Outcome verified locally (or in CI where applicable).

**Estimated:** 15 hours