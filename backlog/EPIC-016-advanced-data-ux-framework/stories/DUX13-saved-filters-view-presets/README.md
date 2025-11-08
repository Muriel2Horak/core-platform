# S13: Saved Filters & View Presets

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟢 **P2 - MEDIUM**  
**Effort:** ~40 hours  
**Sprint:** 3-4  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Data Analyst / Power User,  
**chci** ukládat filtry a pohledy (saved views) a sdílet je s týmem,  
**abych**:
- Uložil **"High Priority Workflows - Alice"** (assignee=Alice, priority=HIGH)
- Sdílel **"Q4 Revenue Dashboard"** s finance týmem (view-only nebo edit)
- Rychle přepnul mezi pohledy: **"My Tasks"** → **"Team Overview"** → **"All Projects"**
- Viděl version history pohledu (kdo změnil filtry, kdy)
- Nastavil **default view** pro první otevření dashboardu

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Save Current Filters as View

**GIVEN** aplikuji 3 filtry (assignee, priority, date range)  
**WHEN** kliknu "Save View"  
**THEN** otevře se save dialog:

```
┌────────────────────────────────────────┐
│ Save Current View                      │
├────────────────────────────────────────┤
│ View Name: *                           │
│ ┌────────────────────────────────────┐ │
│ │ High Priority - Alice              │ │  ← Suggested name
│ └────────────────────────────────────┘ │
│                                        │
│ Description (optional):                │
│ ┌────────────────────────────────────┐ │
│ │ Critical workflows assigned to     │ │
│ │ Alice for Q4 2025                  │ │
│ └────────────────────────────────────┘ │
│                                        │
│ Visibility:                            │
│ ○ Personal (only me)                   │
│ ● Shared (select users/teams)         │
│                                        │
│ Share with:                            │
│ ┌────────────────────────────────────┐ │
│ │ 🔍 Search users or teams...        │ │
│ └────────────────────────────────────┘ │
│                                        │
│ [bob@example.com] [×]                  │
│ [Finance Team] [×]                     │
│                                        │
│ Permissions:                           │
│ ○ Can View (read-only)                 │
│ ● Can Edit (modify filters)            │
│                                        │
│ ☑ Set as my default view               │
│                                        │
│ [Cancel]  [Save View]                  │
└────────────────────────────────────────┘
```

**Saved view contains:**

```typescript
interface SavedView {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  
  // Filters snapshot
  filters: {
    assigneeIds?: string[];
    priorities?: ('HIGH' | 'MEDIUM' | 'LOW')[];
    tenantIds?: string[];
    dateRange?: { from: Date; to: Date };
    tags?: string[];
    customFilters?: Record<string, any>; // Extensible
  };
  
  // Layout snapshot (optional)
  layout?: {
    viewType: 'table' | 'kanban' | 'miro-board' | 'chart';
    columns?: string[]; // Visible columns
    sortBy?: { field: string; direction: 'asc' | 'desc' };
    groupBy?: string;
    swimlanes?: 'priority' | 'tenant' | 'assignee';
  };
  
  // Sharing
  visibility: 'PERSONAL' | 'SHARED';
  sharedWith?: Array<{
    userId?: string;
    teamId?: string;
    permission: 'CAN_VIEW' | 'CAN_EDIT';
  }>;
  
  // Metadata
  isDefault?: boolean; // Default view for this user
  createdAt: string;
  updatedAt: string;
  version: number; // Version tracking
}
```

### AC2: Quick View Switcher

**GIVEN** user má 5 saved views  
**WHEN** otevřu view switcher dropdown  
**THEN** zobrazí se seznam:

