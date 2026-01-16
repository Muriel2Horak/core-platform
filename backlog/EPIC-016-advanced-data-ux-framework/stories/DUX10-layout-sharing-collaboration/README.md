---
id: S10
epic: EPIC-016-advanced-data-ux-framework
title: "Layout Sharing & Collaboration"
priority: P2
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "45 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-016-advanced-data-ux-framework/stories/DUX10-layout-sharing-collaboration/README.md
    - backlog/EPIC-016-advanced-data-ux-framework/README.md
---

# S10: Layout Sharing & Collaboration

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 🟡 **READY**
**Priority:** 🟢 **P2 - MEDIUM**  
**Effort:** ~45 hours  
**Sprint:** 2-3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Dashboard Creator / Team Lead,  
**chci** share dashboards a layouts s kolegy (view-only nebo edit permissions),  
**abych**:
- Sdílel "Monthly Revenue Dashboard" s finance týmem (view-only)
- Povolil Admin týmu editovat "System Health Dashboard"
- Viděl version history dashboardu (kdo co změnil, kdy)
- Přidal comments k layoutu ("Please add KPI tile for churn rate")

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Share Dashboard Dialog

**GIVEN** uživatel vlastní dashboard "My Analytics Dashboard"  
**WHEN** kliknu "Share" button  
**THEN** otevře se share dialog:

```
┌──────────────────────────────────────────────┐
│ Share "My Analytics Dashboard"               │
├──────────────────────────────────────────────┤
│ Share with:                                  │
│ ┌──────────────────────────────────────────┐ │
│ │ 🔍 Search users or teams...               │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ alice@example.com         [Can Edit ▼] [×]  │
│ bob@example.com           [Can View ▼] [×]  │
│ Finance Team              [Can View ▼] [×]  │
│                                              │
│ [ + Add People ]                             │
│                                              │
│ Share Link:                                  │
│ https://app.com/d/abc123  [Copy Link] 🔗    │
│                                              │
│ Anyone with the link:  [Can View ▼]         │
│                                              │
│         [Cancel]  [Share]                    │
└──────────────────────────────────────────────┘
```

**Permissions:**
- **Owner** (creator): Full control, can delete
- **Can Edit**: Modify layout, add/remove widgets
- **Can View**: Read-only access

### AC2: Share Link Generation

**GIVEN** shared dashboard  
**WHEN** generuji share link  
**THEN**:
- Backend vytvoří unique token: `/dashboards/shared/abc123def456`
- Link permissions:
  - **Can View**: Anyone with link can view (no login required for public share)
  - **Restricted**: Only invited users can access (login required)

**Backend API:**

```http
POST /api/dashboards/:id/share
{
  "userIds": [123, 456],
  "teamIds": [789],
  "permission": "CAN_VIEW",
  "linkAccess": "RESTRICTED"
}

→ Response:
{
  "shareToken": "abc123def456",
  "shareUrl": "https://app.com/dashboards/shared/abc123def456",
  "expiresAt": "2025-12-31T23:59:59Z"
}
```

### AC3: Permission Management

**GIVEN** dashboard shared with alice@example.com (Can Edit)  
**WHEN** owner změní permission na "Can View"  
**THEN**:
- Alice CANNOT edit widgets (toolbar hidden)
- Alice CAN view & export data
- Audit log: "Permission changed: alice@example.com Can Edit → Can View"

**Permission matrix:**

| Permission | View Dashboard | Export Data | Edit Layout | Add Widgets | Delete Dashboard |
|------------|----------------|-------------|-------------|-------------|------------------|
| **Owner**  | ✅             | ✅          | ✅          | ✅          | ✅               |
| **Can Edit** | ✅           | ✅          | ✅          | ✅          | ❌               |
| **Can View** | ✅           | ✅          | ❌          | ❌          | ❌               |

### AC4: Version History

