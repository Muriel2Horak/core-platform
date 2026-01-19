# T2: Comments & Annotations

**Story:** [S10: Collaboration Features](README.md)  
**Effort:** 9 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Umoznit komentar a anotace na dashboardu a konkretnich widgetech.

---

## 🏗️ IMPLEMENTATION

```json
{
  "dashboardId": "dash-123",
  "widgetId": "widget-456",
  "text": "Revenue spike due to promo",
  "author": "user@tenant.com",
  "createdAt": "2025-11-07T10:30:00Z"
}
```

---

## ✅ Acceptance Criteria

- [ ] Komentar lze pridat na dashboard i konkretni widget.
- [ ] Thread reply je ulozeny a zobrazeny v UI.
- [ ] Notifikace pri odpovedi jsou tenant-scoped.

---

## ✅ DELIVERABLES

- [ ] Comment entity + CRUD API
- [ ] UI panel pro komentare a anotace
- [ ] Notifikace + audit log

---

**Estimated:** 9 hours
