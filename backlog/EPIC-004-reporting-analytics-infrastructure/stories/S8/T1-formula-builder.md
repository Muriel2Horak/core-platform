# T1: Formula Builder UI

**Story:** [S8: Frontend-Backend Integration](README.md)  
**Effort:** 8 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Visual formula builder pro custom metrics.

---

## 🏗️ IMPLEMENTATION

```typescript
export const FormulaBuilder: React.FC = () => {
  const [formula, setFormula] = useState('');
  
  return (
    <Box>
      <FieldPicker onSelect={(f) => setFormula(formula + f)} />
      <OperatorPicker onSelect={(op) => setFormula(formula + op)} />
      <TextField value={formula} />
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Formula builder UI
- [ ] Field picker
- [ ] Syntax validation

---

**Estimated:** 8 hours
