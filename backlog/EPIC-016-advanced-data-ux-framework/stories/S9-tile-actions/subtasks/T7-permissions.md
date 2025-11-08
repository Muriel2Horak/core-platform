# T7: Action Permissions

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Permission checks pro actions (admin-only, tenant-owner).

---

## 🎯 ACCEPTANCE CRITERIA

1. Permission annotations
2. Frontend checks
3. Backend validation
4. Hide unauthorized actions

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/security/RequiresPermission.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
  String value();
}

// backend/src/main/java/cz/muriel/core/workflow/WorkflowController.java
@PostMapping("/{id}/approve")
@RequiresPermission("workflow:approve")
public void approve(@PathVariable UUID id) {
  workflowService.approve(id);
}
```

```typescript
// frontend/src/hooks/usePermissions.ts
export const usePermissions = () => {
  const { user } = useAuth();
  
  const hasPermission = (permission: string) => {
    return user?.permissions.includes(permission) || user?.role === 'ADMIN';
  };
  
  return { hasPermission };
};
```

---

## ✅ DELIVERABLES

- [ ] Permission annotations
- [ ] Frontend hook
- [ ] Backend validation

---

**Estimated:** 15 hours
