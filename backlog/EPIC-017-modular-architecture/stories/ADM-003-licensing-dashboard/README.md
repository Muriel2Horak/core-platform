---
id: ADM-003
epic: EPIC-017-modular-architecture
title: "Licensing Dashboard"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/ADM-003-licensing-dashboard/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---

# ADM-003: Licensing Dashboard

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** LIC-001  
**Category:** Admin UI

---

## 📖 User Story

**As a platform admin**,  
I want a dashboard showing license status,  
So that I can proactively renew expiring licenses.

---

## 🎯 Acceptance Criteria

- ⏳ Widget: Expiring licenses (30 days warning)
- ⏳ Widget: User count vs limit per module
- ⏳ Widget: Revenue forecast (based on active licenses)
- ⏳ Audit log viewer (license uploads, denials)
- ⏳ Export license report (CSV)

---

## 🧩 Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | KPI widgets: active licenses, expiring, disabled | 0.5d | LIC-003 |
| 2 | Usage/consumption charts + filters | 0.5d | 1 |
| 3 | Expiry alerts + export (CSV/PDF) | 0.25d | 1 |
| 4 | Backend endpoints integration + caching | 0.25d | 1 |
| 5 | UI tests + snapshot tests | 0.25d | 2 |

---

## 💻 Implementation

### Dashboard Widgets

```tsx
// LicensingDashboard.tsx

import React from 'react';
import { Card, CardContent, Typography, List, ListItem } from '@mui/material';
import { Warning, People, AttachMoney } from '@mui/icons-material';

export const LicensingDashboard: React.FC = () => {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16 }}>
      {/* Expiring Licenses Widget */}
      <Card>
        <CardContent>
          <Typography variant="h6">
            <Warning /> Expiring Soon
          </Typography>
          <List>
            <ListItem>Helpdesk (Partner Vendor) - 7 days</ListItem>
            <ListItem>CRM (Acme Corp) - 15 days</ListItem>
          </List>
        </CardContent>
      </Card>
      
      {/* User Count Widget */}
      <Card>
        <CardContent>
          <Typography variant="h6">
            <People /> User Limits
          </Typography>
          <List>
            <ListItem>Helpdesk: 45 / 50 users</ListItem>
            <ListItem>CRM: 80 / 100 users</ListItem>
          </List>
        </CardContent>
      </Card>
      
      {/* Revenue Widget */}
      <Card>
        <CardContent>
          <Typography variant="h6">
            <AttachMoney /> Revenue Forecast
          </Typography>
          <Typography variant="h4">€15,000 / month</Typography>
          <Typography variant="body2">Based on active licenses</Typography>
        </CardContent>
      </Card>
    </div>
  );
};
```

---

**Last Updated:** 9. listopadu 2025
