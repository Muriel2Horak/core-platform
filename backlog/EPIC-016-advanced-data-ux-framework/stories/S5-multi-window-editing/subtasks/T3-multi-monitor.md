# T3: Multi-Monitor Support

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Drag popups na sekundární monitor.

---

## 🎯 ACCEPTANCE CRITERIA

1. Detect screen.availWidth/availHeight
2. Allow negative x/y (secondary left)
3. Save position cross-monitor
4. Restore on monitor if available

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/hooks/useMultiMonitor.ts
export const useMultiMonitor = () => {
  const [screens, setScreens] = useState<ScreenDetails[]>([]);
  
  useEffect(() => {
    if ('getScreenDetails' in window) {
      // @ts-ignore - Experimental API
      window.getScreenDetails().then((details: any) => {
        setScreens(details.screens);
      });
    } else {
      setScreens([{
        availWidth: window.screen.availWidth,
        availHeight: window.screen.availHeight,
        left: 0,
        top: 0
      }]);
    }
  }, []);
  
  const isPositionValid = (x: number, y: number) => {
    return screens.some(screen => 
      x >= screen.left && 
      x <= screen.left + screen.availWidth &&
      y >= screen.top && 
      y <= screen.top + screen.availHeight
    );
  };
  
  return { screens, isPositionValid };
};
```

---

## ✅ DELIVERABLES

- [ ] Multi-monitor detection
- [ ] Cross-monitor drag
- [ ] Position validation
- [ ] Unit tests

---

**Estimated:** 15 hours
