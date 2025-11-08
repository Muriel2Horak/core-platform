# META-013: Reporting & Analytics Integration

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~1,000 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **generovat Cube.js schemas z metamodelu**, abych **měl automatické reporting capabilities pro všechny entity**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 7️⃣ Reporting & analytika: Metamodel jako základ pro reporting - popisuje které entity / pole jsou agregovatelné, group-by, dimension, measure. Slouží jako vstup pro Cube/semantic layer, generování views/materializací, generování UI pro přehledy (tabulky + grafy). RLS/tenant a RBAC se používá i pro reporting.

---

## 🎯 Acceptance Criteria

### AC1: Field Metadata for Reporting
```yaml
entity: Order
fields:
  - name: customer_name
    type: string
    reporting:
      dimension: true      # Group-by capable
      filterable: true
  
  - name: total_amount
    type: decimal
    reporting:
      measure: true        # Aggregatable
      aggregations: [sum, avg, min, max]
  
  - name: created_at
    type: datetime
    reporting:
      dimension: true
      timeGranularity: [day, week, month, year]
```

### AC2: Cube.js Schema Generation
- **GIVEN** Order entity s reporting metadata
- **THEN** generuje Cube schema:
```javascript
// generated/Order.js
cube(`Order`, {
  sql: `SELECT * FROM core_sales_order`,
  
  dimensions: {
    customer_name: {
      sql: `customer_name`,
      type: `string`
    },
    created_at: {
      sql: `created_at`,
      type: `time`
    }
  },
  
  measures: {
    total_amount_sum: {
      sql: `total_amount`,
      type: `sum`
    },
    total_amount_avg: {
      sql: `total_amount`,
      type: `avg`
    }
  },
  
  preAggregations: {
    daily_summary: {
      measures: [total_amount_sum],
      dimensions: [customer_name],
      timeDimension: created_at,
      granularity: `day`
    }
  }
});
```

### AC3: RLS in Cube Queries
- **GIVEN** tenant-aware entity
- **THEN** Cube query obsahuje:
```javascript
filters: [
  { member: `Order.tenant_id`, operator: `equals`, values: [context.tenantId] }
]
```

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
