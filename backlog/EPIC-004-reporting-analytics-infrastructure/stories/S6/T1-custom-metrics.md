# T1: Custom Metrics

**Story:** [S6: Custom Calculated Metrics](README.md)  
**Effort:** 18 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

User-defined calculated metrics.

---

## 🏗️ IMPLEMENTATION

```java
@Entity
public class CustomMetric {
  private String name;
  private String formula; // "SUM(revenue) / COUNT(users)"
  
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> cubeConfig;
}
```

---

## ✅ DELIVERABLES

- [ ] CustomMetric entity
- [ ] Formula parser
- [ ] Cube.js integration

---

**Estimated:** 18 hours
