# S4: Role-Based Default Layouts

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🔴 **P0** | **Effort:** ~35h | **Sprint:** 4

---

## 📋 USER STORY

**Jako** System Admin, **chci** přednastavené defaultní dashboardy pro každou roli (ADMIN, TENANT_ADMIN, ANALYST, VIEWER), **abych** uživatelům poskytl relevantní data od prvního přihlášení.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Default Layouts per Role**:
   - ADMIN: System health KPIs + all tenants table
   - TENANT_ADMIN: Tenant users + workflows
   - ANALYST: Charts + pivot tables
   - VIEWER: Read-only KPI dashboard

2. **Auto-Assignment**: První login → load default layout pro roli

3. **Layout Versioning**: Track changes, rollback možnost

4. **Migration on Role Change**: User promoted VIEWER → ADMIN → load new default

---

## 🏗️ TASK BREAKDOWN (~35h)

### T1: Default Layout Templates (10h)
- JSON template per role
- Seed database with default layouts

### T2: Role Detection & Assignment (8h)
- Check user role on dashboard load
- Assign default if no personal layout exists

### T3: Layout Versioning (12h)
- Version field in layout schema
- Migration script for layout upgrades

### T4: Testing (5h)

---

## 📦 DEPENDENCIES

- **EPIC-003**: RBAC (role detection) ✅

---

## 📊 SUCCESS METRICS

- 100% users get default layout on first login
- Layout load < 1s

