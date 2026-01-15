---
id: S10
epic: EPIC-004-reporting-analytics-infrastructure
title: "Collaboration Features"
priority: P3
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "34 hours"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/reporting
    - frontend/src/components/Reporting
  test_paths:
    - backend/src/test/java/cz/muriel/core/reporting
    - frontend/src/test
  docs_paths:
    - backlog/EPIC-004-reporting-analytics-infrastructure/stories/REP10-collaboration-features/README.md
    - backlog/EPIC-004-reporting-analytics-infrastructure/README.md
---

# S10: Collaboration Features

**Status:** 📋 **PLANNED** (Phase 10 - Post-MVP)  
**Priority:** P3 (Nice-to-Have)  
**Effort:** ~34 hodin (4 tasky)  
**Dependencies:** Sharing + comments + audit log + UI komponenty

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

## 📋 Story Description

Jako **uzivatel/report owner** potrebuji **sdilet dashboardy, komentovat a vratit zmeny**, abych **mohl spolupracovat bez ztraty kontextu**.

## ✅ Acceptance Criteria

1. **Sdileni dashboardu**
   - Dashboard lze sdilet pres link nebo s vybranymi uzivateli/rolemi.
   - Sdileni respektuje expiraci a minimalni prava (view/comment/edit).

2. **Komentare a anotace**
   - Komentare lze pridat na dashboard nebo konkretni widget.
   - Zobrazujeme historii + notifikace na odpoved.

3. **Version history**
   - Ulozena je historie zmen (min. 10 verzi).
   - Umoznen je rollback na vybranou verzi.

4. **Audit a tenant izolace**
   - Sdileni i komentare jsou auditovane a tenant-scoped.

## Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Shared Dashboards](subtasks/T1-shared-dashboards.md) | 12h | EPIC-014 S4, S9 |
| 2 | [T2: Comments & Annotations](subtasks/T2-comments-annotations.md) | 9h | T1 |
| 3 | [T3: Version History](subtasks/T3-version-history.md) | 9h | T1 |
| 4 | [T4: Testing](subtasks/T4-testing.md) | 4h | T1, T2, T3 |

## 🔗 Závislosti

- EPIC-014 S3, S4, S9 (UI komponenty)
- Audit log / permissions (backend)

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

### T1: Dashboard Sharing (~12h)
- T1.1: Backend API: Generate share token, expiration logic
- T1.2: Public viewer page (no auth required)
- T1.3: Share dialog UI
- T1.4: Anonymization engine

### T2: Comments & Annotations (~9h)
- T2.1: Comment storage (DB schema)
- T2.2: Comment CRUD API
- T2.3: Frontend: Comment thread UI
- T2.4: Notifications integration

### T3: Version History (~9h)
- T3.1: Snapshot storage
- T3.2: Diff calculation
- T3.3: Version history UI
- T3.4: Rollback mechanism

### T4: Testing (~4h)
- T4.1: Share permission tests
- T4.2: Comment + notification tests
- T4.3: Version history/rollback tests

---

## ⚠️ Critical Decisions Needed

1. **User Research:** Je collaboration high-priority feature?
2. **Security Review:** Public link sharing security implications
3. **Storage Cost:** Versions consume storage (how much retention?)

---

**Status:** 📋 **PLANNED** - Needs user research + prioritization

---

**Last Updated:** 7. listopadu 2025
