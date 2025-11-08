# T2: View Mode Switcher

**Story:** [S1: Universal Data View Engine](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Implementovat toolbar s view mode switcher icons (📋 Table / 📊 Chart / 🔥 Heatmap / 🗂️ Pivot / 📇 Cards).

---

## 🎯 ACCEPTANCE CRITERIA

1. Toolbar s 5 view mode buttons
2. Active state highlighting
3. Smooth transition mezi modes
4. LocalStorage persistence

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dataview/ViewModeSwitcher.tsx
type ViewMode = 'table' | 'chart' | 'heatmap' | 'pivot' | 'cards';

export const ViewModeSwitcher: React.FC<{ value: ViewMode; onChange: (mode: ViewMode) => void }> = ({ value, onChange }) => {
  return (
    <ToggleButtonGroup value={value} exclusive onChange={(_, mode) => onChange(mode)}>
      <ToggleButton value="table"><TableChart /> Table</ToggleButton>
      <ToggleButton value="chart"><BarChart /> Chart</ToggleButton>
      <ToggleButton value="heatmap"><Whatshot /> Heatmap</ToggleButton>
      <ToggleButton value="pivot"><ViewModule /> Pivot</ToggleButton>
      <ToggleButton value="cards"><GridView /> Cards</ToggleButton>
    </ToggleButtonGroup>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] ViewModeSwitcher component
- [ ] Mode persistence (localStorage)
- [ ] Smooth transitions
- [ ] Unit tests

---

**Estimated:** 15 hours
