# T3: Save & Share Layouts

**Story:** [S8: Customizable Entity Popups](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Save layouts, share s týmem.

---

## 🎯 ACCEPTANCE CRITERIA

1. Save layout to backend
2. Load layout
3. Share with team
4. Default layout per role

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/layout/EntityLayout.java
@Entity
public class EntityLayout {
  @Id
  private UUID id;
  
  private String entityType;
  private String name;
  
  @Column(columnDefinition = "jsonb")
  private List<LayoutSection> sections;
  
  @ManyToOne
  private User owner;
  
  private boolean isShared;
}
```

---

## ✅ DELIVERABLES

- [ ] Layout entity + API
- [ ] Save/load
- [ ] Share functionality

---

**Estimated:** 5 hours
