# T1: Multi-select Filters

**Story:** [S2: Advanced Filtering & Search](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Implementovat multi-select filter dropdowns (Status, Role, Tenant, Priority).

---

## 🎯 ACCEPTANCE CRITERIA

1. Multi-select dropdowns s checkboxes
2. Filter chips zobrazení
3. Clear all filters button
4. URL query params sync

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/filters/MultiSelectFilter.tsx
export const MultiSelectFilter: React.FC<{ options: Option[]; onChange: (selected: string[]) => void }> = ({ options, onChange }) => {
  const [selected, setSelected] = useState<string[]>([]);
  
  return (
    <FormControl>
      <InputLabel>Status</InputLabel>
      <Select multiple value={selected} onChange={(e) => {
        setSelected(e.target.value as string[]);
        onChange(e.target.value as string[]);
      }}>
        {options.map(opt => (
          <MenuItem key={opt.value} value={opt.value}>
            <Checkbox checked={selected.includes(opt.value)} />
            <ListItemText primary={opt.label} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] MultiSelectFilter component
- [ ] Chips display
- [ ] URL params sync
- [ ] Unit tests

---

**Estimated:** 20 hours
