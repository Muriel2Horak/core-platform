# T2: Advanced Filter Builder

**Story:** [S2: Advanced Filtering & Search](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Implementovat advanced filter builder s AND/OR conditions.

---

## 🎯 ACCEPTANCE CRITERIA

1. Add condition button
2. AND/OR toggle
3. Field + Operator + Value inputs
4. Remove condition button

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/filters/FilterBuilder.tsx
interface FilterCondition {
  field: string;
  operator: 'equals' | 'contains' | 'greaterThan';
  value: any;
  logic: 'AND' | 'OR';
}

export const FilterBuilder: React.FC = () => {
  const [conditions, setConditions] = useState<FilterCondition[]>([]);
  
  const addCondition = () => {
    setConditions([...conditions, { field: '', operator: 'equals', value: '', logic: 'AND' }]);
  };
  
  return (
    <Box>
      {conditions.map((cond, idx) => (
        <Box key={idx} sx={{ display: 'flex', gap: 1 }}>
          <Select value={cond.field} onChange={(e) => updateCondition(idx, 'field', e.target.value)}>
            <MenuItem value="name">Name</MenuItem>
            <MenuItem value="email">Email</MenuItem>
          </Select>
          
          <Select value={cond.operator}>
            <MenuItem value="equals">Equals</MenuItem>
            <MenuItem value="contains">Contains</MenuItem>
          </Select>
          
          <TextField value={cond.value} onChange={(e) => updateCondition(idx, 'value', e.target.value)} />
          
          <IconButton onClick={() => removeCondition(idx)}><Delete /></IconButton>
        </Box>
      ))}
      
      <Button onClick={addCondition}>+ Add Condition</Button>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] FilterBuilder component
- [ ] AND/OR logic
- [ ] Dynamic conditions
- [ ] Unit tests

---

**Estimated:** 20 hours
