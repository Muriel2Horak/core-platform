# T1: Redis Cache

**Story:** [S7: Performance Optimization](README.md)  
**Effort:** 10 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Redis cache pro pre-aggregations.

---

## 🏗️ IMPLEMENTATION

```yaml
# cube.yml
cache:
  type: redis
  host: redis
  port: 6379
  ttl: 600
```

---

## ✅ DELIVERABLES

- [ ] Redis integration
- [ ] Cache invalidation
- [ ] Performance tests

---

**Estimated:** 10 hours
