# MOD-004: UI Manifest Integration

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** MOD-002, UI-Spec Engine  
**Category:** Module System

---

## 📖 User Story

**As a module**,  
I want to declare my UI (menu, views, dashboards) in manifest,  
So that frontend renders module UI without code changes.

---

## 🎯 Acceptance Criteria

- ⏳ Module manifest declares menu items (label, route, icon, roles)
- ⏳ Frontend fetches menu from `/api/modules/menu` (filtered by tenant + role)
- ⏳ Module views registered (list, detail, form specs)
- ⏳ Dynamic routing: `/modules/{moduleId}/entities/{entityName}`
- ⏳ Module disabled → menu hidden, routes return 404

---

## 💻 Implementation

### Manifest UI Section

```json
{
  "id": "helpdesk",
  "provides": {
    "ui": {
      "menu": [
        {
          "label": "Tickets",
          "route": "/helpdesk/tickets",
          "icon": "support_agent",
          "roles": ["HELPDESK_USER", "HELPDESK_ADMIN"]
        }
      ],
      "views": [
        {
          "entity": "helpdesk.Ticket",
          "type": "list",
          "spec": "ticket-list.json"
        },
        {
          "entity": "helpdesk.Ticket",
          "type": "detail",
          "spec": "ticket-detail.json"
        }
      ],
      "dashboards": [
        {
          "name": "HelpdeskOverview",
          "route": "/helpdesk/dashboard",
          "spec": "dashboard.json"
        }
      ]
    }
  }
}
```

### Frontend API

```typescript
// Fetch menu items (filtered by tenant + user roles)
const response = await fetch('/api/modules/menu');
const menuItems: MenuItem[] = await response.json();

// Example response:
[
  {
    moduleId: "helpdesk",
    label: "Tickets",
    route: "/helpdesk/tickets",
    icon: "support_agent",
    enabled: true
  }
]
```

---

**Last Updated:** 9. listopadu 2025
