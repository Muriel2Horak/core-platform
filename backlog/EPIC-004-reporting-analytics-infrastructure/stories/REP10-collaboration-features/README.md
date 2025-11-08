# S10: Collaboration Features

**Status:** 📋 **PLANNED** (Phase 9 - Post-MVP)  
**Priority:** P3 (Nice-to-Have)  
**Effort:** TBD (~25-35 hodin estimate)  
**Dependencies:** Backend infrastructure pro sharing + comments

---

## 🎯 Vision

**Team collaboration** on dashboards - share reports, discuss insights, track changes.

**User Stories:**

1. **Dashboard Sharing**  
   "As a team lead, I want to share dashboard with external stakeholder via public link (expires in 7 days), so they can view results without login."

2. **Comments & Annotations**  
   "As an analyst, I want to add comment 'Revenue spike due to Black Friday promo' directly on chart, so team understands context."

3. **Version History**  
   "As a dashboard creator, I want to revert to last week's version after colleague accidentally broke layout, so I can undo changes."

---

## 📋 Feature Breakdown (HIGH-LEVEL)

### Feature 1: Dashboard Sharing

**Current Limitation:**
- Dashboards pouze pro authenticated users v tenantovi
- External stakeholders NEMOHOU vidět reports (bez account creation)

**Proposed:**
```typescript
// Share dialog
{
  "shareType": "public_link",  // or "specific_users", "tenant_wide"
  "permissions": "view_only",  // or "edit", "comment"
  "expiresAt": "2025-11-14",   // 7 days from now
  "passwordProtected": true,
  "password": "SecurePass123",
  "anonymizeData": true         // Hide sensitive fields (PII)
}

// Generated URL:
// https://admin.core-platform.local/reports/shared/abc123?token=xyz789
```

**GAPS:**
- ❌ Security model nedefinovaný (row-level security pro shared links?)
- ❌ Anonymization rules (které fields hide?)
- ❌ Expiration mechanism (automatic cleanup?)

---

### Feature 2: Comments & Annotations

**Proposed:**
```typescript
// Comment object
{
  "id": "comment-123",
  "dashboardId": "dashboard-456",
  "widgetId": "widget-789",      // Optional: comment on specific widget
  "position": { "x": 150, "y": 200 },  // Chart coordinates
  "author": "john.doe@company.com",
  "text": "Revenue spike due to Black Friday promo",
  "createdAt": "2025-11-07T10:30:00Z",
  "replies": [
    {
      "author": "jane.smith@company.com",
      "text": "Good insight! Let's track this for next year.",
      "createdAt": "2025-11-07T11:00:00Z"
    }
  ]
}
```

**GAPS:**
- ❌ UI design (comment thread sidebar? inline annotations?)
- ❌ Notifications (email when someone replies?)
- ❌ Permissions (kdo může komentovat?)

---

### Feature 3: Version History

**Proposed:**
```json
// Version snapshot
{
  "id": "version-5",
  "dashboardId": "dashboard-456",
  "snapshotData": { /* full dashboard JSON */ },
  "createdBy": "john.doe@company.com",
  "createdAt": "2025-11-07T10:00:00Z",
  "changeDescription": "Added revenue forecast widget",
  "diff": {
    "added": ["widget-789"],
    "removed": [],
    "modified": ["widget-123"]
  }
}
```

**GAPS:**
- ❌ Retention policy (keep versions forever? 30 days?)
- ❌ Diff visualization (how to show changes?)
- ❌ Branching (multiple versions? merge conflicts?)

---

## 🛠️ Proposed Task Breakdown

### T1: Dashboard Sharing (~10h)
- T1.1: Backend API: Generate share token, expiration logic
- T1.2: Public viewer page (no auth required)
- T1.3: Share dialog UI
- T1.4: Anonymization engine

### T2: Comments & Annotations (~10h)
- T2.1: Comment storage (DB schema)
- T2.2: Comment CRUD API
- T2.3: Frontend: Comment thread UI
- T2.4: Notifications integration

### T3: Version History (~10h)
- T3.1: Snapshot storage
- T3.2: Diff calculation
- T3.3: Version history UI
- T3.4: Rollback mechanism

---

## ⚠️ Critical Decisions Needed

1. **User Research:** Je collaboration high-priority feature?
2. **Security Review:** Public link sharing security implications
3. **Storage Cost:** Versions consume storage (how much retention?)

---

**Status:** 📋 **PLANNED** - Needs user research + prioritization

---

**Last Updated:** 7. listopadu 2025
