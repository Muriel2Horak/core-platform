# S4: Role-Based Default Layouts

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P1 - HIGH**  
**Effort:** ~50 hours  
**Sprint:** 2-3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Admin / System Architect,  
**chci** definovat default dashboard layouts pro různé role (ADMIN, TENANT_ADMIN, ANALYST, VIEWER),  
**abych**:
- Nový uživatel s rolí ANALYST viděl immediately relevantní data (analytics widgets)
- VIEWER měl read-only dashboard s essential KPIs
- ADMIN měl admin control panel widgets
- Layouts byly verzované a migrovatelné při změně role

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Default Layout Templates per Role

**GIVEN** role definitions v EPIC-003 (RBAC)  
**WHEN** definuji default layouts  
**THEN** každá role má JSON template:

```json
// layouts/default-admin.json
{
  "role": "ADMIN",
  "version": "1.0.0",
  "widgets": [
    {"id": "users-table", "x": 0, "y": 0, "w": 12, "h": 6, "type": "table"},
    {"id": "tenant-stats", "x": 0, "y": 6, "w": 6, "h": 4, "type": "chart"},
    {"id": "system-health", "x": 6, "y": 6, "w": 6, "h": 4, "type": "kpi"}
  ]
}

// layouts/default-analyst.json
{
  "role": "ANALYST",
  "version": "1.0.0",
  "widgets": [
    {"id": "revenue-chart", "x": 0, "y": 0, "w": 8, "h": 5, "type": "chart"},
    {"id": "top-products", "x": 8, "y": 0, "w": 4, "h": 5, "type": "table"},
    {"id": "kpi-tiles", "x": 0, "y": 5, "w": 12, "h": 2, "type": "kpi-row"}
  ]
}

// layouts/default-viewer.json
{
  "role": "VIEWER",
  "version": "1.0.0",
  "widgets": [
    {"id": "dashboard-summary", "x": 0, "y": 0, "w": 12, "h": 3, "type": "kpi-row"},
    {"id": "recent-activity", "x": 0, "y": 3, "w": 12, "h": 5, "type": "table", "readonly": true}
  ]
}
```

### AC2: Auto-Assignment on First Login

**GIVEN** nový uživatel s rolí ANALYST  
**WHEN** first login  
**THEN**:
- Backend detekuje: `user.layouts.length === 0`
- Načte `layouts/default-analyst.json`
- Vytvoří záznam:

```sql
INSERT INTO user_layouts (user_id, layout_name, layout_data, is_default)
VALUES (
  123,
  'My Dashboard',
  '{"widgets": [...]}',  -- From default-analyst.json
  true
);
```

- Frontend zobrazí dashboard s analyst widgets

### AC3: Layout Versioning

**GIVEN** existující default layout v1.0.0  
**WHEN** vydám novou verzi v1.1.0 (přidám widget "predictive-analytics")  
**THEN**:
- Uložím jako nový template: `default-analyst-v1.1.0.json`
- Existing uživatelé s v1.0.0 dostanou migration prompt:

```
┌─────────────────────────────────────────┐
│ New Dashboard Features Available!      │
│                                         │
│ Your "Analyst Dashboard" can be         │
│ upgraded to v1.1.0 with new widgets:    │
│                                         │
│ • Predictive Analytics Chart            │
│ • AI-Powered Insights                   │
│                                         │
│ [ Upgrade Now ]  [ Keep Current ]      │
└─────────────────────────────────────────┘
```

- Pokud upgrade → merge v1.1.0 widgets do existujícího layoutu (preserve custom changes)

### AC4: Role Change Migration

**GIVEN** uživatel s rolí ANALYST (má analyst layout)  
**WHEN** admin změní roli na ADMIN  
**THEN**:
- Při next loginu detekce: `user.role !== layout.role`
- Nabídka migrace:

```
Your role has changed from ANALYST to ADMIN.
Would you like to switch to the default ADMIN dashboard?

[ Use ADMIN Dashboard ]  [ Keep My Custom Layout ]
```

- Pokud Use ADMIN → backup current layout + load default-admin.json
- Pokud Keep → keep existing (ignore role change)

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Default Layout Templates (JSON)** (8h)

**Cíl:** Vytvořit 4 default templates pro ADMIN, TENANT_ADMIN, ANALYST, VIEWER

**Implementation:**