```
┌────────────────────────────────────────┐
│ 📁 My Views                            │
├────────────────────────────────────────┤
│ ★ My Tasks (default)                   │  ← Default view (star)
│   3 filters active                     │
│                                        │
│   High Priority - Alice                │
│   5 filters active                     │
│                                        │
│   Q4 Projects                          │
│   2 filters active                     │
│                                        │
├────────────────────────────────────────┤
│ 👥 Shared with Me                      │
├────────────────────────────────────────┤
│   Team Dashboard (by Bob)              │
│   4 filters active · Can Edit          │
│                                        │
│   Finance Overview (by Alice)          │
│   6 filters active · Can View          │
│                                        │
├────────────────────────────────────────┤
│ [+ Create New View]                    │
│ [⚙️ Manage Views...]                   │
└────────────────────────────────────────┘
```

**Click on view:**
- Filtry se aplikují instantly
- URL se změní: `/dashboard?view=my-tasks-123`
- Notification: "View 'My Tasks' applied (3 filters)"

### AC3: Edit & Version History

**GIVEN** saved view existuje  
**WHEN** upravím filtry a kliknu "Update View"  
**THEN**:
- Vytvoří se nová verze (version++)
- Version history trackuje změny

**Version history:**

```
┌────────────────────────────────────────┐
│ View: High Priority - Alice            │
│ Version History                        │
├────────────────────────────────────────┤
│ Version 3 (Current)                    │
│ ├─ 2025-11-08 14:30 by alice@         │
│ └─ Added filter: Priority = HIGH       │
│                                        │
│ Version 2                              │
│ ├─ 2025-11-07 10:15 by alice@         │
│ └─ Changed date range: Last 30 days   │
│                                        │
│ Version 1                              │
│ ├─ 2025-11-01 09:00 by alice@         │
│ └─ Created view                        │
│                                        │
│ [Restore Version 2]  [Compare]         │
└────────────────────────────────────────┘
```

**Restore previous version:**

```
┌────────────────────────────────────────┐
│ Restore Version 2?                     │
│                                        │
│ This will create a new version (v4)   │
│ with filters from v2                  │
│                                        │
│ Changes:                               │
│ - Date range: Last 7 days → Last 30   │
│ - Priority filter: HIGH → (removed)   │
│                                        │
│ [Cancel]  [Restore]                    │
└────────────────────────────────────────┘
```

### AC4: Share View with Permissions

**GIVEN** saved view owner  
**WHEN** kliknu "Share"  
**THEN** můžu přidat users/teams s permissions:

**Share dialog:**

```
┌────────────────────────────────────────┐
│ Share "High Priority - Alice"          │
├────────────────────────────────────────┤
│ Share with:                            │
│ ┌────────────────────────────────────┐ │
│ │ 🔍 bob@example.com                 │ │  ← Search
│ └────────────────────────────────────┘ │
│                                        │
│ bob@example.com     [Can View ▼] [×]  │
│ charlie@example.com [Can Edit ▼] [×]  │
│ Finance Team        [Can View ▼] [×]  │
│                                        │
│ Share Link:                            │
│ https://app.com/v/abc123  [Copy] 🔗   │
│                                        │
│ Anyone with the link: [Can View ▼]    │
│                                        │
│ [Cancel]  [Share]                      │
└────────────────────────────────────────┘
```

**Permission matrix:**

| Permission | Apply View | Edit Filters | Update View | Delete View | Share |
|------------|-----------|-------------|-------------|-------------|-------|
| **Owner**  | ✅        | ✅          | ✅          | ✅          | ✅    |
| **Can Edit** | ✅      | ✅          | ✅          | ❌          | ❌    |
| **Can View** | ✅      | ❌ (read-only) | ❌       | ❌          | ❌    |

### AC5: Default View on Load

**GIVEN** user má default view nastaven  
**WHEN** otevřu dashboard  
**THEN**:
- Default view se aplikuje automaticky
- URL: `/dashboard?view=my-tasks-123`
- Notification: "Default view 'My Tasks' applied"

**Set default view:**

```
View dropdown → ⋮ More → Set as Default

┌────────────────────────────────────────┐
│ Set "High Priority - Alice" as default?│
│                                        │
│ This view will be applied automatically│
│ when you open the dashboard.           │
│                                        │
│ Current default: "My Tasks"            │
│                                        │
│ [Cancel]  [Set as Default]             │
└────────────────────────────────────────┘
```

