# T2: Advanced Chart Config

**Story:** [S7: Extended Data Widgets](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

UI pro konfiguraci chartů (axes, colors, legends).

---

## 🎯 ACCEPTANCE CRITERIA

1. Axis configuration
2. Color palette picker
3. Legend position
4. Tooltips customization

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/charts/ChartConfigPanel.tsx
export const ChartConfigPanel: React.FC<{ config: ChartConfig; onChange: (c: ChartConfig) => void }> = ({ config, onChange }) => {
  return (
    <Box>
      <TextField
        label="X Axis Label"
        value={config.xAxisLabel}
        onChange={(e) => onChange({ ...config, xAxisLabel: e.target.value })}
      />
      
      <FormControl>
        <InputLabel>Color Palette</InputLabel>
        <Select value={config.colorPalette} onChange={(e) => onChange({ ...config, colorPalette: e.target.value })}>
          <MenuItem value="blues">Blues</MenuItem>
          <MenuItem value="greens">Greens</MenuItem>
          <MenuItem value="reds">Reds</MenuItem>
        </Select>
      </FormControl>
      
      <FormControl>
        <InputLabel>Legend Position</InputLabel>
        <Select value={config.legendPosition} onChange={(e) => onChange({ ...config, legendPosition: e.target.value })}>
          <MenuItem value="top">Top</MenuItem>
          <MenuItem value="bottom">Bottom</MenuItem>
          <MenuItem value="left">Left</MenuItem>
          <MenuItem value="right">Right</MenuItem>
        </Select>
      </FormControl>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Config panel
- [ ] Color picker
- [ ] Live preview
- [ ] Unit tests

---

**Estimated:** 15 hours
