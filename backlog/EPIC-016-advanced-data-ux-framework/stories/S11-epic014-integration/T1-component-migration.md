# T1: Component Migration

**Story:** [S11: EPIC-014 Integration](README.md)  
**Effort:** 25 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Migrovat na EPIC-014 design system components.

---

## 🎯 ACCEPTANCE CRITERIA

1. Replace custom tiles with EPIC-014 Tile
2. Replace dashboards with EPIC-014 DashboardLayout
3. Replace forms with EPIC-014 FormBuilder
4. Consistent styling

---

## 🏗️ IMPLEMENTATION

```typescript
// PŘED (custom):
import { CustomTile } from '../components/custom/Tile';

<CustomTile title="User" data={user} />

// PO (EPIC-014):
import { Tile } from '@core-platform/design-system';

<Tile
  entityType="User"
  data={user}
  actions={['edit', 'delete']}
  onActionClick={handleAction}
/>
```

```typescript
// Dashboard migration:
import { DashboardLayout } from '@core-platform/design-system';

<DashboardLayout
  widgets={widgets}
  onWidgetChange={updateWidgets}
  gridCols={12}
/>
```

---

## ✅ DELIVERABLES

- [ ] Migrate tiles
- [ ] Migrate dashboards
- [ ] Migrate forms

---

**Estimated:** 25 hours