**GIVEN** dashboard byl 5× editován  
**WHEN** otevřu "Version History"  
**THEN** zobrazí se timeline:

```
Version 5 (Current)
├─ 2025-01-20 14:30 by alice@example.com
└─ Added "Revenue Trend" chart

Version 4
├─ 2025-01-19 10:15 by bob@example.com
└─ Removed "Deprecated Widget"

Version 3
├─ 2025-01-18 09:00 by alice@example.com
└─ Changed layout: 2-column → 3-column grid

[ Restore Version 3 ]  [ Compare Versions ]
```

**Actions:**
- **Restore**: Rollback k previous version (vytvoří nový snapshot)
- **Compare**: Side-by-side diff (červené = removed, zelené = added)

### AC5: Comments & Collaboration

**GIVEN** shared dashboard  
**WHEN** člen týmu přidá comment  
**THEN**:
- Comment připnut k specific widget nebo layout area
- Notifikace pro dashboard owner
- Comment thread (replies podporovány)

**Comment UI:**

```typescript
interface Comment {
  id: string;
  dashboardId: string;
  widgetId?: string; // Optional: comment on specific widget
  author: User;
  content: string;
  createdAt: string;
  replies: Comment[];
}
```

**Comment display:**

```
💬 3 Comments

alice@example.com · 2 hours ago
"Please add a KPI tile for churn rate. It's missing from the overview."
  └─ bob@example.com · 1 hour ago
     "Good idea! I'll add it today."

charlie@example.com · 1 day ago (on "Revenue Chart" widget)
"Can we change this to a line chart instead of bar?"
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Share Dialog UI** (10h)

**Implementation:**

```typescript
// frontend/src/components/sharing/ShareDialog.tsx
import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Select,
  MenuItem,
  Chip,
  IconButton,
  Box
} from '@mui/material';
import { Close, ContentCopy } from '@mui/icons-material';

interface ShareDialogProps {
  dashboardId: string;
  dashboardName: string;
  onClose: () => void;
}

export const ShareDialog: React.FC<ShareDialogProps> = ({ dashboardId, dashboardName, onClose }) => {
  const [sharedWith, setSharedWith] = useState<Array<{ userId: string; email: string; permission: string }>>([]);
  const [shareLink, setShareLink] = useState<string | null>(null);

  const handleAddUser = (email: string) => {
    setSharedWith([...sharedWith, { userId: '', email, permission: 'CAN_VIEW' }]);
  };

  const handleGenerateLink = async () => {
    const response = await fetch(`/api/dashboards/${dashboardId}/share`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userIds: sharedWith.map(u => u.userId),
        linkAccess: 'RESTRICTED'
      })
    });

    const { shareUrl } = await response.json();
    setShareLink(shareUrl);
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(shareLink!);
  };

  return (
    <Dialog open fullWidth maxWidth="sm" onClose={onClose}>
      <DialogTitle>
        Share "{dashboardName}"
        <IconButton onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
          <Close />
        </IconButton>
      </DialogTitle>

      <DialogContent>
        {/* User search input */}
        <TextField
          label="Search users or teams..."
          fullWidth
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleAddUser((e.target as HTMLInputElement).value);
              (e.target as HTMLInputElement).value = '';
            }
          }}
          sx={{ mb: 2 }}
        />

        {/* Shared users list */}
        {sharedWith.map((user, index) => (
          <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Box sx={{ flex: 1 }}>{user.email}</Box>
            <Select
              value={user.permission}
              onChange={(e) => {
                const updated = [...sharedWith];
                updated[index].permission = e.target.value;
                setSharedWith(updated);
              }}
              size="small"
              sx={{ mr: 1 }}
            >
              <MenuItem value="CAN_VIEW">Can View</MenuItem>
              <MenuItem value="CAN_EDIT">Can Edit</MenuItem>
            </Select>
            <IconButton
              size="small"
              onClick={() => setSharedWith(sharedWith.filter((_, i) => i !== index))}
            >
              <Close fontSize="small" />
            </IconButton>
          </Box>
        ))}

        {/* Share link */}
        {shareLink && (
          <Box sx={{ mt: 3 }}>
            <TextField
              label="Share Link"
              value={shareLink}
              fullWidth
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <IconButton onClick={handleCopyLink}>
                    <ContentCopy />
                  </IconButton>
                )
              }}
            />
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={handleGenerateLink}>
          Share
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