### AC6: View Templates (Pre-built Views)

**GIVEN** nový user bez saved views  
**WHEN** otevřu view switcher  
**THEN** zobrazí se template gallery:

```
┌────────────────────────────────────────┐
│ 🎨 View Templates                      │
├────────────────────────────────────────┤
│ ┌────────────────────────────────────┐ │
│ │ 📋 My Assigned Tasks               │ │
│ │ Filters: Assignee = You            │ │
│ │ Layout: Table view                 │ │
│ │ [Use Template]                     │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🔥 High Priority Items             │ │
│ │ Filters: Priority = HIGH           │ │
│ │ Layout: Kanban board               │ │
│ │ [Use Template]                     │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📊 Team Dashboard                  │ │
│ │ Filters: Team = Your team          │ │
│ │ Layout: Chart view                 │ │
│ │ [Use Template]                     │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘
```

**Click "Use Template":**
- Creates personal copy of template
- User can customize and save

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Saved View Backend API** (12h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/dataview/model/SavedView.java
package cz.muriel.core.dataview.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_views")
@Data
public class SavedView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long ownerId;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String filtersJson; // Serialized filters

    @Column(columnDefinition = "jsonb")
    private String layoutJson; // Serialized layout config

    @Enumerated(EnumType.STRING)
    private ViewVisibility visibility;

    @Column
    private Boolean isDefault = false;

    @Column
    private Integer version = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

enum ViewVisibility {
    PERSONAL,
    SHARED
}
```

**Service:**

```java
// backend/src/main/java/cz/muriel/core/dataview/service/SavedViewService.java
@Service
public class SavedViewService {

    private final SavedViewRepository viewRepository;
    private final SavedViewShareRepository shareRepository;

    /**
     * Create new saved view
     */
    @Transactional
    public SavedView createView(SavedViewRequest request, Long userId) {
        var view = new SavedView();
        view.setName(request.getName());
        view.setDescription(request.getDescription());
        view.setOwnerId(userId);
        view.setFiltersJson(serializeFilters(request.getFilters()));
        view.setLayoutJson(serializeLayout(request.getLayout()));
        view.setVisibility(request.getVisibility());
        view.setCreatedAt(LocalDateTime.now());
        view.setUpdatedAt(LocalDateTime.now());

        var savedView = viewRepository.save(view);

        // Create shares if visibility = SHARED
        if (request.getVisibility() == ViewVisibility.SHARED) {
            for (var shareRequest : request.getSharedWith()) {
                var share = new SavedViewShare();
                share.setViewId(savedView.getId());
                share.setUserId(shareRequest.getUserId());
                share.setTeamId(shareRequest.getTeamId());
                share.setPermission(shareRequest.getPermission());
                shareRepository.save(share);
            }
        }

        return savedView;
    }

    /**
     * Update saved view (creates new version)
     */
    @Transactional
    public SavedView updateView(Long viewId, SavedViewRequest request, Long userId) {
        var view = viewRepository.findById(viewId).orElseThrow();

        // Check permission
        if (!canEdit(view, userId)) {
            throw new ForbiddenException("No edit permission");
        }

        // Create version history entry
        var version = new SavedViewVersion();
        version.setViewId(viewId);
        version.setVersionNumber(view.getVersion());
        version.setFiltersJson(view.getFiltersJson());
        version.setLayoutJson(view.getLayoutJson());
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);

        // Update view
        view.setFiltersJson(serializeFilters(request.getFilters()));
        view.setLayoutJson(serializeLayout(request.getLayout()));
        view.setVersion(view.getVersion() + 1);
        view.setUpdatedAt(LocalDateTime.now());

        return viewRepository.save(view);
    }

    /**
     * Get views accessible to user
     */
    public List<SavedView> getAccessibleViews(Long userId) {
        var ownedViews = viewRepository.findByOwnerId(userId);
        var sharedViews = getSharedViews(userId);

        return Stream.concat(ownedViews.stream(), sharedViews.stream())
            .collect(Collectors.toList());
    }

    private boolean canEdit(SavedView view, Long userId) {
        if (view.getOwnerId().equals(userId)) return true;

        var share = shareRepository.findByViewIdAndUserId(view.getId(), userId);
        return share.isPresent() && share.get().getPermission() == SharePermission.CAN_EDIT;
    }
}
```

**Deliverable:** Backend API for saved views

---

#### **T2: View Switcher UI** (10h)

**Implementation:**

```typescript
// frontend/src/components/dataview/ViewSwitcher.tsx
import React, { useState } from 'react';
import {
  Menu,
  MenuItem,
  Button,
  Divider,
  ListItemIcon,
  ListItemText,
  Typography
} from '@mui/material';
import { Star, People, Add, Settings } from '@mui/icons-material';

