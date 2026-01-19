# T1: Formula Builder UI

**Story:** [S8: Frontend-Backend Integration](README.md)  
**Effort:** 8 hours  
**Priority:** P0  
**Dependencies:** EPIC-014 S3, S9

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

## ✅ Acceptance Criteria

- [ ] Formula builder umoznuje vyber pole a operatoru bez manualniho psani.
- [ ] Validace blokuje ulozeni neplatneho vyrazu a zobrazuje chybu.
- [ ] Dostupna pole jsou nacitena z Cube.js meta API.
- [ ] Po ulozeni se custom metric objevi v seznamu metrik.

---

## ✅ DELIVERABLES

- [ ] Formula builder UI
- [ ] Field picker
- [ ] Syntax validation

---

**Estimated:** 8 hours