**Deliverable:** Share dialog UI

---

#### **T2: Backend Permission System** (15h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/dashboard/model/DashboardShare.java
package cz.muriel.core.dashboard.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_shares")
@Data
public class DashboardShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dashboardId;

    @Column
    private Long userId; // Null if shared via link

    @Column
    private Long teamId; // Null if shared with individual user

    @Enumerated(EnumType.STRING)
    private SharePermission permission;

    @Column(unique = true)
    private String shareToken; // For share links

    @Enumerated(EnumType.STRING)
    private LinkAccess linkAccess; // PUBLIC, RESTRICTED

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

enum SharePermission {
    OWNER,
    CAN_EDIT,
    CAN_VIEW
}

enum LinkAccess {
    PUBLIC,      // Anyone with link (no login required)
    RESTRICTED   // Only authenticated users
}
```

**Service:**

```java
// backend/src/main/java/cz/muriel/core/dashboard/service/DashboardSharingService.java
@Service
public class DashboardSharingService {

    private final DashboardShareRepository shareRepository;

    /**
     * Share dashboard with users/teams
     */
    @Transactional
    public DashboardShareDTO shareDashboard(Long dashboardId, ShareRequest request) {
        // Create individual user shares
        for (Long userId : request.getUserIds()) {
            var share = new DashboardShare();
            share.setDashboardId(dashboardId);
            share.setUserId(userId);
            share.setPermission(request.getPermission());
            shareRepository.save(share);
        }

        // Generate share link
        var linkShare = new DashboardShare();
        linkShare.setDashboardId(dashboardId);
        linkShare.setShareToken(generateShareToken());
        linkShare.setPermission(request.getPermission());
        linkShare.setLinkAccess(request.getLinkAccess());
        linkShare.setExpiresAt(LocalDateTime.now().plusDays(30)); // 30-day expiry
        shareRepository.save(linkShare);

        return new DashboardShareDTO(
            linkShare.getShareToken(),
            "/dashboards/shared/" + linkShare.getShareToken(),
            linkShare.getExpiresAt()
        );
    }

    /**
     * Check if user has permission to access dashboard
     */
    public SharePermission getUserPermission(Long dashboardId, Long userId) {
        // Check direct user share
        var userShare = shareRepository.findByDashboardIdAndUserId(dashboardId, userId);
        if (userShare.isPresent()) {
            return userShare.get().getPermission();
        }

        // Check team shares
        var user = userRepository.findById(userId).orElseThrow();
        var teamShares = shareRepository.findByDashboardIdAndTeamIdIn(dashboardId, user.getTeamIds());
        if (!teamShares.isEmpty()) {
            return teamShares.stream()
                .map(DashboardShare::getPermission)
                .max(Comparator.comparingInt(p -> p.ordinal())) // Highest permission wins
                .orElse(null);
        }

        return null; // No access
    }

    private String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

**Deliverable:** Backend permission system

---

#### **T3: Version History** (12h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/dashboard/model/DashboardVersion.java
@Entity
@Table(name = "dashboard_versions")
@Data
public class DashboardVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dashboardId;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "jsonb")
    private String layoutData; // Snapshot of layout

    @Column(nullable = false)
    private Long createdBy;

    private String changeDescription;
    private LocalDateTime createdAt;
}
```

**Service:**

```java
@Service
public class DashboardVersionService {