```json
// backend/src/main/resources/layouts/default-admin.json
{
  "role": "ADMIN",
  "version": "1.0.0",
  "name": "Admin Control Panel",
  "description": "System administration dashboard with user management and health monitoring",
  "widgets": [
    {
      "id": "users-table",
      "type": "table",
      "title": "User Management",
      "dataSource": "Users",
      "x": 0,
      "y": 0,
      "w": 12,
      "h": 6,
      "config": {
        "columns": ["email", "role", "status", "lastLogin"],
        "actions": ["edit", "delete", "impersonate"]
      }
    },
    {
      "id": "tenant-stats",
      "type": "chart",
      "title": "Tenant Statistics",
      "dataSource": "Tenants",
      "x": 0,
      "y": 6,
      "w": 6,
      "h": 4,
      "config": {
        "chartType": "bar",
        "measure": "userCount",
        "dimension": "tenantName"
      }
    },
    {
      "id": "system-health",
      "type": "kpi",
      "title": "System Health",
      "x": 6,
      "y": 6,
      "w": 6,
      "h": 4,
      "config": {
        "metrics": [
          {"label": "Active Users", "value": "{{activeUsers}}", "trend": "+5%"},
          {"label": "API Uptime", "value": "99.9%", "status": "success"},
          {"label": "DB Connections", "value": "45/100", "status": "warning"}
        ]
      }
    }
  ]
}
```

```json
// backend/src/main/resources/layouts/default-analyst.json
{
  "role": "ANALYST",
  "version": "1.0.0",
  "name": "Analytics Dashboard",
  "widgets": [
    {
      "id": "revenue-chart",
      "type": "chart",
      "title": "Revenue Trend",
      "dataSource": "Revenue",
      "x": 0,
      "y": 0,
      "w": 8,
      "h": 5,
      "config": {
        "chartType": "line",
        "timeDimension": "createdAt",
        "measure": "amount",
        "granularity": "day"
      }
    },
    {
      "id": "top-products",
      "type": "table",
      "title": "Top 10 Products",
      "dataSource": "Products",
      "x": 8,
      "y": 0,
      "w": 4,
      "h": 5,
      "config": {
        "orderBy": "revenue DESC",
        "limit": 10
      }
    },
    {
      "id": "kpi-tiles",
      "type": "kpi-row",
      "x": 0,
      "y": 5,
      "w": 12,
      "h": 2,
      "config": {
        "tiles": [
          {"label": "Total Revenue", "measure": "Revenue.sum"},
          {"label": "Avg Order Value", "measure": "Orders.avgValue"},
          {"label": "Conversion Rate", "measure": "Conversions.rate"}
        ]
      }
    }
  ]
}
```

**Deliverable:** 4 JSON templates in `backend/src/main/resources/layouts/`

---

#### **T2: Role Detection & Auto-Assignment** (12h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/layout/service/LayoutService.java
package cz.muriel.core.layout.service;