interface ViewSwitcherProps {
  currentView: SavedView | null;
  onViewChange: (view: SavedView) => void;
}

export const ViewSwitcher: React.FC<ViewSwitcherProps> = ({
  currentView,
  onViewChange
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const { data: views } = useQuery(['saved-views'], fetchSavedViews);

  const myViews = views?.filter(v => v.isOwner) || [];
  const sharedViews = views?.filter(v => !v.isOwner) || [];

  return (
    <>
      <Button
        variant="outlined"
        onClick={(e) => setAnchorEl(e.currentTarget)}
        startIcon={currentView?.isDefault ? <Star /> : undefined}
      >
        {currentView?.name || 'Select View'}
      </Button>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <Typography variant="caption" sx={{ px: 2, py: 1, color: 'text.secondary' }}>
          📁 My Views
        </Typography>

        {myViews.map(view => (
          <MenuItem
            key={view.id}
            onClick={() => {
              onViewChange(view);
              setAnchorEl(null);
            }}
            selected={view.id === currentView?.id}
          >
            <ListItemIcon>
              {view.isDefault && <Star fontSize="small" />}
            </ListItemIcon>
            <ListItemText
              primary={view.name}
              secondary={`${view.filterCount} filters active`}
            />
          </MenuItem>
        ))}

        <Divider />

        <Typography variant="caption" sx={{ px: 2, py: 1, color: 'text.secondary' }}>
          👥 Shared with Me
        </Typography>

        {sharedViews.map(view => (
          <MenuItem
            key={view.id}
            onClick={() => {
              onViewChange(view);
              setAnchorEl(null);
            }}
          >
            <ListItemIcon>
              <People fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary={view.name}
              secondary={`by ${view.ownerName} · ${view.permission}`}
            />
          </MenuItem>
        ))}

        <Divider />

        <MenuItem onClick={() => { /* Open create view dialog */ }}>
          <ListItemIcon>
            <Add fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Create New View" />
        </MenuItem>

        <MenuItem onClick={() => { /* Open manage views */ }}>
          <ListItemIcon>
            <Settings fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Manage Views..." />
        </MenuItem>
      </Menu>
    </>
  );
};
```

**Deliverable:** View switcher dropdown

---

#### **T3: Save View Dialog** (8h)

**Implementation:**

```typescript
// frontend/src/components/dataview/SaveViewDialog.tsx
import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  RadioGroup,
  FormControlLabel,
  Radio,
  Checkbox,
  Button
} from '@mui/material';

interface SaveViewDialogProps {
  open: boolean;
  currentFilters: any;
  currentLayout: any;
  onSave: (view: SavedViewRequest) => void;
  onClose: () => void;
}

