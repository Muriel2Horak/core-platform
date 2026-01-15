# T3: Version History

**Story:** [S10: Collaboration Features](README.md)  
**Effort:** 9 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Ukladat verze dashboardu a umoznit rollback na vybranou verzi.

---

## 🏗️ IMPLEMENTATION

```json
{
  "dashboardId": "dash-123",
  "version": 5,
  "createdBy": "user@tenant.com",
  "snapshot": { "widgets": [] }
}
```

---

## ✅ Acceptance Criteria

- [ ] Vytvori se snapshot pri ulozeni dashboardu.
- [ ] UI zobrazi seznam poslednich verzi (min. 10).
- [ ] Rollback obnovi vybranou verzi a ulozi audit zaznam.

---

## ✅ DELIVERABLES

- [ ] Snapshot storage + retention policy
- [ ] Version history API + UI
- [ ] Rollback mechanizmus

---

**Estimated:** 9 hours