import cz.muriel.core.auth.model.User;
import cz.muriel.core.auth.model.Role;
import cz.muriel.core.layout.model.UserLayout;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LayoutService {

    private final UserLayoutRepository layoutRepository;
    private final ObjectMapper objectMapper;

    public LayoutService(UserLayoutRepository layoutRepository, ObjectMapper objectMapper) {
        this.layoutRepository = layoutRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get or create default layout for user based on role
     */
    public UserLayout getOrCreateDefaultLayout(User user) {
        // Check if user already has a layout
        var existingLayouts = layoutRepository.findByUserId(user.getId());
        if (!existingLayouts.isEmpty()) {
            return existingLayouts.stream()
                .filter(UserLayout::isDefault)
                .findFirst()
                .orElse(existingLayouts.get(0));
        }

        // First login → Create from role template
        var defaultTemplate = loadDefaultTemplate(user.getRole());
        var userLayout = new UserLayout();
        userLayout.setUserId(user.getId());
        userLayout.setLayoutName("My Dashboard");
        userLayout.setLayoutData(defaultTemplate.getWidgets());
        userLayout.setRole(user.getRole());
        userLayout.setVersion(defaultTemplate.getVersion());
        userLayout.setDefault(true);

        return layoutRepository.save(userLayout);
    }

    /**
     * Load default template from resources/layouts/
     */
    private DefaultLayoutTemplate loadDefaultTemplate(Role role) {
        try {
            String filename = "layouts/default-" + role.name().toLowerCase() + ".json";
            var resource = new ClassPathResource(filename);
            return objectMapper.readValue(resource.getInputStream(), DefaultLayoutTemplate.class);
        } catch (IOException e) {
            log.error("Failed to load default layout for role {}", role, e);
            return getEmptyTemplate(role);
        }
    }

    private DefaultLayoutTemplate getEmptyTemplate(Role role) {
        var template = new DefaultLayoutTemplate();
        template.setRole(role);
        template.setVersion("1.0.0");
        template.setWidgets(Collections.emptyList());
        return template;
    }
}
```

**DTO:**

```java
// backend/src/main/java/cz/muriel/core/layout/dto/DefaultLayoutTemplate.java
package cz.muriel.core.layout.dto;

import lombok.Data;
import java.util.List;

@Data
public class DefaultLayoutTemplate {
    private Role role;
    private String version;
    private String name;
    private String description;
    private List<WidgetConfig> widgets;
}

@Data
class WidgetConfig {
    private String id;
    private String type;
    private String title;
    private String dataSource;
    private int x;
    private int y;
    private int w;
    private int h;
    private Map<String, Object> config;
}
```

**Deliverable:** Auto-assignment on first login

---

#### **T3: Layout Versioning System** (15h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/layout/service/LayoutVersionService.java
@Service
public class LayoutVersionService {

    /**
     * Check if user's layout is outdated
     */
    public LayoutUpgradeInfo checkForUpgrade(User user) {
        var userLayout = layoutRepository.findDefaultByUserId(user.getId());
        if (userLayout == null) return null;

        var currentVersion = userLayout.getVersion();
        var latestTemplate = loadDefaultTemplate(user.getRole());
        var latestVersion = latestTemplate.getVersion();

        if (isNewerVersion(latestVersion, currentVersion)) {
            return new LayoutUpgradeInfo(
                currentVersion,
                latestVersion,
                getNewFeatures(currentVersion, latestVersion, user.getRole())
            );
        }

        return null;
    }

    /**
     * Upgrade user layout to latest version
     */
    @Transactional
    public UserLayout upgradeLayout(Long userId, boolean mergeCustomChanges) {
        var userLayout = layoutRepository.findDefaultByUserId(userId);
        var user = userRepository.findById(userId).orElseThrow();

        // Backup old layout
        var backup = createBackup(userLayout);
        layoutRepository.save(backup);

        // Load latest template
        var latestTemplate = loadDefaultTemplate(user.getRole());

        if (mergeCustomChanges) {
            // Merge: Keep user's custom widgets + add new widgets from template
            var mergedWidgets = mergeWidgets(
                userLayout.getLayoutData(),
                latestTemplate.getWidgets()
            );
            userLayout.setLayoutData(mergedWidgets);
        } else {
            // Full replace
            userLayout.setLayoutData(latestTemplate.getWidgets());
        }

        userLayout.setVersion(latestTemplate.getVersion());
        return layoutRepository.save(userLayout);
    }

    private List<WidgetConfig> mergeWidgets(
        List<WidgetConfig> userWidgets,
        List<WidgetConfig> templateWidgets
    ) {
        var merged = new ArrayList<>(userWidgets);
        var userWidgetIds = userWidgets.stream()
            .map(WidgetConfig::getId)
            .collect(Collectors.toSet());

        // Add new widgets from template that user doesn't have
        templateWidgets.stream()
            .filter(widget -> !userWidgetIds.contains(widget.getId()))
            .forEach(merged::add);

        return merged;
    }

    private boolean isNewerVersion(String latest, String current) {
        // Semantic versioning comparison
        var latestParts = latest.split("\\.");
        var currentParts = current.split("\\.");

        for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
            int latestNum = Integer.parseInt(latestParts[i]);
            int currentNum = Integer.parseInt(currentParts[i]);
            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }

        return latestParts.length > currentParts.length;
    }

    private List<String> getNewFeatures(String from, String to, Role role) {
        // Load changelog from resources/layouts/CHANGELOG.md
        // Parse new features for this role between versions
        return Arrays.asList(
            "Predictive Analytics Chart",
            "AI-Powered Insights Widget"
        );
    }
}
```

**Frontend upgrade prompt:**

```typescript
// frontend/src/components/layout/LayoutUpgradeDialog.tsx
import React from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, List, ListItem } from '@mui/material';

interface LayoutUpgradeDialogProps {
  upgradeInfo: {
    currentVersion: string;
    latestVersion: string;
    newFeatures: string[];
  };
  onUpgrade: (merge: boolean) => void;
  onDismiss: () => void;
}

export const LayoutUpgradeDialog: React.FC<LayoutUpgradeDialogProps> = ({
  upgradeInfo,
  onUpgrade,
  onDismiss
}) => {
  return (
    <Dialog open maxWidth="sm" fullWidth>
      <DialogTitle>
        🎉 New Dashboard Features Available!
      </DialogTitle>
      <DialogContent>
        <p>
          Your dashboard can be upgraded from <strong>{upgradeInfo.currentVersion}</strong> to{' '}
          <strong>{upgradeInfo.latestVersion}</strong> with new widgets:
        </p>
        <List>
          {upgradeInfo.newFeatures.map((feature, i) => (
            <ListItem key={i}>• {feature}</ListItem>
          ))}
        </List>
        <p>
          <strong>Note:</strong> Your custom widgets will be preserved.
        </p>
      </DialogContent>
      <DialogActions>
        <Button onClick={onDismiss}>Keep Current</Button>
        <Button onClick={() => onUpgrade(true)} variant="contained" color="primary">
          Upgrade Now
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

**Deliverable:** Versioning + upgrade system

---

#### **T4: Role Change Migration** (10h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/auth/listener/RoleChangeListener.java
@Component
public class RoleChangeListener implements ApplicationListener<RoleChangedEvent> {

    private final LayoutService layoutService;

    @Override
    public void onApplicationEvent(RoleChangedEvent event) {
        var user = event.getUser();
        var oldRole = event.getOldRole();
        var newRole = event.getNewRole();

        // Mark user for layout migration prompt
        var userLayout = layoutRepository.findDefaultByUserId(user.getId());
        if (userLayout != null && userLayout.getRole() != newRole) {
            userLayout.setMigrationPending(true);
            userLayout.setPendingRole(newRole);
            layoutRepository.save(userLayout);
        }
    }
}
```

**Frontend migration dialog:**

```typescript
// frontend/src/hooks/useLayoutMigration.ts
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';

export const useLayoutMigration = () => {
  const { user } = useAuth();
  const [migrationPending, setMigrationPending] = useState(false);
  const [pendingRole, setPendingRole] = useState<Role | null>(null);

  useEffect(() => {
    const checkMigration = async () => {
      const response = await fetch('/api/layouts/migration-status');
      const { pending, pendingRole } = await response.json();
      setMigrationPending(pending);
      setPendingRole(pendingRole);
    };

    checkMigration();
  }, [user]);

  const acceptMigration = async () => {
    await fetch('/api/layouts/migrate-to-role', { method: 'POST' });
    setMigrationPending(false);
  };

  const rejectMigration = async () => {
    await fetch('/api/layouts/dismiss-migration', { method: 'POST' });
    setMigrationPending(false);
  };

  return { migrationPending, pendingRole, acceptMigration, rejectMigration };
};
```

**Deliverable:** Role change detection + migration prompt

---

#### **T5: Testing** (5h)

**E2E Tests:**

```typescript
// e2e/specs/layout/role-defaults.spec.ts
import { test, expect } from '@playwright/test';

test('New ANALYST user gets default analyst layout', async ({ page }) => {
  // Create new user with ANALYST role
  await createTestUser({ role: 'ANALYST', email: 'analyst@test.com' });

  // Login
  await page.goto('/login');
  await page.fill('[name=email]', 'analyst@test.com');
  await page.fill('[name=password]', 'Test.1234');
  await page.click('button[type=submit]');

  // Verify default analyst widgets present
  await expect(page.locator('text=Revenue Trend')).toBeVisible();
  await expect(page.locator('text=Top 10 Products')).toBeVisible();
  await expect(page.locator('[data-widget-id="kpi-tiles"]')).toBeVisible();
});

test('Layout upgrade prompt appears for outdated version', async ({ page }) => {
  // Setup: User with v1.0.0 layout, new v1.1.0 available
  await setupLayoutVersion('1.0.0');
  await publishNewLayoutVersion('1.1.0');

  await page.goto('/dashboard');

  // Verify upgrade dialog
  await expect(page.locator('text=New Dashboard Features Available')).toBeVisible();
  await expect(page.locator('text=Predictive Analytics Chart')).toBeVisible();

  // Click upgrade
  await page.click('button:has-text("Upgrade Now")');

  // Verify new widgets added
  await expect(page.locator('[data-widget-id="predictive-analytics"]')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ First login → default layout loaded < 1s
- ✅ 90% users keep default layout (good UX!)
- ✅ Layout upgrade adoption > 60% (valuable new features)
- ✅ Role migration prompt shown < 2s after role change

---

## 🔗 DEPENDENCIES

- **EPIC-003:** RBAC roles (ADMIN, TENANT_ADMIN, ANALYST, VIEWER)
- **S1:** DataView component (widgets render)

---

## 📚 DOCUMENTATION

- [ ] Admin Guide: Creating custom role templates
- [ ] Migration Guide: Versioning strategy
- [ ] Developer Guide: Adding new widgets to templates

---

**Status:** 📋 TODO → Ready for implementation  
**Next:** S6: Visual Query Builder

