# T6: Undo/Redo

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 20 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Undo/redo history pro akce (Ctrl+Z, Ctrl+Shift+Z).

---

## 🎯 ACCEPTANCE CRITERIA

1. Action history stack
2. Undo operation
3. Redo operation
4. History limit (50 actions)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/stores/historyStore.ts
interface HistoryAction {
  type: string;
  undo: () => Promise<void>;
  redo: () => Promise<void>;
}

export const useHistoryStore = create<{
  past: HistoryAction[];
  future: HistoryAction[];
  addAction: (action: HistoryAction) => void;
  undo: () => void;
  redo: () => void;
}>((set, get) => ({
  past: [],
  future: [],
  
  addAction: (action) => set(state => ({
    past: [...state.past, action].slice(-50),
    future: []
  })),
  
  undo: async () => {
    const { past, future } = get();
    const action = past[past.length - 1];
    if (!action) return;
    
    await action.undo();
    set({
      past: past.slice(0, -1),
      future: [action, ...future]
    });
  },
  
  redo: async () => {
    const { past, future } = get();
    const action = future[0];
    if (!action) return;
    
    await action.redo();
    set({
      past: [...past, action],
      future: future.slice(1)
    });
  }
}));
```

---

## ✅ DELIVERABLES

- [ ] History store
- [ ] Undo/redo logic
- [ ] Keyboard shortcuts

---

**Estimated:** 20 hours
