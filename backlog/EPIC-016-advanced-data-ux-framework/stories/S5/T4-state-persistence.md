# T4: Window State Persistence

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 20 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Save/restore otevřených oken při page reload.

---

## 🎯 ACCEPTANCE CRITERIA

1. Save window state to localStorage
2. Restore on reload
3. Restore position + size
4. Restore entity data

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/stores/windowStore.ts (extension)
export const useWindowStore = create<WindowStore>()(
  persist(
    (set) => ({
      windows: [],
      openWindow: (entityType, entityId) => { /* ... */ },
      closeWindow: (id) => { /* ... */ },
      bringToFront: (id) => { /* ... */ }
    }),
    {
      name: 'window-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        windows: state.windows.map(w => ({
          id: w.id,
          entityType: w.entityType,
          entityId: w.entityId,
          position: w.position,
          size: w.size
        }))
      })
    }
  )
);

// frontend/src/components/WindowManager.tsx
export const WindowManager: React.FC = () => {
  const { windows, openWindow } = useWindowStore();
  
  useEffect(() => {
    // Restore entity data on mount
    windows.forEach(async (w) => {
      const data = await api.get(`/api/${w.entityType.toLowerCase()}/${w.entityId}`);
      // Update window with fresh data
    });
  }, []);
  
  return (
    <>
      {windows.map(w => (
        <PopupWindow key={w.id} {...w}>
          <EntityEditor entityType={w.entityType} entityId={w.entityId} />
        </PopupWindow>
      ))}
    </>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Persist middleware
- [ ] Restore logic
- [ ] Data refresh
- [ ] Unit tests

---

**Estimated:** 20 hours
