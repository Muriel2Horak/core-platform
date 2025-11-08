# T1: Action Button Framework

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 25 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Generic action button framework pro entity tiles.

---

## 🎯 ACCEPTANCE CRITERIA

1. Action button registration
2. Dynamic button rendering
3. Permission checks
4. Loading states

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/actions/ActionButton.tsx
interface Action {
  id: string;
  label: string;
  icon: ReactNode;
  handler: (entity: any) => Promise<void>;
  permission?: string;
}

export const ActionButton: React.FC<{ action: Action; entity: any }> = ({ action, entity }) => {
  const [loading, setLoading] = useState(false);
  const { hasPermission } = useAuth();
  
  if (action.permission && !hasPermission(action.permission)) {
    return null;
  }
  
  const handleClick = async () => {
    setLoading(true);
    try {
      await action.handler(entity);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <IconButton onClick={handleClick} disabled={loading}>
      {loading ? <CircularProgress size={20} /> : action.icon}
    </IconButton>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] ActionButton component
- [ ] Permission system
- [ ] Loading states

---

**Estimated:** 25 hours
