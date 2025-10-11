# S2: Phase 8 - More Integrations (3h)

**Cíl:** Integrace presence systému do TenantEditPage a RoleEditPage  
**Očekávaný čas:** 3h  
**Prerekvizity:** Phase 7 complete (UI components ready)

---

## 1️⃣ TenantEditPage Integration (1.5h)

### 1.1 Create TenantEditPage Component
**File:** `frontend/src/pages/admin/TenantEditPage.tsx`

**Features:**
- Form fields: name, slug, status (active/inactive), description
- Wrap in `WithPresenceFeedback` HOC
- Field-level locking with `FieldLockIndicator`
- `PresenceIndicator` in header
- Auto-save on blur (debounced 500ms)
- Optimistic UI updates
- Error handling with retry

**Acceptance Criteria:**
- ✅ Shows presence indicator in header
- ✅ Shows lock indicator on focused fields
- ✅ Auto-saves on blur
- ✅ Shows snackbar on save success/error
- ✅ TypeScript compiles without errors

### 1.2 Add Routing
**File:** `frontend/src/App.tsx`

```tsx
<Route path="/admin/tenants/:id/edit" element={
  <RequireAuth allowedRoles={['ROLE_SUPER_ADMIN']}>
    <TenantEditPage />
  </RequireAuth>
} />
```

---

## 2️⃣ RoleEditPage Integration (1.5h)

### 2.1 Create RoleEditPage Component
**File:** `frontend/src/pages/admin/RoleEditPage.tsx`

**Features:**
- Form fields: name, description, permissions (multi-select)
- Wrap in `WithPresenceFeedback` HOC
- Field-level locking with `FieldLockIndicator`
- `PresenceIndicator` in header
- Auto-save on blur (debounced 500ms)
- Permission changes trigger immediate save
- Error handling with retry

**Acceptance Criteria:**
- ✅ Shows presence indicator in header
- ✅ Shows lock indicator on focused fields
- ✅ Permission changes auto-save
- ✅ Shows snackbar on save success/error
- ✅ TypeScript compiles without errors

### 2.2 Add Routing
**File:** `frontend/src/App.tsx`

```tsx
<Route path="/admin/roles/:id/edit" element={
  <RequireAuth allowedRoles={['ROLE_SUPER_ADMIN']}>
    <RoleEditPage />
  </RequireAuth>
} />
```

---

## 3️⃣ Manual Testing (30min)

### 3.1 Test Multi-User Presence
**Steps:**
1. Open UserEditPage in Browser A (user1)
2. Open TenantEditPage in Browser B (user2)
3. Open RoleEditPage in Browser C (user3)
4. Verify all 3 users appear in presence indicators
5. Test field locking across pages
6. Test auto-save on blur
7. Test stale mode (disconnect WiFi, reconnect)

**Expected Results:**
- All 3 users visible in presence indicators
- Field locks work across browsers
- Auto-save triggers on blur (500ms debounce)
- Stale mode activates/deactivates correctly
- No console errors

### 3.2 Test Edge Cases
- Slow network (throttle to 3G)
- Rapid tab switching
- Browser refresh mid-edit
- Multiple tabs same user

---

## 4️⃣ Documentation (15min)

### 4.1 Update Integration Guide
**File:** `docs/PRESENCE_INTEGRATION.md`

**Content:**
- Pattern for integrating presence into new pages
- HOC wrapper usage
- Field locking best practices
- Auto-save debouncing
- Error handling

---

## 🎯 Phase 8 Deliverables

- [x] TenantEditPage.tsx with full presence integration
- [x] RoleEditPage.tsx with full presence integration
- [x] Routing for both pages
- [x] Manual testing (2 browsers, 3 users)
- [x] Integration guide documentation
- [x] 0 TypeScript errors in new code

---

## ⏱️ Time Tracking

| Task | Estimate | Actual | Notes |
|------|----------|--------|-------|
| TenantEditPage | 1.5h | TBD | Form + presence + routing |
| RoleEditPage | 1.5h | TBD | Form + presence + routing |
| Manual Testing | 30min | TBD | 2 browsers, 3 users |
| Documentation | 15min | TBD | Integration guide |
| **Total** | **3h** | **TBD** | |

---

**Progress:** 0% (Ready to start)  
**Next:** Create TenantEditPage.tsx
