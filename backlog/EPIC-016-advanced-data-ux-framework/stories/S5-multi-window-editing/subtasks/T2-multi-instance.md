# T2: Multi-Instance Manager

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Otevřít 5+ entity instancí současně (User #1, User #2, Workflow #5).

---

## 🎯 ACCEPTANCE CRITERIA

1. Open multiple instances
2. Track active windows
3. Switch between windows
4. Close individual windows

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/stores/windowStore.ts
interface EntityWindow {
  id: string;
  entityType: string;
  entityId: string;
  title: string;
  position: { x: number; y: number };
  size: { width: number; height: number };
  zIndex: number;
}

export const useWindowStore = create<{
  windows: EntityWindow[];
  openWindow: (entityType: string, entityId: string) => void;
  closeWindow: (id: string) => void;
  bringToFront: (id: string) => void;
}>((set) => ({
  windows: [],
  
  openWindow: (entityType, entityId) => set(state => {
    const id = `${entityType}-${entityId}`;
    if (state.windows.find(w => w.id === id)) return state;
    
    return {
      windows: [...state.windows, {
        id,
        entityType,
        entityId,
        title: `${entityType} #${entityId}`,
        position: { x: 100 + state.windows.length * 30, y: 100 + state.windows.length * 30 },
        size: { width: 600, height: 400 },
        zIndex: Math.max(...state.windows.map(w => w.zIndex), 0) + 1
      }]
    };
  }),
  
  closeWindow: (id) => set(state => ({
    windows: state.windows.filter(w => w.id !== id)
  })),
  
  bringToFront: (id) => set(state => ({
    windows: state.windows.map(w => 
      w.id === id 
        ? { ...w, zIndex: Math.max(...state.windows.map(w => w.zIndex)) + 1 }
        : w
    )
  }))
}));
```

---

## ✅ DELIVERABLES

- [ ] Window manager store
- [ ] Multi-instance support
- [ ] Z-index management
- [ ] Unit tests

---

**Estimated:** 20 hours
