# T2: Section Types

**Story:** [S8: Customizable Entity Popups](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Section types: Form Fields, Related Data, Audit Log, Actions.

---

## 🎯 ACCEPTANCE CRITERIA

1. Form Fields section (text, number, date inputs)
2. Related Data section (mini table)
3. Audit Log section (timeline)
4. Actions section (buttons)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/layout-builder/sections/FormFieldsSection.tsx
export const FormFieldsSection: React.FC<{ fields: string[]; data: any }> = ({ fields, data }) => {
  return (
    <Grid container spacing={2}>
      {fields.map(field => (
        <Grid item xs={6} key={field}>
          <TextField
            label={field}
            value={data[field]}
            fullWidth
          />
        </Grid>
      ))}
    </Grid>
  );
};

// frontend/src/components/layout-builder/sections/RelatedDataSection.tsx
export const RelatedDataSection: React.FC<{ entity: string; filter: any }> = ({ entity, filter }) => {
  const { data } = useCubeQuery({ entity, filter });
  
  return (
    <DataGrid
      rows={data}
      columns={['name', 'status', 'createdAt']}
      pageSize={5}
    />
  );
};
```

---

## ✅ DELIVERABLES

- [ ] 4 section types
- [ ] Section type selector
- [ ] Rendering logic
- [ ] Unit tests

---

**Estimated:** 15 hours
