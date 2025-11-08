# T1: Cube.js Schema Setup

**Story:** [S1: Cube.js Data Modeling](README.md)  
**Effort:** 8 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Implementovat základní Cube.js schémata pro Users, Tenants, WorkflowInstances.

---

## 🎯 ACCEPTANCE CRITERIA

1. 3 cube schemas vytvořeny
2. Dimensions definovány
3. Measures (count, sum, avg)
4. Multi-tenant WHERE clause

---

## 🏗️ IMPLEMENTATION

```javascript
// cube/schema/Users.js
cube(`Users`, {
  sql: `SELECT * FROM core.users WHERE tenant_id = ${SECURITY_CONTEXT.tenant_id}`,
  
  dimensions: {
    id: { sql: `id`, type: `number`, primaryKey: true },
    firstName: { sql: `first_name`, type: `string` },
    email: { sql: `email`, type: `string` },
    createdAt: { sql: `created_at`, type: `time` }
  },
  
  measures: {
    count: { type: `count` },
    avgAge: { sql: `age`, type: `avg` }
  }
});
```

---

## ✅ DELIVERABLES

- [ ] Users.js cube
- [ ] Tenants.js cube
- [ ] WorkflowInstances.js cube
- [ ] Security context tested

---

**Estimated:** 8 hours