    /**
     * Create version snapshot when layout changes
     */
    @Transactional
    public DashboardVersion createVersion(Long dashboardId, String layoutData, Long userId, String description) {
        var latestVersion = versionRepository.findTopByDashboardIdOrderByVersionNumberDesc(dashboardId);
        var versionNumber = latestVersion.map(v -> v.getVersionNumber() + 1).orElse(1);

        var version = new DashboardVersion();
        version.setDashboardId(dashboardId);
        version.setVersionNumber(versionNumber);
        version.setLayoutData(layoutData);
        version.setCreatedBy(userId);
        version.setChangeDescription(description);
        version.setCreatedAt(LocalDateTime.now());

        return versionRepository.save(version);
    }

    /**
     * Restore previous version
     */
    @Transactional
    public void restoreVersion(Long dashboardId, Integer versionNumber, Long userId) {
        var version = versionRepository.findByDashboardIdAndVersionNumber(dashboardId, versionNumber)
            .orElseThrow();

        // Create new version with restored data
        createVersion(dashboardId, version.getLayoutData(), userId, "Restored from version " + versionNumber);

        // Update current dashboard layout
        var dashboard = dashboardRepository.findById(dashboardId).orElseThrow();
        dashboard.setLayoutData(version.getLayoutData());
        dashboardRepository.save(dashboard);
    }
}
```

**Deliverable:** Version history with restore

---

#### **T4: Comments System** (8h)

**Implementation:**

```typescript
// frontend/src/components/sharing/CommentsPanel.tsx
import React, { useState } from 'react';
import { Box, TextField, Button, Avatar, Typography } from '@mui/material';

interface Comment {
  id: string;
  author: { name: string; avatar: string };
  content: string;
  createdAt: string;
  replies: Comment[];
}

export const CommentsPanel: React.FC<{ dashboardId: string; widgetId?: string }> = ({
  dashboardId,
  widgetId
}) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState('');

  const handleAddComment = async () => {
    await fetch(`/api/dashboards/${dashboardId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ widgetId, content: newComment })
    });
    setNewComment('');
    // Reload comments
  };

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h6">💬 Comments ({comments.length})</Typography>

      {comments.map(comment => (
        <Box key={comment.id} sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Avatar src={comment.author.avatar} sx={{ width: 32, height: 32 }} />
            <Box>
              <Typography variant="subtitle2">{comment.author.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                {comment.createdAt}
              </Typography>
              <Typography variant="body2" sx={{ mt: 0.5 }}>
                {comment.content}
              </Typography>
            </Box>
          </Box>
        </Box>
      ))}

      <TextField
        multiline
        rows={2}
        value={newComment}
        onChange={(e) => setNewComment(e.target.value)}
        placeholder="Add a comment..."
        fullWidth
        sx={{ mt: 2 }}
      />
      <Button onClick={handleAddComment} variant="contained" sx={{ mt: 1 }}>
        Post Comment
      </Button>
    </Box>
  );
};
```

**Deliverable:** Comments system

---

## 🧪 TESTING

```typescript
// e2e/specs/sharing/dashboard-sharing.spec.ts
test('Share dashboard with user', async ({ page }) => {
  await page.goto('/dashboards/1');
  await page.click('button:has-text("Share")');

  // Add user
  await page.fill('input[placeholder="Search users..."]', 'alice@example.com');
  await page.keyboard.press('Enter');

  // Set permission
  await page.selectOption('select', 'CAN_EDIT');

  // Generate link
  await page.click('button:has-text("Share")');

  // Verify link generated
  await expect(page.locator('text=/https:\/\/.*\/shared\/.*/")).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Share link generation < 500ms
- ✅ Permission changes apply instantly (<1s)
- ✅ 50% teams use sharing feature (good collaboration!)

---

## 🔗 DEPENDENCIES

- **EPIC-003:** RBAC (permissions)
- **S1:** DataView (view-only mode)

---

**Status:** 📋 TODO  
**Next:** S11: EPIC-014 Integration
