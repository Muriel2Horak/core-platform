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

**Estimated:** 15 hours
