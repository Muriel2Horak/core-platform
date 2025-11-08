# T5: Keyboard Shortcuts

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 15 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Keyboard shortcuts (Ctrl+D delete, Ctrl+E edit).

---

## 🎯 ACCEPTANCE CRITERIA

1. Global shortcut listener
2. Shortcut registry
3. Help dialog (Ctrl+?)
4. Disable in inputs

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/hooks/useKeyboardShortcuts.ts
export const useKeyboardShortcuts = (shortcuts: Record<string, () => void>) => {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      // Don't trigger in input fields
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return;
      }
      
      const key = [
        e.ctrlKey && 'Ctrl',
        e.shiftKey && 'Shift',
        e.key.toUpperCase()
      ].filter(Boolean).join('+');
      
      if (shortcuts[key]) {
        e.preventDefault();
        shortcuts[key]();
      }
    };
    
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [shortcuts]);
};

// Usage:
useKeyboardShortcuts({
  'Ctrl+D': () => deleteSelected(),
  'Ctrl+E': () => editSelected(),
  'Ctrl+?': () => openHelpDialog()
});
```

---

## ✅ DELIVERABLES

- [ ] Shortcut hook
- [ ] Help dialog
- [ ] Input exclusion

---

**Estimated:** 15 hours
