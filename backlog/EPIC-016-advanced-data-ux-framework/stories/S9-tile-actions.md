# S9: Tile Click Actions & Navigation

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🟡 **P1** | **Effort:** ~40h | **Sprint:** 10

---

## 📋 USER STORY

**Jako** Dashboard User, **chci** klikat na tiles/charts a spustit akce (drill-down, external URL, modal popup), **abych** mohl rychle navigovat k detailu.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Drill-Down**: Klik na chart bar → filtr detail view (Tenants by Status → klik "ACTIVE" → table s active tenants)
2. **External URL**: Klik na tile → otevře external link (e.g., Grafana dashboard)
3. **Modal Popup**: Klik na tile → otevře detail v modal (e.g., Workflow detail)
4. **Navigation State**: Back button funguje (vrátí filtr)

---

## 🏗️ TASK BREAKDOWN (~40h)

### T1: Click Action Configuration (10h)
- Config per widget: `onClick: 'drill-down' | 'url' | 'popup'`

### T2: Drill-Down Implementation (15h)
- Extract filter from clicked data
- Update parent view filters
- Breadcrumb navigation

### T3: External URL Handler (5h)
- Open link in new tab
- Variable substitution (e.g., `https://grafana?tenant={{tenantId}}`)

### T4: Modal Popup Trigger (8h)
- Open modal with detail content

### T5: Testing (2h)

---

## 📦 DEPENDENCIES

- **S1**: DataView (for drill-down target)
- **S5**: Multi-Window (for popups)

---

## 📊 SUCCESS METRICS

- Click action < 200ms
- Navigation state preserved

