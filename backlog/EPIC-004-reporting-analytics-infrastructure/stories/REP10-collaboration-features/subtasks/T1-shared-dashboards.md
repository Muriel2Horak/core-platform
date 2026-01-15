# T1: Shared Dashboards

**Story:** [S10: Collaboration Features](README.md)  
**Effort:** 12 hours  
**Priority:** P1  
**Dependencies:** None

---

## 📋 OBJECTIVE

Share dashboards s týmem.

---

## 🏗️ IMPLEMENTATION

```java
@Entity
public class DashboardShare {
  @ManyToOne
  private Dashboard dashboard;
  
  @ManyToOne
  private User sharedWith;
  
  private SharePermission permission; // VIEW, EDIT
}
```

---

## ✅ Acceptance Criteria

- [ ] Dashboard lze sdilet s uzivatelem/rolí a nastavit prava (view/edit).
- [ ] Sdileni ma expiraci a moznost zruseni.
- [ ] Sdileni je auditovano (kdo/co/kdy).

---

## ✅ DELIVERABLES

- [ ] Share system
- [ ] Permissions
- [ ] Share UI

---

**Estimated:** 12 hours
