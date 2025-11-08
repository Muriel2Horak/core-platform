# T1: Layout Builder

**Story:** [S8: Customizable Entity Popups](README.md)  
**Effort:** 25 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Drag & drop layout builder pro entity detail popupy.

---

## 🎯 ACCEPTANCE CRITERIA

1. Grid layout builder
2. Add/remove sections
3. Drag fields between sections
4. Preview layout

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/layout-builder/LayoutBuilder.tsx
interface LayoutSection {
  id: string;
  title: string;
  fields: string[];
  gridPosition: { x: number; y: number; w: number; h: number };
}

export const LayoutBuilder: React.FC<{ entityType: string }> = ({ entityType }) => {
  const [sections, setSections] = useState<LayoutSection[]>([]);
  
  const addSection = () => {
    setSections([...sections, {
      id: uuid(),
      title: 'New Section',
      fields: [],
      gridPosition: { x: 0, y: sections.length, w: 6, h: 2 }
    }]);
  };
  
  return (
    <Box>
      <Button onClick={addSection}>Add Section</Button>
      
      <GridLayout
        layout={sections.map(s => s.gridPosition)}
        onLayoutChange={(layout) => {
          setSections(sections.map((s, i) => ({ ...s, gridPosition: layout[i] })));
        }}
      >
        {sections.map(section => (
          <Paper key={section.id}>
            <TextField
              value={section.title}
              onChange={(e) => {
                setSections(sections.map(s => s.id === section.id ? { ...s, title: e.target.value } : s));
              }}
            />
            <FieldDropzone fields={section.fields} />
          </Paper>
        ))}
      </GridLayout>
    </Box>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] LayoutBuilder component
- [ ] Drag & drop sections
- [ ] Field assignment
- [ ] Unit tests

---

**Estimated:** 25 hours