export const SaveViewDialog: React.FC<SaveViewDialogProps> = ({
  open,
  currentFilters,
  currentLayout,
  onSave,
  onClose
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility] = useState<'PERSONAL' | 'SHARED'>('PERSONAL');
  const [isDefault, setIsDefault] = useState(false);

  const handleSave = () => {
    onSave({
      name,
      description,
      filters: currentFilters,
      layout: currentLayout,
      visibility,
      isDefault
    });
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Save Current View</DialogTitle>

      <DialogContent>
        <TextField
          label="View Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          fullWidth
          required
          sx={{ mb: 2, mt: 1 }}
        />

        <TextField
          label="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          fullWidth
          multiline
          rows={2}
          sx={{ mb: 2 }}
        />

        <RadioGroup value={visibility} onChange={(e) => setVisibility(e.target.value as any)}>
          <FormControlLabel value="PERSONAL" control={<Radio />} label="Personal (only me)" />
          <FormControlLabel value="SHARED" control={<Radio />} label="Shared (with users/teams)" />
        </RadioGroup>

        <FormControlLabel
          control={<Checkbox checked={isDefault} onChange={(e) => setIsDefault(e.target.checked)} />}
          label="Set as my default view"
          sx={{ mt: 2 }}
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSave} variant="contained" disabled={!name}>
          Save View
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

**Deliverable:** Save view dialog

---

#### **T4: Version History** (6h)

**Implementation:**

```typescript
// frontend/src/components/dataview/ViewVersionHistory.tsx
import React from 'react';
import { Box, Typography, Button, List, ListItem, ListItemText } from '@mui/material';

export const ViewVersionHistory: React.FC<{ viewId: string }> = ({ viewId }) => {
  const { data: versions } = useQuery(['view-versions', viewId], () =>
    fetch(`/api/saved-views/${viewId}/versions`).then(r => r.json())
  );

  const handleRestore = async (versionNumber: number) => {
    await fetch(`/api/saved-views/${viewId}/restore/${versionNumber}`, {
      method: 'POST'
    });
  };

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 2 }}>Version History</Typography>

      <List>
        {versions?.map((version: any) => (
          <ListItem key={version.versionNumber}>
            <ListItemText
              primary={`Version ${version.versionNumber} ${version.isCurrent ? '(Current)' : ''}`}
              secondary={
                <>
                  {version.createdAt} by {version.createdBy}
                  <br />
                  {version.changeDescription}
                </>
              }
            />
            {!version.isCurrent && (
              <Button size="small" onClick={() => handleRestore(version.versionNumber)}>
                Restore
              </Button>
            )}
          </ListItem>
        ))}
      </List>
    </Box>
  );
};
```

**Deliverable:** Version history UI

---

#### **T5: Testing** (4h)

**E2E tests:**

```typescript
// e2e/specs/dataview/saved-views.spec.ts
test('Save current filters as view', async ({ page }) => {
  await page.goto('/dashboard');

  // Apply filters
  await page.fill('input[name="assignee"]', 'Alice');
  await page.click('button:has-text("High Priority")');

  // Save view
  await page.click('button:has-text("Save View")');
  await page.fill('input[label="View Name"]', 'My Test View');
  await page.click('button:has-text("Save View")');

  // Verify saved
  await expect(page.locator('text=View saved successfully')).toBeVisible();
});

test('Apply saved view', async ({ page }) => {
  await page.goto('/dashboard');

  // Open view switcher
  await page.click('button:has-text("Select View")');

  // Select saved view
  await page.click('text=My Test View');

  // Verify filters applied
  await expect(page.locator('text=3 filters active')).toBeVisible();
});
```

**Deliverable:** E2E tests for saved views

---

## 📊 SUCCESS METRICS

- ✅ View apply < 500ms
- ✅ Save view < 1s
- ✅ 60%+ users create at least 1 saved view
- ✅ 30%+ views are shared with team

---

## 🔗 DEPENDENCIES

- **S2:** Advanced Filtering (filter logic)
- **S10:** Layout Sharing (sharing mechanism)
- **EPIC-003:** RBAC (permissions)

---

**Status:** 📋 TODO  
**Effort:** ~40 hours (~1 sprint)  
**Next:** S14 (Miro-style Board)
